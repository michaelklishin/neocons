(ns clojurewerkz.neocons.rest.cypher
  (:refer-clojure :exclude [empty?])
  (:require [clojure.data.json                 :as json]
            [clojurewerkz.neocons.rest         :as rest])
  (:use     [clojurewerkz.support.http.statuses]
            [clojurewerkz.neocons.rest.helpers]
            [clojurewerkz.neocons.rest.records])
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Neo4JEndpoint]
            [clojurewerkz.neocons.rest.records CypherQueryResponse]))

;;
;; Implementation
;;

(defn cypher-query-location-for
  [^Neo4JEndpoint endpoint]
  (str (:uri endpoint) "cypher"))



;;
;; API
;;

(defn tableize
  "Turns Cypher query response into a table, like SQL queries in relational databases"
  ([response]
     (if-let [{:keys [columns data]} response]
       (tableize columns data)
       (list)))
  ([columns rows]
     (map (fn [row] (zipmap columns row)) rows)))

(defn query
  "Performs a Cypher query, returning columns and rows separately (the way Neo4J REST API does)"
  ([^String q]
     (query q {}))
  ([^String q params]
     (let [{ :keys [status headers body] } (rest/POST (cypher-query-location-for rest/*endpoint*) :body (json/json-str { :query q :params params }))]
       (if (missing? status)
         nil
         (instantiate-cypher-query-response-from (json/read-json body true))))))

(def ^{:doc "Performs a Cypher query, returning result formatted as a table (using tableize)"}
  tquery (comp tableize query))


(defn empty?
  "Returns true if provided Cypher response is empty (has no data columns), false otherwise.
   Empty responses can be returned by queries but also commonly mutating Cypher (with Neo4J Server 1.8+)"
  [^CypherQueryResponse response]
  (clojure.core/empty? (:data response)))