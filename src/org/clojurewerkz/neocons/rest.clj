(ns org.clojurewerkz.neocons.rest
  (:import  [java.net URI])
  (:require [clj-http.client   :as http]
            [clojure.data.json :as json])
  (:use     [org.clojurewerkz.neocons.rest.statuses]))

;;
;; Implementation
;;

(defn GET
  [^String uri]
  (http/get uri))





(defrecord Neo4JEndpoint
    [version node-uri node-index-uri relationship-index-uri relationship-types-uri batch-uri extensions-info-uri extensions reference-node-uri])

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
          (Neo4JEndpoint. (:neo4j_version payload)
                          (:node          payload)
                          (:node_index         payload)
                          (:relationship_index payload)
                          (:relationship_types payload)
                          (:batch              payload)
                          (:extensions_info    payload)
                          (:extensions         payload)
                          (:reference_node     payload))))))
  (connect! [uri]
    (defonce ^{ :dynamic true } *endpoint* (connect uri))))
