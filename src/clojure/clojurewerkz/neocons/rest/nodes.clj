;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.nodes
  (:require [clj-http.client                         :as http]
            [cheshire.core                           :as json]
            [clojurewerkz.neocons.rest               :as rest]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.cypher        :as cypher]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers  :refer :all]
            [clojurewerkz.neocons.rest.records  :refer :all]
            [clojure.string :refer [join]]
            [clojurewerkz.neocons.rest.conversion :refer [to-id]])
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Connection]
            [clojurewerkz.neocons.rest.records Node Relationship Index]
            clojure.lang.Named)
  (:refer-clojure :exclude [get find]))

;;
;; Implementation
;;




;;
;; API
;;

(declare add-to-index)
(defn create
  "Creates and returns a node with given properties. 0-arity creates a node without properties."
  ([^Connection connection]
     (create connection {}))
  ([^Connection connection data]
     (let [{:keys [status headers body]} (rest/POST connection (get-in connection [:endpoint :node-uri]) :body (json/encode data))
           payload  (json/decode body true)
           location (:self payload)]
       (Node. (extract-id location) location data (:relationships payload) (:create_relationship payload))))
  ([^Connection connection data indexes]
     (let [node (create connection data)]
       (doseq [[idx [k v]] indexes]
         (add-to-index connection node idx k v))
       node)))

(defn create-unique-in-index
  "Atomically creates and returns a node with the given properties and adds it to an index while ensuring key uniqueness
   in that index. This is the same as first creating a node using the `clojurewerkz.neocons.rest.nodes/create` function
   and indexing it with the 4-arity of `clojurewerkz.neocons.rest.nodes/add-to-index` but performed atomically and requires
   only a single request.

   For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-unique-indexes.html section (19.8.1)"
  [^Connection connection idx k v data]
  (let [req-body    (json/encode {:key k :value v :properties data})
        uri         (str (url-with-path (get-in connection [:endpoint :node-index-uri]) idx) "?unique")
        {:keys [status headers body]} (rest/POST connection uri :body req-body)
        payload  (json/decode body true)
        location (:self payload)]
    (Node. (extract-id location) location (:data payload) (:relationships payload) (:create_relationship payload))))

(defn create-batch
  "Does an efficient batch insert of multiple nodes. Use it if you need to insert tens of hundreds of thousands
   of nodes.

   This function returns a lazy sequence of results, so you may need to force it using clojure.core/doall"
  [^Connection connection xs]
  (let [batched (doall (reduce (fn [acc x]
                                 (conj acc {:body   x
                                            :to     "/node"
                                            :method "POST"})) [] xs))
        {:keys [status headers body]} (rest/POST connection (get-in connection [:endpoint :batch-uri]) :body (json/encode batched))
        payload                       (map :body (json/decode body true))]
    (map instantiate-node-from payload)))

(defn get
  "Fetches a node by id"
  [^Connection connection ^long id]
  (let [{:keys [status body]} (rest/GET connection (node-location-for (:endpoint connection) id))
        payload  (json/decode body true)]
    (instantiate-node-from payload id)))

(defn get-properties
  [^Connection connection ^long id]
  (let [{:keys [status headers body]} (rest/GET connection (node-properties-location-for (:endpoint connection) id))]
    (case (long status)
      200 (json/decode body true)
      204 {}
      (throw (Exception. (str "Unexpected response from the server: " status ", expected 200 or 204"))))))

(defn get-many
  "Fetches multiple nodes by id.

  This is a non-standard operation that requires Cypher support as well as support for that very feature
  by Cypher itself (Neo4j Server versions 1.6.3 and later)."
  ([^Connection connection coll]
     (let [{:keys [data]} (cypher/query connection "START x = node({ids}) RETURN x" {:ids coll})]
       (map (comp instantiate-node-from first) data))))

(defn ^{:deprecated true} multi-get
  "Fetches multiple nodes by id. Deprecated, please use get-many instead."
  [^Connection connection coll]
  (apply get-many connection coll))


(defmulti update
  "Updated a node's data (properties)"
  (fn [_ node _]
    (class node)))

