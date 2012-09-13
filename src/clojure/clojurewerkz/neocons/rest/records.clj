(ns clojurewerkz.neocons.rest.records
  (:require clojurewerkz.neocons.rest)
  (:use clojurewerkz.neocons.rest.helpers
        [clojurewerkz.neocons.rest.conversion :only [to-id]])
  (:import clojurewerkz.neocons.rest.Neo4JEndpoint))

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

(defn rel-index-location-for
  [^Neo4JEndpoint endpoint idx]
  (str (:relationship-index-uri endpoint) "/" (encode idx)))

(defn node-in-index-location-for
  ([^Neo4JEndpoint endpoint ^long id idx]
     (str (:node-index-uri endpoint) "/" (encode idx) "/" id))
  ([^Neo4JEndpoint endpoint ^long id idx key]
     (str (:node-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" id))
  ([^Neo4JEndpoint endpoint id idx key value]
     (str (:node-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" (encode (str value)) "/" id)))

(defn rel-in-index-location-for
  ([^Neo4JEndpoint endpoint ^long id idx]
     (str (:relationship-index-uri endpoint) "/" (encode idx) "/" id))
  ([^Neo4JEndpoint endpoint ^long id idx key]
     (str (:relationship-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" id))
  ([^Neo4JEndpoint endpoint id idx key value]
     (str (:relationship-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" (encode (str value)) "/" id)))

(defn node-index-lookup-location-for
  [^Neo4JEndpoint endpoint ^String idx key value]
  (str (:node-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" (encode (str value))))

(defn auto-node-index-location-for
  [^Neo4JEndpoint endpoint]
  (str (:uri endpoint) "index/auto/node/"))

(defn auto-node-index-lookup-location-for
  [^Neo4JEndpoint endpoint key value]
  (str (auto-node-index-location-for endpoint) (encode key) "/" (encode (str value))))


(defn rel-index-lookup-location-for
  [^Neo4JEndpoint endpoint ^String idx key value]
  (str (:relationship-index-uri endpoint) "/" (encode idx) "/" (encode key) "/" (encode (str value))))

(defn auto-rel-index-location-for
  [^Neo4JEndpoint endpoint]
  (str (:uri endpoint) "index/auto/relationship/"))

(defn auto-rel-index-lookup-location-for
  [^Neo4JEndpoint endpoint key value]
  (str (auto-rel-index-location-for endpoint) (encode key) "/" (encode (str value))))


(defn node-traverse-location-for
  [^Neo4JEndpoint endpoint rel]
  (str (:node-uri endpoint) "/" (to-id rel) "/traverse/node"))

(defn path-traverse-location-for
  [^Neo4JEndpoint endpoint rel]
  (str (:node-uri endpoint) "/" (to-id rel) "/traverse/path"))

(defn paths-location-for
  [^Neo4JEndpoint endpoint rel]
  (str (:node-uri endpoint) "/" (to-id rel) "/paths"))

(defn path-location-for
  [^Neo4JEndpoint endpoint rel]
  (str (:node-uri endpoint) "/" (to-id rel) "/path"))

(defn rel-properties-location-for
  [^Neo4JEndpoint endpoint rel]
  (str (:relationships-uri endpoint) "/" (to-id rel) "/properties"))

(defn rel-property-location-for
  [^Neo4JEndpoint endpoint rel prop]
  (str (rel-properties-location-for endpoint (to-id rel)) "/" (name prop)))

(defn rel-traverse-location-for
  [^Neo4JEndpoint endpoint rel]
  (str (:node-uri endpoint) "/" (to-id rel) "/traverse/relationship"))

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
