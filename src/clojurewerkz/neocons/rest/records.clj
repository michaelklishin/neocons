(ns clojurewerkz.neocons.rest.records
  (:use clojurewerkz.neocons.rest.helpers))

(defrecord Node
    [id location-uri data relationships-uri create-relationship-uri])

(defrecord Relationship
    [id location-uri start-uri end-uri type data])

(defrecord Index
    [^String name ^String template ^String provider ^String type])

(defrecord Path
    [start end length nodes relationships])

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
