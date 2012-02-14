(ns clojurewerkz.neocons.rest.relationships
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Neo4JEndpoint]
            [clojurewerkz.neocons.rest.records Node Relationship])
  (:require [clojure.data.json                 :as json]
            [clojurewerkz.neocons.rest         :as rest])
  (:use     [clojurewerkz.neocons.rest.statuses]
            [clojurewerkz.neocons.rest.helpers]
            [clojurewerkz.neocons.rest.records]
            [clojure.string :only [join]])
  (:refer-clojure :exclude (get)))

;;
;; Implementation
;;

(defn- rel-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:relationships-uri endpoint) "/" id))

(defn- relationships-location-for
  [^Neo4JEndpoint endpoint ^Node node kind types]
  (let [query-params (if types
                       (str "/" (join "&" (map name types)))
                       "")]
    (str (:node-uri endpoint) "/" (:id node) "/relationships/" (name kind) query-params)))

(defn rel-properties-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:relationships-uri endpoint) "/" id "/properties"))

(defn rel-property-location-for
  [^Neo4JEndpoint endpoint ^long id prop]
  (str (rel-properties-location-for endpoint id) "/" (name prop)))

(defn- relationships-for
  [^Node node kind types]
  (let [{ :keys [status headers body] } (rest/GET (relationships-location-for rest/*endpoint* node kind types))
        xs  (json/read-json body true)]
    (if (missing? status)
      nil
      (map instantiate-rel-from xs))))

(defn rel-traverse-location-for
  [^Neo4JEndpoint endpoint ^long id]
  (str (:node-uri endpoint) "/" id "/traverse/relationship"))


;;
;; API
;;

(defn create
  ([^Node from ^Node to rel-type]
     (create from to rel-type {}))
  ([^Node from ^Node to rel-type data]
     (let [{ :keys [status headers body] } (rest/POST (:create-relationship-uri from)
                                                      :body (json/json-str { :to (:location-uri to) :type rel-type :data data }))
           payload  (json/read-json body true)]
       (instantiate-rel-from payload))))


(defn get
  [^long id]
  (let [{ :keys [status headers body] } (rest/GET (rel-location-for rest/*endpoint* id))
        payload  (json/read-json body true)]
    (if (missing? status)
      nil
      (instantiate-rel-from payload id))))


(defn delete
  [^long id]
  (let [{ :keys [status headers] } (rest/DELETE (rel-location-for rest/*endpoint* id))]
    (if (or (missing? status)
            (conflict? status))
      [nil status]
      [id  status])))


(defn update
  [^long id data]
  (rest/PUT (rel-properties-location-for rest/*endpoint* id) :body (json/json-str data))
  data)


(defn delete-property
  [^long id prop]
  (rest/DELETE (rel-property-location-for rest/*endpoint* id prop))
  nil)



(defn all-for
  "Returns all relationships for given node."
  [^Node node &{ :keys [types] }]
  (relationships-for node :all types))

(defn all-ids-for
  "Returns ids of all relationships for given node."
  [^Node node &{ :keys [types] }]
  (map :id (all-for node :types types)))

(defn incoming-for
  "Returns incoming (inbound) relationships for given node."
  [^Node node &{ :keys [types] }]
  (relationships-for node :in types))

(defn outgoing-for
  "Returns outgoing (outbound) relationships for given node."
  [^Node node &{ :keys [types] }]
  (relationships-for node :out types))

(defn outgoing-ids-for
  "Returns ids of outgoing (outbound) relationships for given node."
  [^Node node &{ :keys [types] }]
  (map :id (outgoing-for node :types types)))


(defn purge-all
  "Deletes all relationships for given node. Usually used before deleting the node,
   because Neo4J won't allow nodes with relationships to be deleted."
  ([^Node node]
     (doseq [id (all-ids-for node)]
       (delete id))))

(defn purge-outgoing
  "Deletes all outgoing relationships for given node."
  ([^Node node]
     (doseq [id (outgoing-ids-for node)]
       (delete id))))

(defn all-types
  []
  (let [{ :keys [_ _  body] } (rest/GET (:relationship-types-uri rest/*endpoint*))]
    (json/read-json body true)))


(defn traverse
  ([id & { :keys [order relationships uniqueness prune-evaluator return-filter max-depth] :or {
                                                                                               order         "breadth_first"
                                                                                               uniqueness    "node_global"
                                                                                               prune-evaluator { :language "builtin" :name "none" }
                                                                                               return-filter   { :language "builtin" :name "all"  }
                                                                                               } }]
     (let [request-body {
                         :order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth
                         }
           { :keys [status body] } (rest/POST (rel-traverse-location-for rest/*endpoint* id) :body (json/json-str request-body))
           xs (json/read-json body true)]
       (map (fn [doc]
              (instantiate-rel-from doc)) xs))))
