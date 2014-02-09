;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest
  (:import  java.net.URI)
  (:require [clj-http.client   :as http]
            [cheshire.custom   :as json]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers :refer [maybe-append]]))

;;
;; Implementation
;;

(defn- env-var
  [^String s]
  (get (System/getenv) s))

(def ^{:private true}
  global-options {:throw-entire-message? true})

(def ^{:private true}
  http-authentication-options {})

(defn GET
  [^String uri & {:as options}]
  (io!
   (http/get uri (merge global-options http-authentication-options options {:accept :json}))))

(defn POST
  [^String uri &{:keys [body] :as options}]
  (io!
   (http/post uri (merge global-options http-authentication-options options {:accept :json :content-type :json :body body}))))

(defn PUT
  [^String uri &{:keys [body] :as options}]
  (io!
   (http/put uri (merge global-options http-authentication-options options {:accept :json :content-type :json :body body}))))

(defn DELETE
  [^String uri &{:keys [body] :as options}]
  (io!
   (http/delete uri (merge global-options http-authentication-options options {:accept :json}))))




(defrecord Neo4JEndpoint
    [version
     node-uri
     relationships-uri
     node-index-uri
     relationship-index-uri
     relationship-types-uri
     batch-uri
     extensions-info-uri
     extensions
     reference-node-uri
     uri
     cypher-uri
     transaction-uri])

(def ^{:dynamic true} *endpoint*)

;;
;; API
;;



(defn connect
  "Connects to given Neo4J REST API endpoint and performs service discovery"
  ([^String uri]
     (let [login    (env-var "NEO4J_LOGIN")
           password (env-var "NEO4J_PASSWORD")]
       (connect uri login password)))
  ([^String uri login password]
     (let [{ :keys [status body] } (if (and login password)
                                     (GET uri :basic-auth [login password])
                                     (GET uri))]
       (if (success? status)
         (let [payload (json/decode body true)]
           (alter-var-root (var http-authentication-options) (constantly { :basic-auth [login password] }))
           (Neo4JEndpoint. (:neo4j_version      payload)
                           (:node               payload)
                           (str uri (if (.endsWith uri "/")
                                      "relationship"
                                      "/relationship"))
                           (:node_index         payload)
                           (:relationship_index payload)
                           (:relationship_types payload)
                           (:batch              payload)
                           (:extensions_info    payload)
                           (:extensions         payload)
                           (:reference_node     payload)
                           (maybe-append uri "/")
                           (:cypher             payload)
                           (:transaction        payload)
                           ))))))

(defn connect!
  "Like connect but also mutates *endpoint* state to store the connection"
  ([uri]
     (alter-var-root (var *endpoint*) (fn [_] (connect uri))))
  ([uri login password]
     (alter-var-root (var *endpoint*) (fn [_] (connect uri login password)))))
