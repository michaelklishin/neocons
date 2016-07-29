;; Copyright (c) 2011-2015 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.bolt
  (:require [clojure.string :as string])
  (:import (java.util Map)
           (org.neo4j.driver.v1 AuthTokens Config Driver
                                GraphDatabase Record Session
                                StatementResult StatementRunner
                                Transaction Values)))

(defn- env-var
  [^String s]
  (get (System/getenv) s))

(defn connect
  (^Driver [^String url]
   (let [login    (env-var "NEO4J_LOGIN")
         password (env-var "NEO4J_PASSWORD")]
     (if (or (string/blank? login)
             (string/blank? password))
       (GraphDatabase/driver url)
       (connect url login password))))
  (^Driver [^String url ^String username ^String password]
   (GraphDatabase/driver url (AuthTokens/basic username password)))
  (^Driver [^String url ^String username ^String password ^Config config]
   (GraphDatabase/driver url (AuthTokens/basic username password) config)))

(defn create-session
  ^Session [^Driver driver]
  (.session driver))

(defn query
  ([^StatementRunner runner ^String qry]
   (query runner qry {}))
  ([^StatementRunner runner ^String qry ^Map params]
   (map (fn [^Record r]
          (into {} (.asMap r)))
        (iterator-seq (.run runner qry params)))))

(defn begin-tx
  ^Transaction [^Session session]
  (.beginTransaction session))

(defn tx-successful
  [^Transaction transaction]
  (.success transaction))

(defn tx-failure
  [^Transaction transaction]
  (.failure transaction))
