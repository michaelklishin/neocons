(ns clojurewerkz.neocons.rest.nodes
  (:use     clojurewerkz.support.http.statuses
            clojurewerkz.neocons.rest.helpers
            clojurewerkz.neocons.rest.records
            [clojure.string :only [join]])
  (:require [clj-http.client                         :as http]
            [clojure.data.json                       :as json]
            [clojurewerkz.neocons.rest               :as rest]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojurewerkz.neocons.rest.cypher        :as cypher])
  (:import  [java.net URI URL]
            clojurewerkz.neocons.rest.Neo4JEndpoint
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
  ([]
     (create {}))
  ([data]
     (let [{:keys [status headers body]} (rest/POST (:node-uri rest/*endpoint*) :body (json/json-str data))
           payload  (json/read-json body true)
           location (:self payload)]
       (Node. (extract-id location) location data (:relationships payload) (:create_relationship payload))))
  ([data indexes]
     (let [node (create data)]
       (doseq [[idx [k v]] indexes]
         (add-to-index node idx k v))
       node)))

(defn get
  "Fetches a node by id"
  [^long id]
  (let [{:keys [status body]} (rest/GET (node-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (instantiate-node-from payload id)))

(defn get-properties
  [^long id]
  (let [{:keys [status headers body]} (rest/GET (node-properties-location-for rest/*endpoint* id))]
    (case (long status)
      200 (json/read-json body true)
      204 {}
      (throw (Exception. (str "Unexpected response from the server: " status ", expected 200 or 204"))))))

(defn get-many
  "Fetches multiple nodes by id.

  This is a non-standard operation that requires Cypher support from Neo4J Server (versions 1.6.0 and later have it in the core)."
  ([coll]
     (let [{:keys [data]} (cypher/query "START x = node({ids}) RETURN x" {:ids coll})]
       (map (comp instantiate-node-from first) data))))

(defn ^{:deprecated true} multi-get
  "Fetches multiple nodes by id. Deprecated, please use get-many instead."
  [coll]
  (apply get-many coll))

(defprotocol MutatingOperations
  (delete       [node] "Deletes a node. The node must have no relationships")
  (destroy      [node] "Deletes a node and all of its relationships")
  (update       [node data] "Updates a node's data (properties)")
  (set-property [node prop value] "Sets a single property on the given node"))

(extend-protocol MutatingOperations
  Node
  (delete [^Node node]
    (delete (:id node)))
  (destroy [^Node node]
    (relationships/purge-all node)
    (delete (:id node)))
  (update [^Node node data]
    (update (:id node) data))
  (set-property [^Node node prop value]
    (set-property (:id node) prop value))

  Long
  (delete [^long id]
    (let [{:keys [status headers]} (rest/DELETE (node-location-for rest/*endpoint* id))]
      [id status]))
  (destroy [^long id]
    ;; a little hack. Relationships purging implementation only needs
    ;; id to be set on Node so we don't have to fetch the entire set of properties. MK.
    (relationships/purge-all (Node. id nil nil nil nil))
    (delete id))
  (update [^long id data]
    (rest/PUT (node-properties-location-for rest/*endpoint* id) :body (json/json-str data))
    data)
  (set-property [^long id prop value]
    (rest/PUT (node-property-location-for rest/*endpoint* id prop) :body (json/json-str value))
    value))

(defn delete-many
  "Deletes multiple nodes"
  [xs]
  (comment Once 1.8 is out, we should migrate this to mutating Cypher to avoid doing N requests)
  (doseq [x xs]
    (delete x)))

(defn destroy-many
  "Destroys multiple nodes and all of their relationships"
  [xs]
  (comment Once 1.8 is out, we should migrate this to mutating Cypher to avoid doing N requests)
  (doseq [x xs]
    (destroy x)))

(defn delete-properties
  [^long id]
  (let [{:keys [status headers]} (rest/PUT (node-properties-location-for rest/*endpoint* id))]
    [id status]))


(defn create-index
  "Creates a new node index. Indexes are used for fast lookups by a property or full text search query."
  ([^String s]
     (let [{:keys [body]} (rest/POST (:node-index-uri rest/*endpoint*) :body (json/json-str {:name (name s)}))
           payload (json/read-json body true)]
       (Index. (name s) (:template payload) "lucene" "exact")))
  ([^String s configuration]
     (let [{:keys [body]} (rest/POST (:node-index-uri rest/*endpoint*) :body (json/json-str (merge {:name (name s)} configuration)))
           payload (json/read-json body true)]
       (Index. (name s) (:template payload) (:provider configuration) (:type configuration)))))

(defn delete-index
  "Deletes a node index"
  [^String s]
  (let [{:keys [status]} (rest/DELETE (node-index-location-for rest/*endpoint* s))]
    [s status]))


(defn all-indexes
  "Returns all node indices"
  []
  (let [{:keys [status body]} (rest/GET (:node-index-uri rest/*endpoint*))]
    (if (= 204 (long status))
      []
      (map (fn [[idx props]] (Index. (name idx) (:template props) (:provider props) (:type props)))
           (json/read-json body true)))))


(defprotocol Indexable
  (add-to-index    [node index key value] "Adds the given node to the index"))

(extend-protocol Indexable
  Node
  (add-to-index [^Node node idx key value]
    (add-to-index (:id node) idx key value))

  Long
  (add-to-index [^long id idx key value]
    (let [req-body              (json/json-str {:key key :value value :uri (node-location-for rest/*endpoint* id)})
          {:keys [status body]} (rest/POST (node-index-location-for rest/*endpoint* idx) :body req-body)
          payload  (json/read-json body true)]
      (instantiate-node-from payload id))))


(defn delete-from-index
  "Deletes the given node from index"
  ([^long id idx]
     (let [{:keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx))]
       [id status]))
  ([^long id idx key]
     (let [{:keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx key))]
       [id status]))
  ([^long id idx key value]
     (let [{:keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx key value))]
       [id status])))


(defn fetch-from
  "Fetches a node from given URI. Exactly like clojurewerkz.neocons.rest.nodes/get but takes a URI instead of an id."
  [^String uri]
  (let [{:keys [status body]} (rest/GET uri)
        payload (json/read-json body true)
        id      (extract-id uri)]
    (instantiate-node-from payload id)))


(defn find
  "Finds nodes using the index"
  ([^String key value]
     (let [{:keys [status body]} (rest/GET (auto-index-lookup-location-for rest/*endpoint* key value))
           xs (json/read-json body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs)))
  ([^String idx key value]
     (let [{:keys [status body]} (rest/GET (index-lookup-location-for rest/*endpoint* idx key value))
           xs (json/read-json body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs))))

(defn find-one
  "Finds a single node using the index"
  [^String idx key value]
  (let [{:keys [status body]} (rest/GET (index-lookup-location-for rest/*endpoint* idx key value))
        [node] (json/read-json body true)]
    (when node
      (fetch-from (:indexed node)))))


(defn query
  "Finds nodes using full text search query"
  ([^String query]
     (let [{:keys [status body]} (rest/GET (auto-index-location-for rest/*endpoint*) :query-params {"query" query})
           xs (json/read-json body true)]
       (map (fn [doc] (instantiate-node-from doc)) xs)))
  ([^String idx ^String query]
     (let [{:keys [status body]} (rest/GET (node-index-location-for rest/*endpoint* idx) :query-params {"query" query})
           xs (json/read-json body true)]
       (map (fn [doc] (instantiate-node-from doc)) xs))))


(defn traverse
  "Performs node traversal"
  ([id & {:keys [order relationships
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
           {:keys [status body]} (rest/POST (node-traverse-location-for rest/*endpoint* id) :body (json/json-str request-body))
           xs (json/read-json body true)]
       (map (fn [doc]
              (instantiate-node-from doc)) xs))))


(defn all-connected-out
  "Returns all nodes given node has outgoing (outbound) relationships with"
  [id &{:keys [types]}]
  (let [rels (relationships/outgoing-for (get id) :types types)
        ids  (set (map #(extract-id (:end %)) rels))]
    (get-many ids)))

(defn connected-out?
  "Returns true if given node has outgoing (outbound) relationships with the other node"
  [id other-id &{:keys [types]}]
  (let [rels (relationships/outgoing-for (get id) :types types)
        uris (set (map :end rels))]
    (uris (node-location-for rest/*endpoint* other-id))))
