;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.paths
  (:import  [java.net URI URL]
            clojurewerkz.neocons.rest.Connection
            [clojurewerkz.neocons.rest.records Node Relationship Path])
  (:require [cheshire.core                     :as json]
            [clojurewerkz.neocons.rest         :as rest]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers  :refer :all]
            [clojurewerkz.neocons.rest.records  :refer :all]))

;;
;; Implementation
;;

;; ...


;;
;; API
;;

(defn traverse
  ([^Connection connection id & {:keys [order relationships uniqueness prune-evaluator return-filter max-depth]
                                 :or {order         "breadth_first"
                                      uniqueness    "none"
                                      prune-evaluator {:language "builtin" :name "none"}
                                      return-filter   {:language "builtin" :name "all"}}}]
     (check-not-nil! id "id must not be nil")
     (let [request-body {:order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth}
           { :keys [status body] } (rest/POST connection (path-traverse-location-for (:endpoint connection) id)
                                              :body (json/encode request-body))
           xs (json/decode body true)]
       (map (fn [doc]
              (instantiate-path-from doc)) xs))))


(defn all-shortest-between
  ([^Connection connection from to & { :keys [relationships max-depth] }]
     (check-not-nil! from "from argument must not be nil")
     (check-not-nil! to   "to argument must not be nil")
     (let [request-body {:to            (node-location-for (:endpoint connection) to)
                         :relationships relationships
                         :max_depth     max-depth
                         :algorithm     "shortestPath"}
           { :keys [status body] } (rest/POST connection (paths-location-for (:endpoint connection) from)
                                              :body (json/encode request-body))
           xs (json/decode body true)]
       (map (fn [doc]
              (instantiate-path-from doc)) (json/decode body true)))))


(defn shortest-between
  ([^Connection connection from to & { :keys [relationships max-depth] }]
     (check-not-nil! from "from argument must not be nil")
     (check-not-nil! to   "to argument must not be nil")
     (let [request-body {:to            (node-location-for (:endpoint connection) to)
                         :relationships relationships
                         :max_depth     max-depth
                         :algorithm     "shortestPath"}
           { :keys [status body] } (rest/POST connection (path-location-for (:endpoint connection) from)
                                              :body (json/encode request-body)
                                              :throw-exceptions false)]
       (if (or (missing? status)
               (server-error? status))
         nil
         (instantiate-path-from (json/decode body true))))))

(defn exists-between?
  [^Connection connection from to &{ :keys [relationships max-depth prune-evaluator uniqueness] }]
  (check-not-nil! from "from argument must not be nil")
  (check-not-nil! to   "to argument must not be nil")
  (not (nil? (shortest-between connection from to :relationships relationships :max-depth max-depth :prune-evaluator prune-evaluator :uniqueness uniqueness))))


(declare node-in?)
(declare relationship-in?)


(defmulti included-in?
  "Returns true if path includes given obj which can be a node or a relationship"
  (fn [_ obj _]
    (class obj)))

(defmethod included-in? Node
  [^Connection connection ^Node node ^Path path]
  (node-in? connection (:id node) path))

(defmethod included-in? Relationship
  [^Connection connection ^Relationship rel ^Path path]
  (relationship-in? connection (:id rel) path))


(defmulti node-in?
  "Returns true if path includes given node"
  (fn [_ obj _]
    (class obj)))

(defmethod node-in? Node
  [^Connection connection ^Node node ^Path path]
  (node-in? connection (:id node) path))

(defmethod node-in? Long
  [^Connection connection ^long node ^Path path]
  (let [uri (node-location-for (:endpoint connection) node)]
      (some (fn [u]
              (= uri u)) (:nodes path))))


(defmulti relationship-in?
  "Returns true if path includes given relationship"
  (fn [_ obj _]
    (class obj)))


(defmethod relationship-in? Long
  [^Connection connection ^long rel ^Path path]
  (let [uri (rel-location-for (:endpoint connection) rel)]
      (some (fn [u]
              (= uri u)) (:relationships path))))


(defmethod relationship-in? Relationship
  [^Connection connection ^long rel ^Path path]
  (relationship-in? connection (:id rel) path))
