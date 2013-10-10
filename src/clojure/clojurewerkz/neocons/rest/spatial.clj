(ns clojurewerkz.neocons.rest.spatial
  (:require [cheshire.core             :as json]
            [clojurewerkz.neocons.rest :as rest]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers  :refer :all]
            [clojurewerkz.neocons.rest.records  :refer :all])
  (:import  [java.net URI URL]
            clojurewerkz.neocons.rest.Neo4JEndpoint))

;;
;; Implementation
;;

(defn- spatial-location-for
  [^Neo4JEndpoint endpoint action]
  (str (:uri endpoint) "ext/SpatialPlugin/graphdb/" action))

(defn- post-spatial
  [item-type body]
  (let [{:keys [status headers body]} (rest/POST
                                       (spatial-location-for rest/*endpoint* item-type)
                                       :body (json/encode body))
        payload  (json/decode body true)]
    (map instantiate-node-from payload)))

;;
;; API
;;

(defn add-simple-point-layer
  "Add a new point layer to the spatial index"
  ([layer lat lon]
     (first (post-spatial "addSimplePointLayer" {:layer layer :lat lat :lon lon})))
  ([layer]
     (first (post-spatial "addSimplePointLayer" {:layer layer}))))


(defn add-node-to-layer
  "Add a node with the appropriate latitude and longitude properties to the given layer"
  [layer node]
  (first (post-spatial "addNodeToLayer" {:layer layer :node (node-location-for rest/*endpoint* (:id node))})))

(defn find-within-distance
  "Find all points in the layer within a given distance of the given point"
  [layer point-x point-y distance-in-km]
  (post-spatial "findGeometriesWithinDistance" {:layer layer :pointX point-x :pointY point-y :distanceInKm distance-in-km}))
