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
        (neorest/connect! "http://neocons:incorrec7-pazzwd@neo4j-proxy.local/db/data/")
        (catch Exception e
          (let [d (.getData e)]
            (is (= (-> d :object :status) 401))))))

    (deftest ^{:http-auth true} test-connection-and-discovery-using-user-info-in-string-uri
      (try
        (neorest/connect! "http://neocons:SEcRe7@neo4j-proxy.local/db/data/")
        (catch Exception e
          (let [d (.getData e)]
            (println d)
            (is (= (-> d :object :status) 401))))))

    (let [neo4j-login    (get (System/getenv) "NEO4J_LOGIN")
          neo4j-password (get (System/getenv) "NEO4J_PASSWORD")]
      (when (and neo4j-login neo4j-password)
        (deftest ^{:http-auth true} test-connection-and-discovery-with-http-credentials-provided-via-env-variables
          (neorest/connect! "http://neo4j-proxy.local/db/data/")
          (is (:version                neorest/*endpoint*))
          (is (:node-uri               neorest/*endpoint*))
          (is (:batch-uri              neorest/*endpoint*))
          (is (:relationship-types-uri neorest/*endpoint*)))))

    (deftest ^{:http-auth true} test-connection-and-discovery-with-provided-http-credentials
      (neorest/connect! "http://neo4j-proxy.local/db/data/" "neocons" "SEcRe7")
      (is (:version                neorest/*endpoint*))
      (is (:node-uri               neorest/*endpoint*))
      (is (:batch-uri              neorest/*endpoint*))
      (is (:relationship-types-uri neorest/*endpoint*)))

    (neorest/connect! "http://localhost:7474/db/data/" "neocons" "SEcRe7")

    (deftest ^{:http-auth true} test-creating-and-immediately-accessing-a-node-without-properties-with-http-auth
      (let [created-node (nodes/create)
            fetched-node (nodes/get (:id created-node))]
        (is (= (:id created-node) (:id fetched-node)))))

    (deftest ^{:http-auth true} test-creating-and-immediately-accessing-a-node-with-properties-with-http-auth
      (let [data         { :key "value" }
            created-node (nodes/create data)
            fetched-node (nodes/get (:id created-node))]
        (is (= (:id created-node) (:id fetched-node)))
        (is (= (:data created-node) data))))))
