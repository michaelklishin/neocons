;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.labels
  "Operations on labels (Neo4J 2.0+)"
  (:require [clj-http.client                          :as http]
            [cheshire.core                            :as json]
            [clojure.string                           :as string]
            [clojurewerkz.neocons.rest                :as rest]
            [clojurewerkz.neocons.rest.conversion     :as conv]
            [clojurewerkz.neocons.rest.records        :as records]
            [clojurewerkz.support.http.statuses       :refer [missing?]])
  (:import [clojurewerkz.neocons.rest Connection])
  (:refer-clojure :exclude [node replace remove rest]))

(defn- get-location-url
  [node]
  (str (:location-uri node) "/labels"))

(defn add
  "Adds a string label or a list of labels (string or keyword) to a node.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-adding-a-label-to-a-node"
  [^Connection connection node labels]
  (rest/POST
   connection
   (get-location-url node)
   :body (json/encode (conv/kw-to-string labels))))

(defn replace
  "This removes any existing labels for the node and adds the labels passes to the function.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-replacing-labels-on-a-node"
  [^Connection connection node labels]
  (conv/string-to-kw
   (rest/PUT
    connection
    (get-location-url node)
    :body (json/encode labels))))

(defn remove
  "This removes the specified label from the node.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-removing-a-label-from-a-node"
  [^Connection connection node label]
  (rest/DELETE
   connection
   (str (get-location-url node) "/" (conv/encode-kw-to-string label))))

(defn- get-labels
  [^Connection connection ^String uri]
  (let [{:keys [status headers body]} (rest/GET connection uri)]
    (when-not (missing? status)
      (conv/string-to-kw
       (json/decode body true)))))

(defn get-all-labels
  "This function gets all labels in the database if no argument is passed.
  If a node is passed, then it returns all the labels associated with the node.

  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-listing-labels-for-a-node
  and http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-list-all-labels"
  ([^Connection connection]
     (get-labels connection (str (get-in connection [:endpoint :uri]) "labels")))
  ([^Connection connection node]
     (get-labels connection (get-location-url node))))

(defn- encode-params
  [^Connection connection ^String label ^String x y]
  (str (get-in connection [:endpoint :uri])
       "label/"
       (conv/encode-kw-to-string label)
       "/nodes"
       (when (and x y)
         (str "?"
              (http/generate-query-string
               [[(conv/kw-to-string x) (json/encode y)]])))))


(defn get-all-nodes
  "This returns all the nodes which have a particular label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-get-all-nodes-with-a-label

  You can also pass a property name and value you want to filter the nodes on.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-get-nodes-by-label-and-property"
  ([^Connection connection label]
     (get-all-nodes connection label nil nil))
  ([^Connection connection label prop-name prop-value]
     (let [base-uri (encode-params connection label prop-name prop-value)
           {:keys [status headers body]} (rest/GET connection base-uri)]
       (when-not (missing? status)
         (map records/instantiate-node-from (json/decode body true))))))
