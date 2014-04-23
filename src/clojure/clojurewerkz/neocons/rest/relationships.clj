;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.relationships
  (:refer-clojure :exclude [get find])
  (:require [cheshire.core                     :as json]
            [clojurewerkz.neocons.rest         :as rest]
            [clojurewerkz.neocons.rest.cypher  :as cypher]
            [clojurewerkz.neocons.rest.paths   :as paths]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers  :refer :all]
            [clojurewerkz.neocons.rest.records  :refer :all]
            [clojure.string :refer [join]]
            [clojurewerkz.neocons.rest.conversion :refer [to-id]])
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Connection Neo4JEndpoint]
            [clojurewerkz.neocons.rest.records Node Relationship Index]))

;;
;; Implementation
;;

(defn- relationships-location-for
  [^Neo4JEndpoint endpoint node kind types]
  (let [query-params (if types
                       (str "/" (join "&" (map name types)))
                       "")]
    (str (:node-uri endpoint) "/" (to-id node) "/relationships/" (name kind) query-params)))

(defn- create-relationship-location-for
  [^Neo4JEndpoint endpoint node]
  (str (:node-uri endpoint) "/" (to-id node) "/relationships/"))

(defn- relationships-for
  [^Connection connection ^Node node kind types]
  (let [{ :keys [status headers body] } (rest/GET connection (relationships-location-for (:endpoint connection) node kind types))
        xs  (json/decode body true)]
    (when-not (missing? status)
      (map instantiate-rel-from xs))))


;;
;; API
;;

(defn create
  "Creates a relationship of given type between two nodes. "
  ([^Connection connection ^Node from ^Node to rel-type]
     (create connection from to rel-type {}))
  ([^Connection connection ^Node from ^Node to rel-type data]
     ;; these (or ...)s here are necessary because Neo4J REST API returns nodes in different format when fetched via nodes/get
     ;; and found via index. We have to account for that. MK.
     (let [{:keys [status headers body]} (rest/POST connection (or (:create-relationship-uri from)
                                                                   (create-relationship-location-for (:endpoint connection) from))
                                                    :body (json/encode {:to (or (:location-uri to)
                                                                                  (node-location-for (:endpoint connection) (to-id to)))
                                                                        :type rel-type :data data}))
           payload  (json/decode body true)]
       (instantiate-rel-from payload))))

(defn create-unique-in-index
  "Atomically creates and returns a relationship with the given properties and adds it to an index while ensuring key uniqueness
   in that index. This is the same as first creating a relationship using the `clojurewerkz.neocons.rest.relationships/create` function
   and indexing it with the 4-arity of `clojurewerkz.neocons.rest.relationships/add-to-index` but performed atomically and requires
   only a single request.

   For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-unique-indexes.html (section 19.8.4)"
  ([^Connection connection ^Node from ^Node to rel-type idx k v]
     (create-unique-in-index connection from to rel-type idx k v {}))
  ([^Connection connection ^Node from ^Node to rel-type idx k v data]
     (let [uri   (str (url-with-path (get-in connection [:endpoint :relationship-index-uri]) idx) "/?unique")
           body  {:key k
                  :value v
                  :start (or (:location-uri from) (node-location-for (:endpoint connection) (to-id from)))
                  :end   (or (:location-uri to) (node-location-for (:endpoint connection) (to-id to)))
                  :type rel-type
                  :properties data}
           {:keys [status headers body]} (rest/POST connection uri :body (json/encode body))
           payload  (json/decode body true)]
       (instantiate-rel-from payload))))

(defn create-many
  "Concurrently creates multiple relations of given type between the *from* node and several provded nodes.
   All relationships will be of the same type.

   This function should be used when number of relationships that need to be created is moderately high (dozens and more),
   otherwise it would be less efficient than using clojure.core/map over the same sequence of nodes"
  ([^Connection connection ^Node from xs rel-type]
     (pmap (fn [^Node n]
             (create connection from n rel-type)) xs))
  ([^Connection connection ^Node from xs rel-type data]
     (pmap (fn [^Node n]
             (create connection from n rel-type data)) xs)))


