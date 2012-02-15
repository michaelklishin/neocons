(ns clojurewerkz.neocons.rest.records
  (:use clojurewerkz.neocons.rest.helpers)
  (:import [clojurewerkz.neocons.rest Neo4JEndpoint]))

(defrecord Node
    [id location-uri data relationships-uri create-relationship-uri])

(defrecord Relationship
    [id location-uri start-uri end-uri type data])

(defrecord Index
    [^String name ^String template ^String provider ^String type])

(defrecord Path
    [start end length nodes relationships])

(defrecord CypherQueryResponse
    [data columns])

(defn node-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id))

(defn rel-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:relationships-uri endpoint) "/" id))

(defn node-properties-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/properties"))

(defn node-property-location-for
  [^Neo4JEndpoint endpoint ^long id prop]
  (str (node-properties-location-for endpoint id) "/" (encode prop)))

(defn node-index-location-for
  [^Neo4JEndpoint endpoint idx]
  (str (:node-index-uri endpoint) "/" (encode idx)))

(defn node-in-index-location-for
  ([^Neo4JEndpoint endpoint ^long id idx]
     (str (:node-index-uri endpoint) "/" (encode idx) "/" id))
  ([^Neo4JEndpoint endpoint ^long id idx key]
     (str (:node-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" id))
  ([^Neo4JEndpoint endpoint id idx key value]
     (str (:node-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" (encode (str value)) "/" id)))

(defn index-lookup-location-for
  [^Neo4JEndpoint endpoint ^String idx key value]
  (str (:node-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" (encode (str value))))

(defn auto-index-location-for
  [^Neo4JEndpoint endpoint]
  (str (:uri endpoint) "index/auto/node/"))

(defn auto-index-lookup-location-for
  [^Neo4JEndpoint endpoint key value]
  (str (auto-index-location-for endpoint) (encode key) "/" (encode (str value))))

(defn node-traverse-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/traverse/node"))

(defn path-traverse-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/traverse/path"))

(defn paths-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/paths"))

(defn path-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/path"))

(defn rel-properties-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:relationships-uri endpoint) "/" id "/properties"))

(defn rel-property-location-for
  [^Neo4JEndpoint endpoint ^long id prop]
  (str (rel-properties-location-for endpoint id) "/" (name prop)))

(defn rel-traverse-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/traverse/relationship"))

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
  ([payload]
     (Path. (:start payload) (:end payload) (:length payload) (:nodes payload) (:relationships payload))))

(defn instantiate-cypher-query-response-from
  [payload]
  (CypherQueryResponse. (:data payload) (:columns payload)))
