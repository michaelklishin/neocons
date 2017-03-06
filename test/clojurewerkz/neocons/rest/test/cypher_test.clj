;; Copyright (c) 2011-2015 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.cypher-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as rel]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojurewerkz.neocons.rest.cypher        :as cy]
            [clojure.test :refer :all]
            [clojure.set :refer [subset?]]
            [clojure.pprint :refer [pprint]]
            [clojurewerkz.neocons.rest.records :refer [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(defn- same-node?
  [a b]
  (and (= (:id a) (:id b))
       (= (:data a) (:data b))))

(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest ^{:cypher true} test-cypher-query-example1
    (let [john  (nodes/create conn {:name "John"})
          sarah (nodes/create conn {:name "Sarah"})
          joe   (nodes/create conn {:name "Joe"})
          maria (nodes/create conn {:name "Maria"})
          steve (nodes/create conn {:name "Steve"})
          rel1  (rel/create conn john sarah :friend)
          rel2  (rel/create conn john joe :friend)
          rel3  (rel/create conn sarah maria :friend)
          rel4  (rel/create conn joe steve :friend)
          {:keys [data columns]} (cy/query conn "START john=node({sid}) MATCH (john)-[:friend]->()-[:friend]->(fof) RETURN john, fof" {:sid (:id john)})
          row1  (map instantiate-node-from (first  data))
          row2  (map instantiate-node-from (second data))]
      (is (= 2 (count data)))
      (is (= ["john" "fof"] columns))
      (is (same-node? john (first row1)))
      (is (or (same-node? maria (last row1))
              (same-node? steve (last row1))))
      (is (same-node? john (first row2)))
      (is (or (same-node? maria (last row2))
              (same-node? steve (last row2))))
      (is (not (same-node? (last row2) (last row1))))))

  (deftest ^{:cypher true} test-cypher-query-example2
    (let [john  (nodes/create conn {:name "John"})
          sarah (nodes/create conn {:name "Sarah"})
          rel1  (rel/create conn john sarah :friend)
          [row1] (cy/tquery conn "START x = node({sid}) MATCH path = (x)--(friend) RETURN path, friend.name" {:sid (:id john)})
          path   (instantiate-path-from (get row1 "path"))]
      (is (paths/included-in? conn john path))
      (is (paths/included-in? conn sarah path))
      (is (= "Sarah" (get row1 "friend.name")))))

  (deftest ^{:cypher true} test-cypher-query-example3
    (let [john  (nodes/create conn { :name "John"  :age 27 })
          sarah (nodes/create conn { :name "Sarah" :age 28 })
          rel1  (rel/create conn john sarah :friend)
          ids   (map :id [john sarah])
          response (cy/tquery conn "START x = node({ids}) RETURN x.name, x.age" { :ids ids })]
      (is (= [{"x.name" "John"  "x.age" 27}
              {"x.name" "Sarah" "x.age" 28}] response))))

  (deftest ^{:cypher true} test-cypher-query-example4
    (let [john  (nodes/create conn { :name "John" })
          sarah (nodes/create conn { :name "Sarah" })
          ids   (map :id [john sarah])
          {:keys [data columns]} (cy/query conn "START x = node({ids}) RETURN x" {:ids ids})]
      (is (= ids (vec (map (comp :id instantiate-node-from first) data))))))

  (deftest ^{:cypher true} test-cypher-query-example5
    (let [john  (nodes/create conn { :name "John" })
          sarah (nodes/create conn { :name "Sarah" })
          ids   (vec (map :id [sarah john]))]
      (is (= ids (vec (map :id (nodes/get-many conn ids)))))))

  (deftest ^{:cypher true} test-cypher-query-example6
    (let [john  (nodes/create conn { :name "John" })
          sarah (nodes/create conn { :name "Sarah" })
          tim   (nodes/create conn { :name "Tim" })
          rel1 (rel/create conn john sarah :friend)
          rel2 (rel/create conn sarah tim :friend)
          ids   (vec (map :id [rel1 rel2]))]
      (is (= ids (vec (map :id (rel/get-many conn ids)))))))

  (deftest ^{:cypher true} test-cypher-tquery
    (let [john  (nodes/create conn { :name "John"  :age 27 })
          sarah (nodes/create conn { :name "Sarah" :age 28 })
          rel1  (rel/create conn john sarah :friend)
          ids   (map :id [john sarah])]
      (is (= [{"x.name" "John"  "x.age" 27}
              {"x.name" "Sarah" "x.age" 28}]
             (vec (cy/tquery conn "START x = node({ids}) RETURN x.name, x.age" { :ids ids }))))))

  (deftest ^{:cypher true} test-tableize
    (let [columns ["x.name" "x.age"]
          rows    [["John" 27] ["Sarah" 28]]]
      (is (= [{"x.name" "John"  "x.age" 27}
              {"x.name" "Sarah" "x.age" 28}] (vec (cy/tableize columns rows))))
      (is (empty? (cy/tableize nil))))))
