(ns clojurewerkz.neocons.rest.constraints
  (:require [clojurewerkz.neocons.rest              :as rest]
            [cheshire.custom                        :as json]
            [clojurewerkz.neocons.rest.conversion   :as conv]
            [clojurewerkz.support.http.statuses     :as support])
  (:refer-clojure :exclude [drop]))

(defn- get-url
  [^String label]
  (str (:uri rest/*endpoint*) "schema/constraint/" (conv/kw-to-string label)))

(defn- get-uniqueness-url
  [label]
  (str (get-url label) "/uniqueness"))

(defn create-unique
  "Creates a unique contrainst on a given label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-create-uniqueness-constraint"
  [label property]
  (let [req-body                      (json/encode {"property_keys" [(conv/kw-to-string property)]})
        {:keys [status headers body]} (rest/POST (get-uniqueness-url label) :body req-body)]
    (when-not (support/missing? status)
      (json/decode body true))))

(defn- get-uniquess-constraints
  [label ^String uri]
  (let [{:keys [status headers body]} (rest/GET (str (get-url label) uri))]
    (when-not (support/missing? status)
      (json/decode body true))))

(defn get-unique
  "Gets information about a unique contrainst on a given label and a property.
  If no property is passed, gets all the various uniqueness constraints for that label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-uniqueness-constraints-for-a-label
  and http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-constraints-for-a-label"
([label]
 (get-uniquess-constraints label "/uniqueness"))
([label property]
 (get-uniquess-constraints label (str "/uniqueness/" (conv/kw-to-string property)))))

(defn get-all
  "Gets information about all the different constraints associated with a label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-constraints-for-a-label

  If no label is passed, gets information about all the constraints.
  http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-constraints"
  ([] (get-uniquess-constraints "" ""))
  ([label] (get-uniquess-constraints label "")))

(defn drop
  "Drops an existing uniquenss constraint on an label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-drop-constraint"

  [label property]
  (rest/DELETE (str (get-uniqueness-url label) "/" (conv/kw-to-string property))))

