(ns clojurewerkz.neocons.rest
  (:import  [java.net URI])
  (:require [clj-http.client   :as http]
            [clojure.data.json :as json])
  (:use     [clojurewerkz.neocons.rest.statuses]))

;;
;; Implementation
;;

(defn GET
  [^String uri &{ :keys [body] :as options }]
  (http/get uri (merge options { :accept :json })))

(defn POST
  [^String uri &{ :keys [body] :as options }]
  (http/post uri (merge options { :accept :json, :body body })))

(defn PUT
  [^String uri &{ :keys [body] :as options }]
  (http/put uri (merge options { :accept :json, :body body })))

(defn DELETE
  [^String uri &{ :keys [body] :as options }]
  (http/delete uri (merge options { :accept :json })))




(defrecord Neo4JEndpoint
    [version node-uri relationships-uri node-index-uri relationship-index-uri relationship-types-uri batch-uri extensions-info-uri extensions reference-node-uri])

(def ^{ :dynamic true } *endpoint*)

;;
;; API
;;

(defprotocol Connection
  (connect  [uri] "Connects to given Neo4J REST API endpoint and performs service discovery")
  (connect! [uri] "Connects to given Neo4J REST API endpoint, performs service discovery and mutates *endpoint* state to store it"))

(extend-protocol Connection
  URI
  (connect [uri]
    (connect (.toString uri)))
  (connect! [uri]
    (connect! (.toString uri)))

  String
  (connect [uri]
    (let [{ :keys [status headers body] } (GET uri)]
      (if (success? status)
        (let [payload (json/read-json body true)]
          (Neo4JEndpoint. (:neo4j_version      payload)
                          (:node               payload)
                          (str uri (if (.endsWith uri "/")
                                     "relationship"
                                     "/relationship"))
                          (:node_index         payload)
                          (:relationship_index payload)
                          (:relationship_types payload)
                          (:batch              payload)
                          (:extensions_info    payload)
                          (:extensions         payload)
                          (:reference_node     payload))))))
  (connect! [uri]
    (defonce ^{ :dynamic true } *endpoint* (connect uri))))
