;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
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
            [clojurewerkz.neocons.rest.test.common   :refer :all]
            [clojure.test :refer :all])
  (:import clojurewerkz.neocons.rest.records.Node))

(use-fixtures :once once-fixture)

(defn- exists?
  [^Node node]
  (try
    (nodes/get *connection* (:id node))
    true
    (catch Exception e
      false)))

(def ^{:private true}
  missing? (complement exists?))


;;
;; Mutating Cypher queries
;;

(deftest ^{:cypher true} test-creating-a-node-via-mutating-cypher
  (testing "without node properties"
    (let [response (cy/tquery *connection* "CREATE (n) RETURN n")]
      (is (empty? (get-in (first response) ["n" :data])))))
  (testing "with node properties"
    (let [response (cy/tquery *connection* "CREATE (n {name: 'Neocons', type: 'client', language: 'Clojure'}) RETURN n")]
      (is (= {:name "Neocons" :language "Clojure" :type "client"} (get-in (first response) ["n" :data])))))
  (testing "with node properties passed as a map"
    (let [props    {:name "Neocons" :language "Clojure" :type "client"}
          response (cy/tquery *connection* "CREATE (n {props}) RETURN n" {:props props})]
      (is (= props (get-in (first response) ["n" :data]))))))

(deftest ^{:cypher true} test-creating-a-relationship-between-nodes-via-mutating-cypher
  (let [n1    (nodes/create *connection*)
        n2    (nodes/create *connection*)
        [{r "r"}] (cy/tquery *connection* "START n1 = node({id1}), n2 = node({id2}) CREATE n1-[r:knows]->n2 RETURN r" {:id1 (:id n1)
                                                                                                          :id2 (:id n2)})
        xs     (rel/all-outgoing-between *connection* n1 n2 ["knows"])]
    (is (= 1 (count xs)))
    (is (rel/starts-with? r (:id n1)))
    (is (rel/ends-with? r (:id n2)))))

(deftest ^{:cypher true} test-creating-a-relationship-between-nodes-if-it-does-not-exist
  (let [n1    (nodes/create *connection*)
        n2    (nodes/create *connection*)
        [{r "r"}] (cy/tquery *connection* "START n1 = node({id1}), n2 = node({id2}) CREATE UNIQUE n1-[r:knows]->n2 RETURN r" {:id1 (:id n1)
                                                                                                                 :id2 (:id n2)})
        _ (cy/tquery *connection* "START n1 = node({id1}), n2 = node({id2}) CREATE UNIQUE n1-[r:knows]->n2 RETURN r" {:id1 (:id n1)
                                                                                                         :id2 (:id n2)})
        xs     (rel/all-outgoing-between *connection* n1 n2 ["knows"])]
    (is (= 1 (count xs)))
    (is (rel/starts-with? r (:id n1)))
    (is (rel/ends-with? r (:id n2)))))

(deftest ^{:cypher true} test-purging-a-node-via-mutating-cypher-case1
  (let [john (nodes/create *connection* {:name "John" :age 28 :location "New York City, NY"})
        beth (nodes/create *connection* {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        gael (nodes/create *connection* {:name "Gaël"      :age 31 :location "Montpellier"})
        rel1 (rel/create *connection* john beth :knows)
        rel2 (rel/create *connection* john gael :knows)]
    (is (thrown? Exception
                 (nodes/delete *connection* (:id john))))
    (is (cy/empty? (cy/query *connection* "START n = node({sid}) MATCH n-[r]-() DELETE n, r" {:sid (:id john)})))
    (is (missing? john))
    (is (exists? beth))
    (is (exists? gael))))


;; this needs Neo4J 1.8 snapshot past 1.8-M01
(deftest ^{:cypher true} test-creating-a-bunch-of-nodes-via-mutating-cypher
  (let [urls     ["http://clojurewerkz.org/"
                  "http://clojurewerkz.org/articles/about.html"
                  "http://clojurewerkz.org/articles/community.html"]
        response (cy/tquery *connection* "CREATE (n {xs}) RETURN n", {:xs (map #(hash-map :url %) urls)})
        returned-urls (map #(-> (get % "n") :data :url) response)]
    (is (= urls returned-urls))))
