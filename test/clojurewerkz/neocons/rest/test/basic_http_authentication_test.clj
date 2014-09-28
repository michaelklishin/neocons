;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.basic-http-authentication-test
  (:require [clojurewerkz.neocons.rest :as neorest]
            [clojurewerkz.neocons.rest.nodes :as nodes]
            [slingshot.slingshot :as slingshot]
            [clojure.test :refer :all])
  (:import [slingshot ExceptionInfo]))

;; This group of tests assumes you have Nginx or Apache proxy set up at neo4j-proxy.local
;; that proxies to whatever Neo4J Server installation you want to use. It is excluded from default
;; test selector and thus CI. Run it using
;;
;; TEST_HTTP_AUTHENTICATION=true NEO4J_LOGIN=neocons NEO4J_PASSWORD=SEcRe7 lein2 test :http-auth
;;
;;
;; Example .htpasswd file for Apache or Nginx:
;;
;; neocons:$apr1$u9kPE9lO$FABK3Wu7XHSFwuQiepi3M.
;;
;; (neocons:SEcRe7)
;; you can check your HTTP authentication setup using curl like so:
;;
;; curl --user neocons:SEcRe7 http://neo4j-proxy.local/db/data/
;; curl --user neocons:SEcRe7 http://neo4j-proxy.local/db/data/nodes/1

(when (get (System/getenv) "TEST_HTTP_AUTHENTICATION")
  (do
    (deftest ^{:http-auth true} test-connection-and-discovery-using-user-info-in-string-uri
      (try
        (neorest/connect "http://neocons:incorrec7-pazzwd@neo4j-proxy.local/db/data/")
        (is false)
        (catch Exception e
          (let [d (.getData e)]
            (is (some #{(-> d :object :status)} [401 403]))))))
    
    (with-redefs [clojurewerkz.neocons.rest/env-var (constantly nil)]
      (deftest ^{:http-auth true} test-connection-and-discovery-using-user-info-in-string-uri-2
        (let [conn (neorest/connect "http://neocons:SEcRe7@neo4j-proxy.local/db/data/")]
          (is (= (-> conn :http-auth :basic-auth) ["neocons" "SEcRe7"])))))
    
    (let [neo4j-login    (get (System/getenv) "NEO4J_LOGIN")
          neo4j-password (get (System/getenv) "NEO4J_PASSWORD")]
      (when (and neo4j-login neo4j-password)
        (deftest ^{:http-auth true} test-connection-and-discovery-with-http-credentials-provided-via-env-variables
          (let   [conn (neorest/connect "http://neo4j-proxy.local/db/data/")]
            (is (= (-> conn :http-auth :basic-auth) ["neocons" "SEcRe7"]))
            (is (:version                (:endpoint conn)))
            (is (:node-uri               (:endpoint conn)))
            (is (:batch-uri              (:endpoint conn)))
            (is (:relationship-types-uri (:endpoint conn)))))))
    
    (deftest ^{:http-auth true} test-connection-and-discovery-with-provided-http-credentials
      (let   [conn (neorest/connect "http://neo4j-proxy.local/db/data/" "neocons" "SEcRe7")]
        (is (= (-> conn :http-auth :basic-auth) ["neocons" "SEcRe7"]))
        (is (:version                (:endpoint conn)))
        (is (:node-uri               (:endpoint conn)))
        (is (:batch-uri              (:endpoint conn)))
        (is (:relationship-types-uri (:endpoint conn)))))
    
    (let  [conn  (neorest/connect "http://neocons:SEcRe7@neo4j-proxy.local/db/data/")]
      (deftest ^{:http-auth true} test-creating-and-immediately-accessing-a-node-without-properties-with-http-auth
        (let [created-node (nodes/create conn)
              fetched-node (nodes/get conn (:id created-node))]
          (is (= (:id created-node) (:id fetched-node)))))
      
      (deftest ^{:http-auth true} test-creating-and-immediately-accessing-a-node-with-properties-with-http-auth
        (let [data         { :key "value" }
              created-node (nodes/create conn data)
              fetched-node (nodes/get conn (:id created-node))]
          (is (= (:id created-node) (:id fetched-node)))
          (is (= (:data created-node) data)))))))