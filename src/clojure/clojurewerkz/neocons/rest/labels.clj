(ns clojurewerkz.neocons.rest.labels
  (:require [clj-http.client                          :as http]
            [cheshire.custom                          :as json]
            [clojure.string                           :as string]
            [clojurewerkz.neocons.rest                :as rest]
            [clojurewerkz.neocons.rest.conversion     :as conv]
            [clojurewerkz.neocons.rest.records        :as records]
            [clojurewerkz.support.http.statuses       :as support])
  (:refer-clojure :exclude [replace remove]))

(defn add
  "Adds a string label or a list of labels (string or keyword) to a node.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-adding-a-label-to-a-node"
  [node labels]
  (rest/POST
    (str (:location-uri node) "/labels")
    :body (json/encode (conv/kw-to-string labels))))

(defn replace
  "This removes any existing labels for the node and adds the labels passes to the function.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-replacing-labels-on-a-node"
  [node labels]
  (conv/string-to-kw
    (rest/PUT
      (str (:location-uri node) "/labels")
      :body (json/encode labels))))

(defn remove
  "This removes the specified label from the node.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-removing-a-label-from-a-node"
  [node label]
  (rest/DELETE
    (str (:location-uri node) "/labels/" (conv/kw-to-string label))))

(defn- get-labels
  [^String uri]
  (let [{:keys [status headers body]} (rest/GET uri)]
    (when-not (support/missing? status)
      (conv/string-to-kw
        (json/decode body true)))))

(defn get-all-labels
  "This function gets all labels in the database if no argument is passed.
  If a node is passed, then it returns all the labels associated with the node.

  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-listing-labels-for-a-node
  and http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-list-all-labels"
  ([] (get-labels (str (:uri rest/*endpoint*) "labels")))
  ([node] (get-labels (str (:location-uri node) "/labels"))))

(defn- encode-params
  [^String label ^String x y]
  (str (:uri rest/*endpoint*)
       "label/"
       (conv/kw-to-string label)
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
  ([label] (get-all-nodes label nil nil))
  ([label prop-name prop-value]
   (let [base-uri (encode-params label prop-name prop-value)
         {:keys [status headers body]} (rest/GET base-uri)]
     (when-not (support/missing? status)
       (map records/instantiate-node-from (json/decode body true))))))
