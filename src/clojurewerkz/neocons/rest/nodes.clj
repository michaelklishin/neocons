(ns clojurewerkz.neocons.rest.nodes
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Neo4JEndpoint]
            [clojure.lang Named])
  (:require [clj-http.client               :as http]
            [clojure.data.json             :as json]
            [clojurewerkz.neocons.rest :as rest])
  (:use     [clojurewerkz.neocons.rest.statuses]
            [clojurewerkz.neocons.rest.helpers]
            [clojure.string :only [join]])
  (:refer-clojure :exclude (get)))

;;
;; Implementation
;;

(defrecord Node
    [id location-uri data relationships-uri create-relationship-uri])

(defrecord Index
    [^String name ^String template ^String provider ^String type])

(defn- instantiate-node-from
  ([^long status headers payload ^long id]
     (Node. id (:self payload) (:data payload) (:all_relationships payload) (:create_relationship payload))))

(defn node-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id))

(defn node-properties-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/properties"))

(defn node-property-location-for
  [^Neo4JEndpoint endpoint ^long id prop]
  (str (node-properties-location-for endpoint id) "/" (name prop)))


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
  (let [{ :keys [status headers body] } (rest/GET (node-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (instantiate-node-from status headers payload id)))

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
  ([name]
     (let [{ :keys [_ _ body] } (rest/POST (:node-index-uri rest/*endpoint*) :body (json/json-str { :name name }))
           payload (json/read-json body true)]
       (Index. name (:template payload) "lucene" "exact")))
  ([name configuration]
     (let [{ :keys [_ _ body] }(rest/POST (:node-index-uri rest/*endpoint*) :body (json/json-str (merge { :name name } configuration)))
           payload (json/read-json body true)]
       (Index. name (:template payload) (:provider configuration) (:type configuration)))))
