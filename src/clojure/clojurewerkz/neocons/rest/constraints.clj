;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.constraints
  "Operations on constraints (Neo4J 2.0+ only)."
  (:require [clojurewerkz.neocons.rest              :as rest]
            [cheshire.core                          :as json]
            [clojurewerkz.neocons.rest.conversion   :as conv]
            [clojurewerkz.support.http.statuses     :refer [missing?]])
  (:import  clojurewerkz.neocons.rest.Connection)
  (:refer-clojure :exclude [rest]))

(defn- get-url
  [^Connection connection ^String label]
  (str (get-in connection [:endpoint :uri]) "schema/constraint/" (conv/encode-kw-to-string label)))

(defn- get-uniqueness-url
  [^Connection connection label]
  (str (get-url connection label) "/uniqueness"))

(defn create-unique
  "Creates a unique constraint on a given label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-create-uniqueness-constraint"
  [^Connection connection label property]
  (let [req-body                      (json/encode {"property_keys" [(conv/kw-to-string property)]})
        {:keys [status headers body]} (rest/POST connection (get-uniqueness-url connection label)
                                                 :body req-body)]
    (when-not (missing? status)
      (conv/map-values-to-kw
       (json/decode body true)
       [:label :property-keys]))))

(defn- get-uniquess-constraints
  [^Connection connection label ^String uri]
  (let [{:keys [status headers body]} (rest/GET connection (str (get-url connection label)
                                                                uri))]
    (when-not (missing? status)
      (map
       #(conv/map-values-to-kw % [:label :property-keys])
       (json/decode body true)))))

(defn get-unique
  "Gets information about a unique constraint on a given label and a property.
  If no property is passed, gets all the various uniqueness constraints for that label.

  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-uniqueness-constraints-for-a-label
  and http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-constraints-for-a-label"
  ([^Connection connection label]
     (get-uniquess-constraints connection label "/uniqueness"))
  ([^Connection connection label property]
     (get-uniquess-constraints connection label (str "/uniqueness/" (conv/encode-kw-to-string property)))))

(defn get-all
  "Gets information about all the different constraints associated with a label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-constraints-for-a-label

  If no label is passed, gets information about all the constraints.
  http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-get-all-constraints"
  ([^Connection connection ]
     (get-uniquess-constraints connection "" ""))
  ([^Connection connection label]
     (get-uniquess-constraints connection label "")))

(defn drop-unique
  "Drops an existing uniquenss constraint on an label and property.
  See http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html#rest-api-drop-constraint"
  [^Connection connection label property]
  (rest/DELETE connection (str (get-uniqueness-url connection label) "/" (conv/encode-kw-to-string property))))