(defmethod update Node
  [^Connection connection ^Node node data]
  (update connection (:id node) data))

(defmethod update Long
  [^Connection connection ^long id data]
  (rest/PUT connection (node-properties-location-for (:endpoint connection) id) :body (json/encode data))
  data)


(defmulti delete
  "Deletes a node. The node must have no relationships"
  (fn [_ node]
    (class node)))

(defmethod delete Node
  [^Connection connection ^Node node]
  (delete connection (:id node)))

(defmethod delete Long
  [^Connection connection ^long id]
  (let [{:keys [status headers]} (rest/DELETE connection (node-location-for (:endpoint connection) id))]
      [id status]))


(defmulti destroy
  "Deletes a node and all of its relationships"
  (fn [_ node]
    (class node)))

(defmethod destroy Node
  [^Connection connection ^Node node]
  (relationships/purge-all connection node)
  (delete connection (:id node)))

(defmethod destroy Long
  [^Connection connection ^long id]
  ;; a little hack. Relationships purging implementation only needs
  ;; id to be set on Node so we don't have to fetch the entire set of properties. MK.
  (relationships/purge-all connection (Node. id nil nil nil nil))
  (delete connection id))


(defmulti set-property
  "Sets a single property on the given node"
  (fn [_ node _ _]
    (class node)))

(defmethod set-property Node
  [^Connection connection ^Node node prop value]
  (set-property connection (:id node) prop value))

(defmethod set-property Long
  [^Connection connection ^long id prop value]
  (rest/PUT connection (node-property-location-for (:endpoint connection) id prop) :body (json/encode value))
  value)


(defn delete-many
  "Deletes multiple nodes"
  [^Connection connection xs]
  (comment Once 1.8 is out, we should migrate this to mutating Cypher to avoid doing N requests)
  (doseq [x xs]
    (delete connection x)))

(defn destroy-many
  "Destroys multiple nodes and all of their relationships"
  [^Connection connection xs]
  (comment Once 1.8 is out, we should migrate this to mutating Cypher to avoid doing N requests)
  (doseq [x xs]
    (destroy connection x)))

(defn delete-properties
  [^Connection connection ^long id]
  (let [{:keys [status headers]} (rest/PUT connection (node-properties-location-for (:endpoint connection) id))]
    [id status]))


(defn create-index
  "Creates a new node index. Indexes are used for fast lookups by a property or full text search query."
  ([^Connection connection ^String s]
     (let [{:keys [body]} (rest/POST connection (get-in connection [:endpoint :node-index-uri])
                                     :body (json/encode {:name (name s)}))
           payload (json/decode body true)]
       (Index. (name s) (:template payload) "lucene" "exact")))
  ([^Connection connection ^String s configuration]
     (let [{:keys [body]} (rest/POST connection (get-in connection [:endpoint :node-index-uri])
                                     :query-string (if (:unique configuration)
                                                     {"unique" "true"}
                                                     {})
                                     :body (json/encode (merge {:name (name s)} {:config (dissoc configuration :unique)})))
           payload (json/decode body true)]
       (Index. (name s) (:template payload) (:provider configuration) (:type configuration)))))

(defn delete-index
  "Deletes a node index"
  [^Connection connection ^String s]
  (let [{:keys [status]} (rest/DELETE connection (node-index-location-for (:endpoint connection) s))]
    [s status]))


(defn all-indexes
  "Returns all node indices"
  [^Connection connection]
  (let [{:keys [status body]} (rest/GET connection (get-in connection [:endpoint :node-index-uri]))]
    (if (= 204 (long status))
      []
      (map (fn [[idx props]] (Index. (name idx) (:template props) (:provider props) (:type props)))
           (json/decode body true)))))


(defn add-to-index
  "Adds the given node to the index"
  ([^Connection connection node idx ^String key value]
     (add-to-index connection node idx key value false))
  ([^Connection connection node idx ^String key value unique?]
     (let [id                    (to-id node)
           req-body              (json/encode {:key (name key) :value value :uri (node-location-for (:endpoint connection) (to-id node))})
           {:keys [status body]} (rest/POST connection (node-index-location-for (:endpoint connection) idx)
                                            :body req-body
                                            :query-string (if unique?
                                                            {"unique" "true"}
                                                            {}))
           payload  (json/decode body true)]
       (instantiate-node-from payload id))))


