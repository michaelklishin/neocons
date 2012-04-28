(ns clojurewerkz.neocons.rest.relationships
  (:import  [java.net URI URL]
            clojurewerkz.neocons.rest.Neo4JEndpoint
            [clojurewerkz.neocons.rest.records Node Relationship])
  (:require [clojure.data.json                 :as json]
            [clojurewerkz.neocons.rest         :as rest]
            [clojurewerkz.neocons.rest.paths   :as paths])
  (:use     clojurewerkz.support.http.statuses
            clojurewerkz.neocons.rest.helpers
            clojurewerkz.neocons.rest.records
            [clojure.string :only [join]])
  (:refer-clojure :exclude [get]))

;;
;; Implementation
;;

(defn- relationships-location-for
  [^Neo4JEndpoint endpoint ^Node node kind types]
  (let [query-params (if types
                       (str "/" (join "&" (map name types)))
                       "")]
    (str (:node-uri endpoint) "/" (:id node) "/relationships/" (name kind) query-params)))

(defn- relationships-for
  [^Node node kind types]
  (let [{ :keys [status headers body] } (rest/GET (relationships-location-for rest/*endpoint* node kind types))
        xs  (json/read-json body true)]
    (if (missing? status)
      nil
      (map instantiate-rel-from xs))))


;;
;; API
;;

(defn create
  "Creates a relationship of given type between two nodes. "
  ([^Node from ^Node to rel-type]
     (create from to rel-type {}))
  ([^Node from ^Node to rel-type data]
     (let [{ :keys [status headers body] } (rest/POST (:create-relationship-uri from)
                                                      :body (json/json-str { :to (:location-uri to) :type rel-type :data data }))
           payload  (json/read-json body true)]
       (instantiate-rel-from payload))))

(defn create-many
  "Concurrently creates multiple relations of given type between the *from* node and several provded nodes.
   All relationships will be of the same time. This function should be used when number of relationships
   that need to be created is moderately high (dozens and more), otherwise it would be less efficient than
   using clojure.core/map over the same sequence of nodes"
  ([^Node from xs rel-type]
     (pmap (fn [^Node n]
             (create from n rel-type)) xs))
  ([^Node from xs rel-type data]
     (pmap (fn [^Node n]
             (create from n rel-type data)) xs)))


(declare outgoing-for)
(defn maybe-create
  "Creates a relationship of given type between two nodes, unless it already exists"
  ([^Node from ^Node to rel-type]
     (maybe-create from to rel-type {}))
  ([^Node from ^Node to rel-type data]
     (if (paths/exists-between? (:id from) (:id to) :relationships [{ :type (name rel-type) :direction "out" }] :max-depth 1)
       (let [rels (outgoing-for from :types [rel-type])
             uri  (node-location-for rest/*endpoint* (:id to))]
         (first (filter #(= (:end-uri %) uri) rels)))
       (create from to rel-type data))))

(defn get
  "Fetches relationship by id"
  [^long id]
  (let [{ :keys [status headers body] } (rest/GET (rel-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (if (missing? status)
      nil
      (instantiate-rel-from payload id))))

(defn delete
  "Deletes relationship by id"
  [^long id]
  (let [{ :keys [status headers] } (rest/DELETE (rel-location-for rest/*endpoint* id))]
    (if (or (missing? status)
            (conflict? status))
      [nil status]
      [id  status])))

(defn maybe-delete
  "Deletes relationship by id but only if it exists. Otherwise, does nothing and returns nil"
  [^long id]
  (if-let [n (get id)]
    (delete id)))

(declare first-outgoing-between)
(defn maybe-delete-outgoing
  "Deletes outgoing relationship of given type between two nodes but only if it exists.
   Otherwise, does nothing and returns nil"
  ([^long id]
     (if-let [n (get id)]
       (delete id)))
  ([^Node from ^Node to rels]
     (if-let [rel (first-outgoing-between from to rels)]
       (delete (:id rel)))))


(defn update
  "Updates relationship data by id"
  [^long id data]
  (rest/PUT (rel-properties-location-for rest/*endpoint* id) :body (json/json-str data))
  data)


(defn delete-property
  "Deletes a property from relationship with the given id"
  [^long id prop]
  (rest/DELETE (rel-property-location-for rest/*endpoint* id prop))
  nil)



(defn all-for
  "Returns all relationships for given node"
  [^Node node &{ :keys [types] }]
  (relationships-for node :all types))

(defn all-ids-for
  "Returns ids of all relationships for the given node"
  [^Node node &{ :keys [types] }]
  (map :id (all-for node :types types)))

(defn incoming-for
  "Returns incoming (inbound) relationships for the given node"
  [^Node node &{ :keys [types] }]
  (relationships-for node :in types))

(defn outgoing-for
  "Returns all outgoing (outbound) relationships for the given node"
  [^Node node &{ :keys [types] }]
  (relationships-for node :out types))

(defn outgoing-ids-for
  "Returns ids of all outgoing (outbound) relationships for given node."
  [^Node node &{ :keys [types] }]
  (map :id (outgoing-for node :types types)))

(defn all-outgoing-between
  "Returns all outgoing (outbound) relationships of given relationship types between two nodes"
  ([^Node from ^Node to rels]
     (if (paths/exists-between? (:id from) (:id to) :relationships rels :max-depth 1)
       (let [rels (outgoing-for from :types rels)
             uri  (node-location-for rest/*endpoint* (:id to))]
         (filter #(= (:end-uri %) uri) rels))
       [])))

(defn first-outgoing-between
  "Returns first outgoing (outbound) relationships of given relationship types between two nodes"
  ([^Node from ^Node to types]
     (first (all-outgoing-between from to types))))


(defn purge-all
  "Deletes all relationships for given node. Usually used before deleting the node,
   because Neo4J won't allow nodes with relationships to be deleted. Nodes are deleted sequentially
   to avoid node locking problems with Neo4J Server before 1.8"
  ([^Node node]
     (doseq [id (all-ids-for node)]
       (delete id))))

(defn purge-outgoing
  "Deletes all outgoing relationships for given node. Nodes are deleted sequentially
   to avoid node locking problems with Neo4J Server before 1.8"
  ([^Node node]
     (doseq [id (outgoing-ids-for node)]
       (delete id))))

(defn all-types
  "Returns all relationship types that exists in the entire database"
  []
  (let [{ :keys [_ _  body] } (rest/GET (:relationship-types-uri rest/*endpoint*))]
    (json/read-json body true)))


(defn traverse
  "Performs relationships traversal"
  ([id & { :keys [order relationships uniqueness prune-evaluator return-filter max-depth] :or {order         "breadth_first"
                                                                                               uniqueness    "node_global"
                                                                                               prune-evaluator {:language "builtin" :name "none"}
                                                                                               return-filter   {:language "builtin" :name "all"}}}]
     (let [request-body {:order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth}
           {:keys [status body]} (rest/POST (rel-traverse-location-for rest/*endpoint* id) :body (json/json-str request-body))
           xs (json/read-json body true)]
       (map (fn [doc]
              (instantiate-rel-from doc)) xs))))
