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
            [clj-http.util     :as util]
            [cheshire.custom   :as json]
            [clojure.string :refer [blank?]]
            [clojurewerkz.support.http.statuses :refer :all]
            [clojurewerkz.neocons.rest.helpers :refer [maybe-append]]))

;;
;; Implementation
;;
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

(defrecord Connection [^Neo4JEndpoint endpoint http-auth options])


(defn- env-var
  [^String s]
  (get (System/getenv) s))

(def ^{:private true}
  global-options {:throw-entire-message? true
                  :accept                :json})

(defn- get-options
  [^Connection connection options]
  (merge global-options (:http-auth connection) (:options connection) options))


(defn GET
  [^Connection connection ^String uri & {:as options}]
  (io!
   (http/get uri (get-options connection options))))

(defn POST
  [^Connection connection ^String uri &{:keys [body] :as options}]
  (io!
   (http/post uri (merge (get-options connection options)
                         {:content-type :json :body body}))))

(defn PUT
  [^Connection connection ^String uri &{:keys [body] :as options}]
  (io!
   (http/put uri (merge (get-options connection options)
                        {:content-type :json :body body}))))

(defn DELETE
  [^Connection connection ^String uri &{:keys [body] :as options}]
  (io!
   (http/delete uri (get-options connection options))))


(defn- process-connect
  [^String uri http-auth options]
  (let  [{:keys [status body]}   (GET (map->Connection
                                       {:options options :http-auth http-auth})
                                      uri)]
    (if (success? status)
         (let [payload    (json/decode body true)
               endpoint   (Neo4JEndpoint. (:neo4j_version      payload)
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
                                          (:transaction        payload))]
           (map->Connection
            {:endpoint  endpoint
             :options   options
             :http-auth http-auth})))))


;;
;; API
;;


(defn connect
  "Connects to given Neo4J REST API endpoint and performs service discovery"
  ([^String uri]
     (let [[login password] (http/parse-user-info (:user-info (http/parse-url uri)))
           login    (or login    (env-var "NEO4J_LOGIN"))
           password (or password (env-var "NEO4J_PASSWORD"))
           token    (env-var "NEO4J_AUTH_TOKEN")]
       (if (blank? token)
         (connect uri login password)
         (connect uri token))))
  ([^String uri token]
   (let   [encoded-token  (util/base64-encode (util/utf8-bytes (str ":" token)))
           auth           {"authorization" (str "Basic realm=\"Neo4j\" " encoded-token)}]
     (process-connect uri {} {:headers auth})))
  ([^String uri login password]
     (let [basic-auth              (if (and login password)
                                     {:basic-auth [login password]}
                                     {})]
       (process-connect uri basic-auth {}))))
