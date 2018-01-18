;; Copyright (c) 2011-2018 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.mutating-cypher-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojurewerkz.neocons.rest.relationships :as rel]
            clojurewerkz.neocons.rest.records
            [clojurewerkz.neocons.rest.cypher        :as cy]
            [clojure.test :refer :all])
  (:import clojurewerkz.neocons.rest.records.Node))

(defn- exists?
  [conn ^Node node]
  (try
    (nodes/get conn (:id node))
    true
    (catch Exception e
      false)))

(def ^{:private true}
  missing? (complement exists?))

(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest ^{:cypher true} test-creating-a-node-via-mutating-cypher
    (testing "without node properties"
      (let [response (cy/tquery conn "CREATE (n) RETURN n")]
        (is (empty? (get-in (first response) ["n" :data])))))
    (testing "with node properties"
      (let [response (cy/tquery conn "CREATE (n {name: 'Neocons', type: 'client', language: 'Clojure'}) RETURN n")]
        (is (= {:name "Neocons" :language "Clojure" :type "client"} (get-in (first response) ["n" :data])))))
    (testing "with node properties passed as a map"
      (let [props    {:name "Neocons" :language "Clojure" :type "client"}
            response (cy/tquery conn "CREATE (n {props}) RETURN n" {:props props})]
        (is (= props (get-in (first response) ["n" :data]))))))

  (deftest ^{:cypher true} test-creating-a-relationship-between-nodes-via-mutating-cypher
    (let [n1    (nodes/create conn)
          n2    (nodes/create conn)
          [{r "r"}] (cy/tquery conn "START n1 = node({id1}), n2 = node({id2}) CREATE (n1)-[r:knows]->(n2) RETURN r" {:id1 (:id n1)
                                                                                                                         :id2 (:id n2)})
          xs     (rel/all-outgoing-between conn n1 n2 ["knows"])]
      (is (= 1 (count xs)))
      (is (rel/starts-with? r (:id n1)))
      (is (rel/ends-with? r (:id n2)))))

  (deftest ^{:cypher true} test-creating-a-relationship-between-nodes-if-it-does-not-exist
    (let [n1    (nodes/create conn)
          n2    (nodes/create conn)
          [{r "r"}] (cy/tquery conn "START n1 = node({id1}), n2 = node({id2}) CREATE UNIQUE (n1)-[r:knows]->(n2) RETURN r" {:id1 (:id n1)
                                                                                                                                :id2 (:id n2)})
          _ (cy/tquery conn "START n1 = node({id1}), n2 = node({id2}) CREATE UNIQUE (n1)-[r:knows]->(n2) RETURN r" {:id1 (:id n1)
                                                                                                                        :id2 (:id n2)})
          xs     (rel/all-outgoing-between conn n1 n2 ["knows"])]
      (is (= 1 (count xs)))
      (is (rel/starts-with? r (:id n1)))
      (is (rel/ends-with? r (:id n2)))))

  (deftest ^{:cypher true} test-purging-a-node-via-mutating-cypher-case1
    (let [john (nodes/create conn {:name "John" :age 28 :location "New York City, NY"})
          beth (nodes/create conn {:name "Elizabeth" :age 30 :location "Chicago, IL"})
          gael (nodes/create conn {:name "Gaël"      :age 31 :location "Montpellier"})
          rel1 (rel/create conn john beth :knows)
          rel2 (rel/create conn john gael :knows)]
      (is (thrown? Exception
                   (nodes/delete conn (:id john))))
      (is (cy/empty? (cy/query conn "START n = node({sid}) MATCH (n)-[r]-() DELETE n, r" {:sid (:id john)})))
      (is (missing? conn john))
      (is (exists? conn beth))
      (is (exists? conn gael))))

  (deftest ^{:cypher true} test-creating-a-bunch-of-nodes-via-mutating-cypher
    (let [urls     ["http://clojurewerkz.org/"
                    "http://clojurewerkz.org/articles/about.html"
                    "http://clojurewerkz.org/articles/community.html"]
          ;;CREATE (n obj)
          response (cy/tquery conn "WITH {xs} AS objs UNWIND objs AS obj CREATE (n {url: obj}) RETURN n", {:xs urls})
          returned-urls (map #(-> (get % "n") :data :url) response)]
      (is (= urls returned-urls)))))
