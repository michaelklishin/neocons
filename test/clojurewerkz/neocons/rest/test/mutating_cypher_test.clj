(ns clojurewerkz.neocons.rest.test.mutating-cypher-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojurewerkz.neocons.rest.relationships :as rel]
            clojurewerkz.neocons.rest.records
            [clojurewerkz.neocons.rest.cypher        :as cy]
            [clojure.test :refer :all])
  (:import clojurewerkz.neocons.rest.records.Node))

(neorest/connect! "http://localhost:7474/db/data/")

(defn- exists?
  [^Node node]
  (try
    (nodes/get (:id node))
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
    (let [response (cy/tquery "CREATE (n) RETURN n")]
      (is (empty? (get-in (first response) ["n" :data])))))
  (testing "with node properties"
    (let [response (cy/tquery "CREATE (n {name: 'Neocons', type: 'client', language: 'Clojure'}) RETURN n")]
      (is (= {:name "Neocons" :language "Clojure" :type "client"} (get-in (first response) ["n" :data])))))
  (testing "with node properties passed as a map"
    (let [props    {:name "Neocons" :language "Clojure" :type "client"}
          response (cy/tquery "CREATE (n {props}) RETURN n" {:props props})]
      (is (= props (get-in (first response) ["n" :data]))))))

(deftest ^{:cypher true} test-creating-a-relationship-between-nodes-via-mutating-cypher
  (let [n1    (nodes/create)
        n2    (nodes/create)
        [{r "r"}] (cy/tquery "START n1 = node({id1}), n2 = node({id2}) CREATE n1-[r:knows]->n2 RETURN r" {:id1 (:id n1)
                                                                                                          :id2 (:id n2)})
        xs     (rel/all-outgoing-between n1 n2 ["knows"])]
    (is (= 1 (count xs)))
    (is (rel/starts-with? r (:id n1)))
    (is (rel/ends-with? r (:id n2)))))

(deftest ^{:cypher true} test-creating-a-relationship-between-nodes-if-it-does-not-exist
  (let [n1    (nodes/create)
        n2    (nodes/create)
        [{r "r"}] (cy/tquery "START n1 = node({id1}), n2 = node({id2}) CREATE UNIQUE n1-[r:knows]->n2 RETURN r" {:id1 (:id n1)
                                                                                                                 :id2 (:id n2)})
        _ (cy/tquery "START n1 = node({id1}), n2 = node({id2}) CREATE UNIQUE n1-[r:knows]->n2 RETURN r" {:id1 (:id n1)
                                                                                                         :id2 (:id n2)})
        xs     (rel/all-outgoing-between n1 n2 ["knows"])]
    (is (= 1 (count xs)))
    (is (rel/starts-with? r (:id n1)))
    (is (rel/ends-with? r (:id n2)))))

(deftest ^{:cypher true} test-purging-a-node-via-mutating-cypher-case1
  (let [john (nodes/create {:name "John" :age 28 :location "New York City, NY"})
        beth (nodes/create {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        gael (nodes/create {:name "Gaël"      :age 31 :location "Montpellier"})
        rel1 (rel/create john beth :knows)
        rel2 (rel/create john gael :knows)]
    (is (thrown? Exception
                 (nodes/delete (:id john))))
    (is (cy/empty? (cy/query "START n = node({sid}) MATCH n-[r]-() DELETE n, r" {:sid (:id john)})))
    (is (missing? john))
    (is (exists? beth))
    (is (exists? gael))))


;; this needs Neo4J 1.8 snapshot past 1.8-M01
(deftest ^{:cypher true} test-creating-a-bunch-of-nodes-via-mutating-cypher
  (let [urls     ["http://clojurewerkz.org/"
                  "http://clojurewerkz.org/articles/about.html"
                  "http://clojurewerkz.org/articles/community.html"]
        response (cy/tquery "CREATE (n {xs}) RETURN n", {:xs (map #(hash-map :url %) urls)})
        returned-urls (map #(-> (get % "n") :data :url) response)]
    (is (= urls returned-urls))))
