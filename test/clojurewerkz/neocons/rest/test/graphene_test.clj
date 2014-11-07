;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.graphene-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as rel]
            [clojurewerkz.neocons.rest.cypher        :as cy]
            [clojurewerkz.neocons.rest.transaction   :as tx]
            [clojure.test :refer :all]))

;; This group of tests the connectivity and usage of Neocons with GrapheneDB,
;; a hosted Neo4j solution. It is excluded from default
;; test selector and but it is run on the CI. Run it using
;;
;; GRAPHENE_URL=testneo.sb02.stations.graphenedb.com GRAPHENE_LOGIN=neocons GRAPHENE_PASSWORD=SEcRe7 lein2 test :graphene
;;
;; You can create a free GrapheneDB account for testing by signing
;; up for the service here: http://www.graphenedb.com/


(defn- env-var
  [^String s]
  (get (System/getenv) s))


(def GRAPHENE-HTTP-URL (str "http://"
                            (env-var "GRAPHENE_URL")
                            ":24789/db/data/"))
(def GRAPHENE-HTTPS-URL (str "https://"
                            (env-var "GRAPHENE_URL")
                            ":24780/db/data/"))


(deftest ^:graphene test-cypher-query-http
  (let [conn  (neorest/connect GRAPHENE-HTTP-URL
                               (env-var "GRAPHENE_LOGIN")
                               (env-var "GRAPHENE_PASS"))
        john  (nodes/create conn {:name "John"  :age 27 })
        sarah (nodes/create conn {:name "Sarah" :age 28 })
        rel1  (rel/create conn john sarah :friend)
        ids   (map :id [john sarah])
        response (cy/tquery conn "START x = node({ids}) RETURN x.name, x.age" { :ids ids })]
    (is (= [{"x.name" "John"  "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}] response))))

(deftest ^:graphene test-transaction-commit-http
  (let [conn                   (neorest/connect GRAPHENE-HTTP-URL
                                                (env-var "GRAPHENE_LOGIN")
                                                (env-var "GRAPHENE_PASS"))
        [transaction [result]] (tx/begin conn
                                         [(tx/statement "CREATE (n {props}) RETURN n"
                                                        {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (is (= (:data result) [{:row [{:name "My Node"}]}]))
    (is (= (:columns result) ["n"]))

    (let [[result] (tx/commit conn transaction [(tx/statement "CREATE n RETURN id(n)" nil)] )]
      (is (= (:columns result) ["id(n)"]))
      (is (= (count (:data result)) 1)))))


(deftest ^:graphene test-cypher-query-https
  (let [conn (neorest/connect GRAPHENE-HTTPS-URL
                              (env-var "GRAPHENE_LOGIN")
                              (env-var "GRAPHENE_PASS"))
        john  (nodes/create conn {:name "John"  :age 27 })
        sarah (nodes/create conn {:name "Sarah" :age 28 })
        rel1  (rel/create conn john sarah :friend)
        ids   (map :id [john sarah])
        response (cy/tquery conn "START x = node({ids}) RETURN x.name, x.age" { :ids ids })]
    (is (= [{"x.name" "John"  "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}] response))))


(deftest ^:graphene test-transaction-commit-https
  (let [conn                   (neorest/connect GRAPHENE-HTTPS-URL
                                                (env-var "GRAPHENE_LOGIN")
                                                (env-var "GRAPHENE_PASS"))
        [transaction [result]] (tx/begin conn
                                         [(tx/statement "CREATE (n {props}) RETURN n"
                                                        {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (is (= (:data result) [{:row [{:name "My Node"}]}]))
    (is (= (:columns result) ["n"]))

    (let [[result] (tx/commit conn transaction [(tx/statement "CREATE n RETURN id(n)" nil)] )]
      (is (= (:columns result) ["id(n)"]))
      (is (= (count (:data result)) 1)))))
