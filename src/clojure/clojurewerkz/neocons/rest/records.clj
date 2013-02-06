(ns clojurewerkz.neocons.rest.records
  (:require clojurewerkz.neocons.rest
            [clojurewerkz.urly.core :as u]
            [clojure.string :as s])
  (:use clojurewerkz.neocons.rest.helpers
        [clojurewerkz.neocons.rest.conversion :only [to-id]])
  (:import clojurewerkz.neocons.rest.Neo4JEndpoint
           java.net.URLEncoder))

(defrecord Node
    [id location-uri data relationships-uri create-relationship-uri])

(defrecord Relationship
    [id location-uri start end type data])

(defrecord Index
    [^String name ^String template ^String provider ^String type])

(defrecord Path
    [start end length nodes relationships])

(defrecord CypherQueryResponse
    [data columns])



(def ^{:const true} slash    "/")

(defn ^String encode-slashes
  [^String s]
  (.replaceAll s "/" "%2F"))

(defn ^String encode-segment
  [^String s]
  (encode-slashes (u/encode-path s)))



(defn ^String url-with-path
  [^String root & segments]
  (str root slash (s/join slash segments)))

(defn ^String root-with-path
  [^Neo4JEndpoint endpoint & segments]
  (str (:uri endpoint) slash (s/join slash segments)))

(defn node-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (url-with-path (:node-uri endpoint) id))

(defn rel-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (url-with-path (:relationships-uri endpoint) id))

(defn node-properties-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (url-with-path (:node-uri endpoint) id "properties"))

(defn node-property-location-for
  [^Neo4JEndpoint endpoint ^long id prop]
  (url-with-path (:node-uri endpoint) id "properties" (encode-segment (name prop))))

(defn node-index-location-for
  [^Neo4JEndpoint endpoint idx]
  (url-with-path (:node-index-uri endpoint) (encode-segment idx)))

(defn rel-index-location-for
  [^Neo4JEndpoint endpoint idx]
  (url-with-path (:relationship-index-uri endpoint) (encode-segment idx)))

(defn node-in-index-location-for
  ([^Neo4JEndpoint endpoint ^long id idx]
     (url-with-path (:node-index-uri endpoint) (encode-segment idx) id))
  ([^Neo4JEndpoint endpoint ^long id idx key]
     (url-with-path (:node-index-uri endpoint) (encode-segment idx) (encode-segment key) id))
  ([^Neo4JEndpoint endpoint id idx key value]
     (url-with-path (:node-index-uri endpoint) (encode-segment idx) (encode-segment key) (encode-segment (str value)) id)))

(defn rel-in-index-location-for
  ([^Neo4JEndpoint endpoint ^long id idx]
     (url-with-path (:relationship-index-uri endpoint) (encode-segment idx) id))
  ([^Neo4JEndpoint endpoint ^long id idx key]
     (url-with-path (:relationship-index-uri endpoint) (encode-segment idx) (encode-segment key) id))
  ([^Neo4JEndpoint endpoint id idx key value]
     (url-with-path (:relationship-index-uri endpoint) (encode-segment idx) (encode-segment key) (encode-segment (str value)) id)))

(defn node-index-lookup-location-for
  [^Neo4JEndpoint endpoint ^String idx key value]
  (url-with-path (:node-index-uri endpoint) (encode-segment idx) (encode-segment key) (encode-segment (str value))))

(defn auto-node-index-location-for
  [^Neo4JEndpoint endpoint]
  (root-with-path endpoint "index" "auto" "node"))

(defn auto-node-index-lookup-location-for
  [^Neo4JEndpoint endpoint key value]
  (url-with-path (auto-node-index-location-for endpoint) (encode-segment key) (encode-segment (str value))))


(defn rel-index-lookup-location-for
  [^Neo4JEndpoint endpoint ^String idx key value]
  (url-with-path (:relationship-index-uri endpoint) (encode-segment idx) (encode-segment key) (encode-segment (str value))))

(defn auto-rel-index-location-for
  [^Neo4JEndpoint endpoint]
  (str (root-with-path endpoint) "index" "auto" "relationship"))

(defn auto-rel-index-lookup-location-for
  [^Neo4JEndpoint endpoint key value]
  (root-with-path (auto-rel-index-location-for endpoint) (encode-segment key) (encode-segment (str value))))


(defn node-traverse-location-for
  [^Neo4JEndpoint endpoint rel]
  (url-with-path (:node-uri endpoint) (to-id rel) "traverse" "node"))

(defn path-traverse-location-for
  [^Neo4JEndpoint endpoint rel]
  (url-with-path (:node-uri endpoint) (to-id rel) "traverse" "path"))

(defn paths-location-for
  [^Neo4JEndpoint endpoint rel]
  (url-with-path (:node-uri endpoint) (to-id rel) "paths"))

(defn path-location-for
  [^Neo4JEndpoint endpoint rel]
  (url-with-path (:node-uri endpoint) (to-id rel) "path"))

(defn rel-properties-location-for
  [^Neo4JEndpoint endpoint rel]
  (url-with-path (:relationships-uri endpoint) (to-id rel) "properties"))

(defn rel-property-location-for
  [^Neo4JEndpoint endpoint rel prop]
  (url-with-path (rel-properties-location-for endpoint (to-id rel)) (encode-segment (name prop))))

(defn rel-traverse-location-for
  [^Neo4JEndpoint endpoint rel]
  (url-with-path (:node-uri endpoint) (to-id rel) "traverse" "relationship"))

(defn instantiate-node-from
  ([payload]
     (let [id (extract-id (:self payload))]
       (Node. id (:self payload) (:data payload) (:all_relationships payload) (:create_relationship payload))))
  ([payload ^long id]
     (Node. id (:self payload) (:data payload) (:all_relationships payload) (:create_relationship payload))))

(defn instantiate-rel-from
  ([payload]
     (let [id (extract-id (:self payload))]
       (Relationship. id (:self payload) (:start payload) (:end payload) (:type payload) (:data payload))))
  ([payload ^long id]
     (Relationship. id (:self payload) (:start payload) (:end payload) (:type payload) (:data payload))))

(defn instantiate-path-from
  [payload]
  (map->Path payload))

(defn instantiate-cypher-query-response-from
  [payload]
  (map->CypherQueryResponse payload))


(defn instantiate-record-from
  "Instantiates a record from the given payload, detecting what kind
  of Neo4J entity (a node, a relationship, a path) this payload
  represents. Defaults to returning the object if we don't know how to
  deal with it."
  [payload]
  (let [f (cond (:create_relationship payload)   instantiate-node-from
                (and (:type payload)
                     (:data payload))            instantiate-rel-from
                (and (:start payload)
                     (:end payload)
                     (not (:type payload)))      instantiate-path-from
                :else                            identity)]
    (f payload)))
