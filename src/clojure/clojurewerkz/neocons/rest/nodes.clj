(ns clojurewerkz.neocons.rest.nodes
  (:use     clojurewerkz.support.http.statuses
            clojurewerkz.neocons.rest.helpers
            clojurewerkz.neocons.rest.records
            [clojure.string :only [join]]
            [clojurewerkz.neocons.rest.conversion :only [to-id]])
  (:require [clj-http.client                         :as http]
            [cheshire.custom                       :as json]
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
     (let [{:keys [status headers body]} (rest/POST (:node-uri rest/*endpoint*) :body (json/encode data))
           payload  (json/decode body true)
           location (:self payload)]
       (Node. (extract-id location) location data (:relationships payload) (:create_relationship payload))))
  ([data indexes]
     (let [node (create data)]
       (doseq [[idx [k v]] indexes]
         (add-to-index node idx k v))
       node)))

(defn create-unique-in-index
  "Atomically creates and returns a node with the given properties and adds it to an index while ensuring key uniqueness
   in that index. This is the same as first creating a node using the `clojurewerkz.neocons.rest.nodes/create` function
   and indexing it with the 4-arity of `clojurewerkz.neocons.rest.nodes/add-to-index` but performed atomically and requires
   only a single request.

   For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-unique-indexes.html section (19.8.1)"
  [idx k v data]
  (let [req-body    (json/encode {:key k :value v :properties data})
        uri         (str (url-with-path (:node-index-uri rest/*endpoint*) idx) "?unique")
        {:keys [status headers body]} (rest/POST uri :body req-body)
        payload  (json/decode body true)
        location (:self payload)]
    (Node. (extract-id location) location (:data payload) (:relationships payload) (:create_relationship payload))))

(defn create-batch
  "Does an efficient batch insert of multiple nodes. Use it if you need to insert tens of hundreds of thousands
   of nodes.

   This function returns a lazy sequence of results, so you may need to force it using clojure.core/doall"
  [xs]
  (let [batched (doall (reduce (fn [acc x]
                                 (conj acc {:body   x
                                            :to     "/node"
                                            :method "POST"})) [] xs))
        {:keys [status headers body]} (rest/POST (:batch-uri rest/*endpoint*) :body (json/encode batched))
        payload                       (map :body (json/decode body true))]
    (map instantiate-node-from payload)))

(defn get
  "Fetches a node by id"
  [^long id]
  (let [{:keys [status body]} (rest/GET (node-location-for rest/*endpoint* id))
        payload  (json/decode body true)]
    (instantiate-node-from payload id)))

(defn get-properties
  [^long id]
  (let [{:keys [status headers body]} (rest/GET (node-properties-location-for rest/*endpoint* id))]
    (case (long status)
      200 (json/decode body true)
      204 {}
      (throw (Exception. (str "Unexpected response from the server: " status ", expected 200 or 204"))))))

(defn get-many
  "Fetches multiple nodes by id.

  This is a non-standard operation that requires Cypher support as well as support for that very feature
  by Cypher itself (Neo4j Server versions 1.6.3 and later)."
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
    (rest/PUT (node-properties-location-for rest/*endpoint* id) :body (json/encode data))
    data)
  (set-property [^long id prop value]
    (rest/PUT (node-property-location-for rest/*endpoint* id prop) :body (json/encode value))
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
     (let [{:keys [body]} (rest/POST (:node-index-uri rest/*endpoint*) :body (json/encode {:name (name s)}))
           payload (json/decode body true)]
       (Index. (name s) (:template payload) "lucene" "exact")))
  ([^String s configuration]
     (let [{:keys [body]} (rest/POST (:node-index-uri rest/*endpoint*)
                                     :query-string (if (:unique configuration)
                                                     {"unique" "true"}
                                                     {})
                                     :body (json/encode (merge {:name (name s)} {:config (dissoc configuration :unique)})))
           payload (json/decode body true)]
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
           (json/decode body true)))))


(defn add-to-index
  "Adds the given node to the index"
  ([node idx ^String key value]
     (add-to-index node idx key value false))
  ([node idx ^String key value unique?]
     (let [id                    (to-id node)
           req-body              (json/encode {:key (name key) :value value :uri (node-location-for rest/*endpoint* (to-id node))})
           {:keys [status body]} (rest/POST (node-index-location-for rest/*endpoint* idx) :body req-body :query-string (if unique?
                                                                                                                         {"unique" "true"}
                                                                                                                         {}))
           payload  (json/decode body true)]
       (instantiate-node-from payload id))))


(defn delete-from-index
  "Deletes the given node from index"
  ([node idx]
     (let [id               (to-id node)
           {:keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx))]
       [id status]))
  ([node idx key]
     (let [id               (to-id node)
           {:keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx key))]
       [id status]))
  ([node idx key value]
     (let [id               (to-id node)
           {:keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx key value))]
       [id status])))


(defn fetch-from
  "Fetches a node from given URI. Exactly like clojurewerkz.neocons.rest.nodes/get but takes a URI instead of an id."
  [^String uri]
  (let [{:keys [status body]} (rest/GET uri)
        payload (json/decode body true)
        id      (extract-id uri)]
    (instantiate-node-from payload id)))


(defn find
  "Finds nodes using the index"
  ([^String key value]
     (let [{:keys [status body]} (rest/GET (auto-node-index-lookup-location-for rest/*endpoint* key value))
           xs (json/decode body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs)))
  ([^String idx ^String  key value]
     (let [{:keys [status body]} (rest/GET (node-index-lookup-location-for rest/*endpoint* idx key value))
           xs (json/decode body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs))))

(defn find-one
  "Finds a single node using the index"
  [^String idx ^String key value]
  (let [{:keys [status body]} (rest/GET (node-index-lookup-location-for rest/*endpoint* idx key value))
        [node] (json/decode body true)]
    (when node
      (fetch-from (:indexed node)))))


(defn query
  "Finds nodes using full text search query"
  ([^String query]
     (let [{:keys [status body]} (rest/GET (auto-node-index-location-for rest/*endpoint*) :query-params {"query" query})
           xs (json/decode body true)]
       (map (fn [doc] (instantiate-node-from doc)) xs)))
  ([^String idx ^String query]
     (let [{:keys [status body]} (rest/GET (node-index-location-for rest/*endpoint* idx) :query-params {"query" query})
           xs (json/decode body true)]
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
           {:keys [status body]} (rest/POST (node-traverse-location-for rest/*endpoint* id) :body (json/encode request-body))
           xs (json/decode body true)]
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
