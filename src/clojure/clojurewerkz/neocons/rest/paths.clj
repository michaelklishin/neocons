(ns clojurewerkz.neocons.rest.paths
  (:import  [java.net URI URL]
            clojurewerkz.neocons.rest.Neo4JEndpoint
            [clojurewerkz.neocons.rest.records Node Relationship Path])
  (:require [cheshire.custom                 :as json]
            [clojurewerkz.neocons.rest         :as rest])
  (:use     clojurewerkz.support.http.statuses
            clojurewerkz.neocons.rest.helpers
            clojurewerkz.neocons.rest.records))

;;
;; Implementation
;;

;; ...


;;
;; API
;;

(defn traverse
  ([id & { :keys [order relationships uniqueness prune-evaluator return-filter max-depth] :or {
                                                                                               order         "breadth_first"
                                                                                               uniqueness    "none"
                                                                                               prune-evaluator { :language "builtin" :name "none" }
                                                                                               return-filter   { :language "builtin" :name "all"  }
                                                                                               } }]
     (check-not-nil! id "id must not be nil")
     (let [request-body {
                         :order           order
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth
                         }
           { :keys [status body] } (rest/POST (path-traverse-location-for rest/*endpoint* id) :body (json/encode request-body))
           xs (json/decode body true)]
       (map (fn [doc]
              (instantiate-path-from doc)) xs))))


(defn all-shortest-between
  ([from to & { :keys [order relationships uniqueness prune-evaluator return-filter max-depth] :or {
                                                                                                    order         "breadth_first"
                                                                                                    uniqueness    "none"
                                                                                                    return-filter   { :language "builtin" :name "all"  }
                                                                                                    } }]
     (check-not-nil! from "from argument must not be nil")
     (check-not-nil! to   "to argument must not be nil")
     (let [request-body {
                         :to              (node-location-for rest/*endpoint* to)
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth
                         :algorithm       "shortestPath"
                         }
           { :keys [status body] } (rest/POST (paths-location-for rest/*endpoint* from) :body (json/encode request-body))
           xs (json/decode body true)]
       (map (fn [doc]
              (instantiate-path-from doc)) (json/decode body true)))))


(defn shortest-between
  ([from to & { :keys [order relationships uniqueness prune-evaluator return-filter max-depth] :or {
                                                                                                    order         "breadth_first"
                                                                                                    uniqueness    "none"
                                                                                                    return-filter   { :language "builtin" :name "all"  }
                                                                                                    } }]
     (check-not-nil! from "from argument must not be nil")
     (check-not-nil! to   "to argument must not be nil")
     (let [request-body {
                         :to              (node-location-for rest/*endpoint* to)
                         :relationships   relationships
                         :uniqueness      uniqueness
                         :prune_evaluator prune-evaluator
                         :return_filter   return-filter
                         :max_depth       max-depth
                         :algorithm       "shortestPath"
                         }
           { :keys [status body] } (rest/POST (path-location-for rest/*endpoint* from) :body (json/encode request-body) :throw-exceptions false)]
       (if (or (missing? status)
               (server-error? status))
         nil
         (instantiate-path-from (json/decode body true))))))

(defn exists-between?
  [from to &{ :keys [relationships max-depth prune-evaluator uniqueness] }]
  (check-not-nil! from "from argument must not be nil")
  (check-not-nil! to   "to argument must not be nil")
  (not (nil? (shortest-between from to :relationships relationships :max-depth max-depth :prune-evaluator prune-evaluator :uniqueness uniqueness))))


(defprotocol PathPredicates
  (included-in?     [node path] "Returns true if path includes given node")
  (included-in?     [rel  path] "Returns true if path includes given relationship")
  (node-in?         [node path] "Returns true if path includes given node")
  (relationship-in? [rel  path] "Returns true if path includes given relationship"))


(defprotocol PathPredicates
  (included-in?     [node path] "Returns true if path includes given node")
  (included-in?     [rel  path] "Returns true if path includes given relationship")
  (node-in?         [node path] "Returns true if path includes given node")
  (relationship-in? [rel  path] "Returns true if path includes given relationship"))


(extend-protocol PathPredicates
  Long
  (node-in? [^long node ^Path path]
    (let [uri (node-location-for rest/*endpoint* node)]
      (some (fn [u]
              (= uri u)) (:nodes path))))
  (relationship-in? [^long rel ^Path path]
    (let [uri (rel-location-for rest/*endpoint* rel)]
      (some (fn [u]
              (= uri u)) (:relationships path))))

  Node
  (included-in? [^Node node ^Path path]
    (node-in? (:id node) path))
  (node-in?     [^Node node ^Path path]
    (node-in? (:id node) path))

  Relationship
  (included-in? [^Relationship rel ^Path path]
    (relationship-in? (:id rel) path))
  (relationship-in? [^Relationship rel ^Path path]
    (relationship-in? (:id rel) path)))
