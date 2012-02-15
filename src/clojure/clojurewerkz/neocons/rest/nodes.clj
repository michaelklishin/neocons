(ns clojurewerkz.neocons.rest.nodes
  (:use     [clojurewerkz.neocons.rest.statuses]
            [clojurewerkz.neocons.rest.helpers]
            [clojurewerkz.neocons.rest.records]
            [clojure.string :only [join]])
  (:require [clj-http.client                         :as http]
            [clojure.data.json                       :as json]
            [clojurewerkz.neocons.rest               :as rest]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths         :as paths])
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Neo4JEndpoint]
            [clojurewerkz.neocons.rest.records Node Relationship Index]
            [clojure.lang Named])
  (:refer-clojure :exclude (get find)))

;;
;; Implementation
;;




;;
;; API
;;

(defn create
  ([]
     (create {}))
  ([data]
     (let [{ :keys [status headers body] } (rest/POST (:node-uri rest/*endpoint*) :body (json/json-str data))
           payload  (json/read-json body true)
           location (:self payload)]
       (Node. (extract-id location) location data (:relationships payload) (:create_relationship payload)))))

(defn get
  [^long id]
  (let [{ :keys [status body] } (rest/GET (node-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (instantiate-node-from payload id)))

(defn delete
  [^long id]
  (let [{ :keys [status headers] } (rest/DELETE (node-location-for rest/*endpoint* id))]
    [id status]))

(defn set-property
  [^long id prop value]
  (rest/PUT (node-property-location-for rest/*endpoint* id prop) :body (json/json-str value))
  value)

(defn update
  [^long id data]
  (rest/PUT (node-properties-location-for rest/*endpoint* id) :body (json/json-str data))
  data)

(defn get-properties
  [^long id]
  (let [{ :keys [status headers body] } (rest/GET (node-properties-location-for rest/*endpoint* id))]
    (case (long status)
      200 (json/read-json body true)
      204 {}
      (throw (Exception. (str "Unexpected response from the server: " status ", expected 200 or 204"))))))

(defn delete-properties
  [^long id]
  (let [{ :keys [status headers] }(rest/PUT (node-properties-location-for rest/*endpoint* id))]
    [id status]))


(defn create-index
  ([s]
     (let [{ :keys [body] } (rest/POST (:node-index-uri rest/*endpoint*) :body (json/json-str { :name (name s) }))
           payload (json/read-json body true)]
       (Index. (name s) (:template payload) "lucene" "exact")))
  ([s configuration]
     (let [{ :keys [body] }(rest/POST (:node-index-uri rest/*endpoint*) :body (json/json-str (merge { :name (name s) } configuration)))
           payload (json/read-json body true)]
       (Index. (name s) (:template payload) (:provider configuration) (:type configuration)))))

(defn delete-index
  [s]
  (let [{ :keys [status]} (rest/DELETE (node-index-location-for rest/*endpoint* s))]
    [s status]))


(defn all-indexes
  []
  (let [{ :keys [status body] } (rest/GET (:node-index-uri rest/*endpoint*))]
    (if (= 204 (long status))
      []
      (map (fn [[idx props]] (Index. (name idx) (:template props) (:provider props) (:type props)))
           (json/read-json body true)))))


(defn add-to-index
  [^long id idx key value]
  (let [body     (json/json-str { :key key :value value :uri (node-location-for rest/*endpoint* id) })
        { :keys [status body] } (rest/POST (node-index-location-for rest/*endpoint* idx) :body body)
        payload  (json/read-json body true)]
    (instantiate-node-from payload id)))

(defn delete-from-index
  ([^long id idx]
     (let [{ :keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx))]
       [id status]))
  ([^long id idx key]
     (let [{ :keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx key))]
       [id status]))
  ([^long id idx key value]
     (let [{ :keys [status]} (rest/DELETE (node-in-index-location-for rest/*endpoint* id idx key value))]
       [id status])))


(defn fetch-from
  [^String uri]
  (let [{ :keys [status body] } (rest/GET uri)
        payload (json/read-json body true)
        id      (extract-id uri)]
    (instantiate-node-from payload id)))


(defn find
  ([^String key value]
     (let [{ :keys [status body] } (rest/GET (auto-index-lookup-location-for rest/*endpoint* key value))
           xs (json/read-json body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs)))
  ([^String idx key value]
     (let [{ :keys [status body] } (rest/GET (index-lookup-location-for rest/*endpoint* idx key value))
           xs (json/read-json body true)]
       (map (fn [doc] (fetch-from (:indexed doc))) xs))))


(defn query
  ([^String query]
     (let [{ :keys [status body] } (rest/GET (auto-index-location-for rest/*endpoint*) :query-params { "query" query })
           xs (json/read-json body true)]
       (map (fn [doc] (instantiate-node-from doc)) xs)))
  ([^String idx ^String query]
     (let [{ :keys [status body] } (rest/GET (node-index-location-for rest/*endpoint* idx) :query-params { "query" query })
           xs (json/read-json body true)]
       (map (fn [doc] (instantiate-node-from doc)) xs))))


(defn traverse
  ([id & { :keys [order relationships uniqueness prune-evaluator return-filter max-depth] :or {
                                                                                               order         "breadth_first"
                                                                                               uniqueness    "node_global"
                                                                                               prune-evaluator { :language "builtin" :name "none" }
                                                                                               return-filter   { :language "builtin" :name "all"  }
                                                                                               } }]
     (let [request-body {
                         :order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth
                         }
           { :keys [status body] } (rest/POST (node-traverse-location-for rest/*endpoint* id) :body (json/json-str request-body))
           xs (json/read-json body true)]
       (map (fn [doc]
              (instantiate-node-from doc)) xs))))


(defn all-connected-out
  "Returns all nodes given node has outgoing (outbound) relationships with"
  [id &{ :keys [types] }]
  (let [rels (relationships/outgoing-for (get id) :types types)
        uris (set (map :end-uri rels))]
    (map fetch-from uris)))

(defn connected-out?
  "Returns true if given node has outgoing (outbound) relationships with the other node"
  [id other-id &{ :keys [types] }]
  (let [rels (relationships/outgoing-for (get id) :types types)
        uris (set (map :end-uri rels))]
    (uris (node-location-for rest/*endpoint* other-id))))
