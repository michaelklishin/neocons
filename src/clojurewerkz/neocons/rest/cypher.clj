(ns clojurewerkz.neocons.rest.cypher
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Neo4JEndpoint])
  (:require [clojure.data.json                 :as json]
            [clojurewerkz.neocons.rest         :as rest])
  (:use     [clojurewerkz.neocons.rest.statuses]
            [clojurewerkz.neocons.rest.helpers]
            [clojurewerkz.neocons.rest.records]))

;;
;; Implementation
;;

(defn cypher-query-location-for
  [^Neo4JEndpoint endpoint]
  (str (:uri endpoint) "cypher"))



;;
;; API
;;

(defn query
  ([^String q]
     (query q {}))
  ([^String q params]
     (let [{ :keys [status headers body] } (rest/POST (cypher-query-location-for rest/*endpoint*) :body (json/json-str { :query q :params params }))]
       (if (missing? status)
         nil
         (instantiate-cypher-query-response-from (json/read-json body true))))))
