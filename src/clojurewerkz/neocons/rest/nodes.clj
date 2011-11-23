(ns org.clojurewerkz.neocons.rest.nodes
  (:import  [java.net URI URL]
            [org.clojurewerkz.neocons.rest Neo4JEndpoint])
  (:require [clj-http.client               :as http]
            [clojure.data.json             :as json]
            [org.clojurewerkz.neocons.rest :as rest])
  (:use     [org.clojurewerkz.neocons.rest.statuses]
            [org.clojurewerkz.neocons.rest.helpers]
            [clojure.string :only [join]])
  (:refer-clojure :exclude (get)))

;;
;; Implementation
;;

(defrecord Node
    [id location-uri data relationships-uri create-relationship-uri])

(defn- instantiate-node-from
  ([^long status headers payload ^long id]
     (Node. id (:self payload) (:data payload) (:all_relationships payload) (:create_relationship payload))))

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
    (Node. (extract-id location) location data (:relationships payload) (:create_relationship payload))))

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
    (if (or (missing? status)
            (conflict? status))
      [nil status]
      [id  status])))
