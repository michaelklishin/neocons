(ns clojurewerkz.neocons.rest.index
  (:require [clojurewerkz.neocons.rest          :as rest]
            [cheshire.custom                    :as json]
            [clojurewerkz.support.http.statuses :as support])
  (:refer-clojure :exclude [drop]))

(defn- get-url
  [^String label]
  (str (:uri rest/*endpoint*) "schema/index/" label))

(defn create
  "Creates an index on a given label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-indexes.html#rest-api-create-index"
  [^String label ^String property]
  (let [req-body                      (json/encode {"property_keys" [property]})
        {:keys [status headers body]} (rest/POST (get-url label) :body req-body)]
    (when-not (support/missing? status)
      (json/decode body true))))

(defn get-all-indexes
  "Gets all indices for a given label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-indexes.html#rest-api-list-indexes-for-a-label"

  [^String label]
  (let [{:keys [status headers body]} (rest/GET (get-url label))]
    (when-not (support/missing? status)
      (json/decode body true))))

(defn drop
  "Drops an index on an existing label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-indexes.html#rest-api-list-indexes-for-a-label"
  [^String label ^String property]
  (rest/DELETE (str (get-url label) "/" property)))
