(ns org.clojurewerkz.neocons.rest.nodes
  (:import  [java.net URI URL]
            [org.clojurewerkz.neocons.rest Neo4JEndpoint])
  (:require [clj-http.client               :as http]
            [clojure.data.json             :as json]
            [org.clojurewerkz.neocons.rest :as rest])
  (:use     [org.clojurewerkz.neocons.rest.statuses]
            [clojure.string :only [join]])
  (:refer-clojure :exclude (get)))

;;
;; Implementation
;;

(defrecord Node
    [id location-uri data relationships-uri])

(defn extract-id
  [^String location]
  (let [url (URL. location)]
    (Long/valueOf ^String (first (re-seq #"\d+$" (.getPath url))))))

(defn- instantiate-node-from
  ([^long status headers payload ^long id]
     (Node. id (:self payload) (:data payload) (:all_relationships payload))))

(defn node-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id))


;;
;; API
;;

(defn create
  [&{ :keys [data] :or { data {} } }]
  (let [{ :keys [status headers body] } (rest/POST (:node-uri rest/*endpoint*) :body (json/json-str data))
        payload  (json/read-json body true)
        location (:self payload)]
    (Node. (extract-id location) location data (:relationships payload))))

(defn get
  [^long id]
  (let [{ :keys [status headers body] } (rest/GET (node-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (if (missing? status)
      nil
      (instantiate-node-from status headers payload id))))

(defn delete
  [^long id]
  (let [{ :keys [status headers] } (rest/DELETE (node-location-for rest/*endpoint* id))]
    (if (missing? status)
      nil
      id)))