(declare outgoing-for)
(defn maybe-create
  "Creates a relationship of given type between two nodes, unless it already exists"
  ([^Connection connection from to rel-type]
     (maybe-create connection from to rel-type {}))
  ([^Connection connection from to rel-type data]
     (if (paths/exists-between? connection (to-id from) (to-id to)
                                :relationships [{:type (name rel-type) :direction "out"}]
                                :max-depth 1)
       (let [rels (outgoing-for connection from :types [rel-type])
             uri  (node-location-for (:endpoint connection) (to-id to))]
         (first (filter #(= (:end %) uri) rels)))
       (create connection from to rel-type data))))

(defn get
  "Fetches relationship by id"
  [^Connection connection ^long id]
  (let [{:keys [status headers body]} (rest/GET connection (rel-location-for (:endpoint connection) id))
        payload  (json/decode body true)]
    (when-not (missing? status)
      (instantiate-rel-from payload id))))

(defn get-many
  "Fetches multiple relationships by id.

  This is a non-standard operation that requires Cypher support as well as support for that very feature
  by Cypher itself (Neo4j Server versions 1.6.3 and later)."
  ([^Connection connection coll]
     (let [{:keys [data]} (cypher/query connection "START x = relationship({ids}) RETURN x" {:ids coll})]
       (map (comp instantiate-rel-from first) data))))

(defn delete
  "Deletes relationship by id"
  [^Connection connection rel]
  (let [{:keys [status headers]} (rest/DELETE connection (rel-location-for (:endpoint connection) (to-id rel)))]
    (if (or (missing? status)
            (conflict? status))
      [nil status]
      [(to-id rel)  status])))

(defn maybe-delete
  "Deletes relationship by id but only if it exists. Otherwise, does nothing and returns nil"
  [^Connection connection ^long id]
  (if-let [n (get id)]
    (delete connection id)))

(defn delete-many
  "Deletes multiple relationships by id."
  [^Connection connection ids]
  (comment Once 1.8 is out, we should migrate this to mutating Cypher to avoid doing N requests)
  (doseq [id ids]
    (delete connection id)))

(declare first-outgoing-between)
(defn maybe-delete-outgoing
  "Deletes outgoing relationship of given type between two nodes but only if it exists.
   Otherwise, does nothing and returns nil"
  ([^Connection connection ^long id]
     (if-let [n (get id)]
       (delete connection id)))
  ([^Connection connection from to rels]
     (if-let [rel (first-outgoing-between connection from to rels)]
       (delete connection (to-id rel)))))


(defn update
  "Updates relationship data by id"
  [^Connection connection rel data]
  (rest/PUT connection (rel-properties-location-for (:endpoint connection) rel) :body (json/encode data))
  data)


(defn delete-property
  "Deletes a property from relationship with the given id"
  [^Connection connection ^long id prop]
  (rest/DELETE connection (rel-property-location-for (:endpoint connection) id prop))
  nil)


(defn starts-with?
  "Returns true if provided relationship starts with the node with the provided id,
   false otherwise"
  [rel ^long id]
  (= (extract-id (:start rel)) id))

(defn ends-with?
  "Returns true if provided relationship ends with the node with the provided id,
   false otherwise"
  [rel ^long id]
  (= (extract-id (:end rel)) id))



;;
;; Indexing
;;

(defn create-index
  "Creates a new relationship index. Indexes are used for fast lookups by a property or full text search query."
  ([^Connection connection ^String s]
     (let [{:keys [body]} (rest/POST connection (get-in connection [:endpoint :relationship-index-uri])
                                     :body (json/encode {:name (name s)}))
           payload (json/decode body true)]
       (Index. (name s) (:template payload) "lucene" "exact")))
  ([^Connection connection ^String s configuration]
     (let [{:keys [body]} (rest/POST connection (get-in connection [:endpoint :relationship-index-uri])
                                     :query-string (if (:unique configuration)
                                                     {"unique" "true"}
                                                     {})
                                     :body (json/encode (merge {:name (name s)} (dissoc configuration :unique))))
           payload (json/decode body true)]
       (Index. (name s) (:template payload) (:provider configuration) (:type configuration)))))


(defn delete-index
  "Deletes a relationship index"
  [^Connection connection ^String s]
  (let [{:keys [status]} (rest/DELETE connection (rel-index-location-for (:endpoint connection) s))]
    [s status]))


(defn all-indexes
  "Returns all relationship indices"
  [^Connection connection]
  (let [{:keys [status body]} (rest/GET connection (get-in connection [:endpoint :relationship-index-uri]))]
    (if (= 204 (long status))
      []
      (map (fn [[idx props]] (Index. (name idx) (:template props) (:provider props) (:type props)))
           (json/decode body true)))))


(defn add-to-index
  "Adds the given rel to the index"
  ([^Connection connection rel idx key value]
     (add-to-index connection rel idx key value false))
  ([^Connection connection rel idx key value unique?]
     (let [id                    (to-id rel)
           req-body              (json/encode {:key key :value value :uri (rel-location-for (:endpoint connection) (to-id rel))})
           {:keys [status body]} (rest/POST connection (rel-index-location-for (:endpoint connection) idx)
                                            :body req-body :query-string (if unique?
                                                                           {"unique" "true"}
                                                                           {}))
          payload  (json/decode body true)]
      (instantiate-rel-from payload id))))


(defn delete-from-index
  "Deletes the given rel from index"
  ([^Connection connection rel idx]
     (let [id               (to-id rel)
           {:keys [status]} (rest/DELETE connection (rel-in-index-location-for (:endpoint connection) id idx))]
       [id status]))
  ([^Connection connection rel idx key]
     (let [id               (to-id rel)
           {:keys [status]} (rest/DELETE connection (rel-in-index-location-for (:endpoint connection) id idx key))]
       [id status]))
  ([^Connection connection rel idx key value]
     (let [id               (to-id rel)
           {:keys [status]} (rest/DELETE connection (rel-in-index-location-for (:endpoint connection) id idx key value))]
       [id status])))


(defn fetch-from
  "Fetches a relationships from given URI. Exactly like clojurewerkz.neocons.rest.relationships/get but takes a URI instead of an id."
  [^Connection connection ^String uri]
  (let [{:keys [status body]} (rest/GET connection uri)
        payload (json/decode body true)
        id      (extract-id uri)]
    (instantiate-rel-from payload id)))


(defn find
  "Finds relationships using the index"
  ([^Connection connection ^String key value]
     (let [{:keys [status body]} (rest/GET connection (auto-rel-index-lookup-location-for (:endpoint connection) key value))
           xs (json/decode body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs)))
  ([^Connection connection ^String idx key value]
     (let [{:keys [status body]} (rest/GET connection (rel-index-lookup-location-for (:endpoint connection) idx key value))
           xs (json/decode body true)]
       (map (fn [doc] (fetch-from connection (:indexed doc))) xs))))

(defn find-one
  "Finds a single relationship using the index"
  [^Connection connection ^String idx key value]
  (let [{:keys [status body]} (rest/GET connection (rel-index-lookup-location-for (:endpoint connection) idx key value))
        [rel] (json/decode body true)]
    (when rel
      (fetch-from connection (:indexed rel)))))


(defn query
  "Finds relationships using full text search query"
  ([^Connection connection ^String query]
     (let [{:keys [status body]} (rest/GET connection (auto-rel-index-location-for (:endpoint connection)) :query-params {"query" query})
           xs (json/decode body true)]
       (map instantiate-rel-from xs)))
  ([^Connection connection ^String idx ^String query]
     (let [{:keys [status body]} (rest/GET connection (rel-index-location-for (:endpoint connection) idx) :query-params {"query" query})
           xs (json/decode body true)]
       (map instantiate-rel-from xs))))


;;
;; Node Operations
;;

(defn all-for
  "Returns all relationships for given node"
  [^Connection connection ^Node node &{ :keys [types] }]
  (relationships-for connection node :all types))

(defn all-ids-for
  "Returns ids of all relationships for the given node"
  [^Connection connection ^Node node &{ :keys [types] }]
  (map :id (all-for connection node :types types)))

(defn incoming-for
  "Returns incoming (inbound) relationships for the given node"
  [^Connection connection ^Node node &{ :keys [types] }]
  (relationships-for connection node :in types))

(defn outgoing-for
  "Returns all outgoing (outbound) relationships for the given node"
  [^Connection connection ^Node node &{ :keys [types] }]
  (relationships-for connection node :out types))

(defn outgoing-ids-for
  "Returns ids of all outgoing (outbound) relationships for given node."
  [^Connection connection ^Node node &{:keys [types]}]
  (map :id (outgoing-for connection node :types types)))

(defn all-outgoing-between
  "Returns all outgoing (outbound) relationships of given relationship types between two nodes"
  ([^Connection connection ^Node from ^Node to rels]
     (if (paths/exists-between? connection (:id from) (:id to) :relationships rels :max-depth 1)
       (let [rels (outgoing-for connection from :types rels)
             uri  (node-location-for (:endpoint connection) (:id to))]
         (filter #(= (:end %) uri) rels))
       [])))

(defn first-outgoing-between
  "Returns first outgoing (outbound) relationships of given relationship types between two nodes"
  ([^Connection connection ^Node from ^Node to types]
     (first (all-outgoing-between connection from to types))))


(defn purge-all
  "Deletes all relationships for given node. Usually used before deleting the node,
   because Neo4J won't allow nodes with relationships to be deleted. Nodes are deleted sequentially
   to avoid node locking problems with Neo4J Server before 1.8"
  ([^Connection connection ^Node node]
     (delete-many connection (all-ids-for connection node))))

(defn purge-outgoing
  "Deletes all outgoing relationships for given node. Nodes are deleted sequentially
   to avoid node locking problems with Neo4J Server before 1.8"
  ([^Connection connection ^Node node]
     (delete-many connection (outgoing-ids-for connection node)))
  ([^Connection connection ^Node node &{:keys [types]}]
     (delete-many connection (outgoing-ids-for connection node :types types))))

(defn replace-outgoing
  "Deletes outgoing relationships of the node `from` with given type, then creates
   new relationships of the same type with `xs` nodes"
  ([^Connection connection ^Node from xs rel-type]
     (purge-outgoing connection from :types [rel-type])
     (create-many connection from xs rel-type)))


;;
;; Rarely used
;;

(defn all-types
  "Returns all relationship types that exists in the entire database"
  [^Connection connection]
  (let [{ :keys [_ _  body] } (rest/GET connection (:relationship-types-uri (:endpoint connection)))]
    (json/decode body true)))


(defn traverse
  "Performs relationships traversal"
  ([^Connection connection id & {:keys [order relationships uniqueness prune-evaluator return-filter max-depth]
                                 :or {order           "breadth_first"
                                      uniqueness      "node_global"
                                      prune-evaluator {:language "builtin" :name "none"}
                                      return-filter   {:language "builtin" :name "all"}}}]
     (let [request-body {:order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth}
           {:keys [status body]} (rest/POST connection (rel-traverse-location-for (:endpoint connection) id) :body (json/encode request-body))
           xs (json/decode body true)]
       (map instantiate-rel-from xs))))
