(ns clojurewerkz.neocons.rest.labels
  (:require [clojurewerkz.neocons.rest          :as rest]
            [clojurewerkz.neocons.rest.records  :as records]
            [cheshire.custom                    :as json]
            [clojurewerkz.support.http.statuses :as support])
  (:refer-clojure :exclude [replace remove]))

(defn add
  "Adds a string label or a list of string labels to a node.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-adding-a-label-to-a-node"
  [node labels]
  (rest/POST
    (str (:location-uri node) "/labels")
    :body (json/encode labels)))

(defn replace
  "This removes any existing labels for the node and adds the labels passes to the function.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-replacing-labels-on-a-node"
  [node labels]
  (rest/PUT
    (str (:location-uri node) "/labels")
    :body (json/encode labels)))

(defn remove
  "This removes the specified label from the node.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-removing-a-label-from-a-node"
  [node label]
  (rest/DELETE
    (str (:location-uri node) "/labels/" label)))

(defn- get-labels
  [uri]
  (let [{:keys [status headers body]} (rest/GET uri)]
    (when-not (support/missing? status)
      (json/decode body true))))

(defn get-all-labels
  "This function gets all labels in the database if no argument is passed.
  If a node is passed, then it returns all the labels associated with the node.

  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-listing-labels-for-a-node
  and http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-list-all-labels"
  ([] (get-labels (str (:uri rest/*endpoint*) "labels")))
  ([node] (get-labels (str (:location-uri node) "/labels"))))

(defn get-all-nodes
  "This returns all the nodes which have a particular label.
  See http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html#rest-api-get-all-nodes-with-a-label"
  [label]
  (let [base-uri (str (:uri rest/*endpoint*) "label/" label "/nodes")
        {:keys [status headers body]} (rest/GET base-uri)]
    (when-not (support/missing? status)
      (map records/instantiate-node-from (json/decode body true)))))
