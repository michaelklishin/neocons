(ns clojurewerkz.neocons.rest.index
  "Operations on indices (Neo4J 2.0+ only)."
  (:require [clojurewerkz.neocons.rest              :as rest]
            [cheshire.core                          :as json]
            [clojurewerkz.neocons.rest.conversion   :as conv]
            [clojurewerkz.support.http.statuses     :refer [missing?]])
  (:refer-clojure :exclude [drop]))

(defn- get-url
  [label]
  (str (:uri rest/*endpoint*) "schema/index/" (conv/kw-to-string label)))

(defn create
  "Creates an index on a given label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-indexes.html#rest-api-create-index"
  [label property]
  (let [req-body                      (json/encode {"property_keys" [(conv/kw-to-string property)]})
        {:keys [status headers body]} (rest/POST (get-url label) :body req-body)]
    (when-not (missing? status)
      (conv/map-values-to-kw
        (json/decode body true)
        [:label]))))

(defn get-all
  "Gets all indices for a given label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-indexes.html#rest-api-list-indexes-for-a-label"

  [label]
  (let [{:keys [status headers body]} (rest/GET (get-url label))]
    (when-not (missing? status)
      (json/decode body true))))

(defn drop
  "Drops an index on an existing label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-indexes.html#rest-api-list-indexes-for-a-label"
  [label property]
  (rest/DELETE (str (get-url label) "/" (conv/kw-to-string property))))