(defn delete-from-index
  "Deletes the given node from index"
  ([^Connection connection node idx]
     (let [id               (to-id node)
           {:keys [status]} (rest/DELETE connection (node-in-index-location-for (:endpoint connection)  id idx))]
       [id status]))
  ([^Connection connection node idx key]
     (let [id               (to-id node)
           {:keys [status]} (rest/DELETE connection (node-in-index-location-for (:endpoint connection) id idx key))]
       [id status]))
  ([^Connection connection node idx key value]
     (let [id               (to-id node)
           {:keys [status]} (rest/DELETE connection (node-in-index-location-for (:endpoint connection) id idx key value))]
       [id status])))


(defn fetch-from
  "Fetches a node from given URI. Exactly like clojurewerkz.neocons.rest.nodes/get but takes a URI instead of an id."
  [^Connection connection ^String uri]
  (let [{:keys [status body]} (rest/GET connection uri)
        payload (json/decode body true)
        id      (extract-id uri)]
    (instantiate-node-from payload id)))


(defn find
  "Finds nodes using the index"
  ([^Connection connection ^String key value]
     (let [{:keys [status body]} (rest/GET connection (auto-node-index-lookup-location-for (:endpoint connection) key value))
           xs (json/decode body true)]
       (map (fn [doc] (fetch-from connection (:indexed doc))) xs)))
  ([^Connection connection ^String idx ^String  key value]
     (let [{:keys [status body]} (rest/GET connection (node-index-lookup-location-for (:endpoint connection) idx key value))
           xs (json/decode body true)]
       (map (fn [doc] (fetch-from connection (:indexed doc))) xs))))

(defn find-one
  "Finds a single node using the index"
  [^Connection connection ^String idx ^String key value]
  (let [{:keys [status body]} (rest/GET connection (node-index-lookup-location-for (:endpoint connection) idx key value))
        [node] (json/decode body true)]
    (when node
      (fetch-from connection (:indexed node)))))


(defn query
  "Finds nodes using full text search query"
  ([^Connection connection ^String query]
     (let [{:keys [status body]} (rest/GET connection (auto-node-index-location-for (:endpoint connection))
                                           :query-params {"query" query})
           xs (json/decode body true)]
       (map instantiate-node-from xs)))
  ([^Connection connection ^String idx ^String query]
     (let [{:keys [status body]} (rest/GET connection (node-index-location-for (:endpoint connection) idx)
                                           :query-params {"query" query})
           xs (json/decode body true)]
       (map instantiate-node-from xs))))


(defn traverse
  "Performs node traversal"
  ([^Connection connection id & {:keys [order relationships
                                        uniqueness prune-evaluator
                                        return-filter max-depth] :or {order         "breadth_first"
                                                                      uniqueness    "node_global"
                                                                      prune-evaluator {:language "builtin" :name "none"}
                                                                      return-filter   {:language "builtin" :name "all"}}}]
     (let [request-body {:order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth}
           {:keys [status body]} (rest/POST connection (node-traverse-location-for (:endpoint connection) id)
                                            :body (json/encode request-body))
           xs (json/decode body true)]
       (map instantiate-node-from xs))))


(defn all-connected-out
  "Returns all nodes given node has outgoing (outbound) relationships with"
  [^Connection connection id &{:keys [types]}]
  (let [rels (relationships/outgoing-for connection (get connection id) :types types)
        ids  (set (map #(extract-id (:end %)) rels))]
    (get-many connection ids)))

(defn connected-out?
  "Returns true if given node has outgoing (outbound) relationships with the other node"
  [^Connection connection id other-id &{:keys [types]}]
  (let [rels (relationships/outgoing-for connection (get connection id) :types types)
        uris (set (map :end rels))]
    (uris (node-location-for (:endpoint connection) other-id))))
