;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.cypher
  (:refer-clojure :exclude [empty?])
  (:require [cheshire.core             :as json]
            [clojurewerkz.neocons.rest :as rest]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers  :refer :all]
            [clojurewerkz.neocons.rest.records  :refer :all])
  (:import  [java.net URI URL]
            clojurewerkz.neocons.rest.Neo4JEndpoint
            clojurewerkz.neocons.rest.records.CypherQueryResponse))

;;
;; Implementation
;;

(defn cypher-query-location-for
  [^Neo4JEndpoint endpoint]
  (:cypher-uri endpoint))



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
     (let [{:keys [status headers body]} (rest/POST (cypher-query-location-for rest/*endpoint*) :body (json/encode {:query q :params params}))]
       (if (missing? status)
         nil
         (instantiate-cypher-query-response-from (json/decode body true))))))

(def ^{:doc "Performs a Cypher query, returning result formatted as a table (using tableize)"}
  tquery (comp tableize query))


(defn empty?
  "Returns true if provided Cypher response is empty (has no data columns), false otherwise.
   Empty responses can be returned by queries but also commonly mutating Cypher (with Neo4J Server 1.8+)"
  [^CypherQueryResponse response]
  (clojure.core/empty? (:data response)))
