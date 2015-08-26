;; Copyright (c) 2011-2015 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.spatial
  (:require [cheshire.core             :as json]
            [clojurewerkz.neocons.rest :as rest]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers  :refer :all]
            [clojurewerkz.neocons.rest.records  :refer :all])
  (:import  [java.net URI URL]
            [clojurewerkz.neocons.rest Connection Neo4JEndpoint]))

;;
;; Implementation
;;

(defn- spatial-location-for
  [^Neo4JEndpoint endpoint action]
  (str (:uri endpoint) "ext/SpatialPlugin/graphdb/" action))

(defn- post-spatial
  [^Connection connection item-type body]
  (let [{:keys [status headers body]} (rest/POST connection
                                       (spatial-location-for (:endpoint connection) item-type)
                                       :body (json/encode body))
        payload  (json/decode body true)]
    (map instantiate-node-from payload)))

;;
;; API
;;

(defn add-simple-point-layer
  "Add a new point layer to the spatial index"
  ([^Connection connection layer lat lon]
     (first (post-spatial connection "addSimplePointLayer" {:layer layer :lat lat :lon lon})))
  ([^Connection connection layer]
     (first (post-spatial connection "addSimplePointLayer" {:layer layer}))))


(defn add-node-to-layer
  "Add a node with the appropriate latitude and longitude properties to the given layer"
  [^Connection connection layer node]
  (first (post-spatial connection "addNodeToLayer" {:layer layer
                                                    :node (node-location-for (:endpoint connection) (:id node))})))

(defn find-within-distance
  "Find all points in the layer within a given distance of the given point"
  [^Connection connection layer point-x point-y distance-in-km]
  (post-spatial connection "findGeometriesWithinDistance" {:layer layer
                                                           :pointX point-x
                                                           :pointY point-y
                                                           :distanceInKm distance-in-km}))
