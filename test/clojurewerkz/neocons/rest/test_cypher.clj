(ns clojurewerkz.neocons.rest.test-cypher
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as rel]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojurewerkz.neocons.rest.cypher        :as cy])
  (:use clojure.test
        [clojure.set :only [subset?]]
        [clojure.pprint :only [pprint]]
        [clojurewerkz.neocons.rest.records :only [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(neorest/connect! "http://localhost:7474/db/data/")


;;
;; Cypher queries
;;

(defn- same-node?
  [a b]
  (and (= (:id a) (:id b))
       (= (:data a) (:data b))))

(deftest ^{:cypher true} test-cypher-query-example1
  (let [john  (nodes/create {:name "John"})
        sarah (nodes/create {:name "Sarah"})
        joe   (nodes/create {:name "Joe"})
        maria (nodes/create {:name "Maria"})
        steve (nodes/create {:name "Steve"})
        rel1  (rel/create john sarah :friend)
        rel2  (rel/create john joe :friend)
        rel3  (rel/create sarah maria :friend)
        rel4  (rel/create joe steve :friend)
        {:keys [data columns]} (cy/query "START john=node({sid}) MATCH john-[:friend]->()-[:friend]->fof RETURN john, fof" {:sid (:id john)})
        row1  (map instantiate-node-from (first  data))
        row2  (map instantiate-node-from (second data))]
    (is (= 2 (count data)))
    (is (= ["john" "fof"] columns))
    (is (same-node? john (first row1)))
    (is (same-node? maria (last row1)))
    (is (same-node? john (first row2)))
    (is (same-node? steve (last row2)))))


(deftest ^{:cypher true} test-cypher-query-example2
  (let [john  (nodes/create {:name "John"})
        sarah (nodes/create {:name "Sarah"})
        rel1  (rel/create john sarah :friend)
        [row1] (cy/tquery "START x = node({sid}) MATCH path = (x--friend) RETURN path, friend.name" {:sid (:id john)})
        path   (instantiate-path-from (get row1 "path"))]
    (is (paths/included-in? john path))
    (is (paths/included-in? sarah path))
    (is (= "Sarah" (get row1 "friend.name")))))


(deftest ^{:cypher true} test-cypher-query-example3
  (let [john  (nodes/create { :name "John"  :age 27 })
        sarah (nodes/create { :name "Sarah" :age 28 })
        rel1  (rel/create john sarah :friend)
        ids   (map :id [john sarah])
        response (cy/tquery "START x = node({ids}) RETURN x.name, x.age" { :ids ids })]
    (is (= [{"x.name" "John"  "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}] response))))

(deftest ^{:cypher true} test-cypher-query-example4
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        ids   (map :id [john sarah])
        {:keys [data columns]} (cy/query "START x = node({ids}) RETURN x" {:ids ids})]
    (is (= ids (vec (map (comp :id instantiate-node-from first) data))))))

(deftest ^{:cypher true} test-cypher-query-example5
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        ids   (vec (map :id [sarah john]))]
    (is (= ids (vec (map :id (nodes/get-many ids)))))))

(deftest ^{:cypher true} test-cypher-tquery
  (let [john  (nodes/create { :name "John"  :age 27 })
        sarah (nodes/create { :name "Sarah" :age 28 })
        rel1  (rel/create john sarah :friend)
        ids   (map :id [john sarah])]
    (is (= [{"x.name" "John"  "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}]
           (vec (cy/tquery "START x = node({ids}) RETURN x.name, x.age" { :ids ids }))))))


(deftest ^{:cypher true} test-tableize
  (let [columns ["x.name" "x.age"]
        rows    [["John" 27] ["Sarah" 28]]]
    (is (= [{"x.name" "John"  "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}] (vec (cy/tableize columns rows))))
    (is (empty? (cy/tableize nil)))))
