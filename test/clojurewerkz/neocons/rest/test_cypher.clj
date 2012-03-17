(ns clojurewerkz.neocons.rest.test-cypher
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.cypher        :as cypher]
            [slingshot.slingshot :as slingshot])
  (:import [slingshot ExceptionInfo])
  (:use clojure.test
        [clojure.set :only [subset?]]
        [clojure.pprint :only [pprint]]
        [clojurewerkz.neocons.rest.records :only [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(neorest/connect! "http://localhost:7474/db/data/")


;;
;; Cypher queries
;;

(deftest ^{ :cypher true } test-cypher-query-example1
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        joe   (nodes/create { :name "Joe" })
        maria (nodes/create { :name "Maria" })
        steve (nodes/create { :name "Steve" })
        rel1  (relationships/create john sarah :friend)
        rel2  (relationships/create john joe :friend)
        rel3  (relationships/create sarah maria :friend)
        rel4  (relationships/create joe steve :friend)
        { :keys [data columns] } (cypher/query "START john=node({sid}) MATCH john-[:friend]->()-[:friend]->fof RETURN john, fof" { :sid (:id john) })
        row1  (map instantiate-node-from (first  data))
        row2  (map instantiate-node-from (second data))]
    (is (= 2 (count data)))
    (is (= ["john" "fof"] columns))
    (is (= (:id john)    (:id (first row1))))
    (is (= (:data john)  (:data (first row1))))
    (is (= (:id maria)   (:id (last row1))))
    (is (= (:data maria) (:data (last row1))))
    (is (= (:id john)    (:id (first row2))))
    (is (= (:data john)  (:data (first row2))))
    (is (= (:id steve)   (:id (last row2))))
    (is (= (:data steve) (:data (last row2))))))


(deftest ^{ :cypher true } test-cypher-query-example2
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        rel1  (relationships/create john sarah :friend)
        { :keys [data columns] } (cypher/query "START x = node({sid}) MATCH path = (x--friend) RETURN path, friend.name" { :sid (:id john) })
        row1  (map instantiate-path-from (first data))
        path1 (first row1)]
    (is (= 1 (count data)))
    (is (= ["path" "friend.name"] columns))
    (is (= 1 (:length path1)))
    (is (= (:start path1) (:location-uri john)))
    (is (= (:end   path1) (:location-uri sarah)))
    (is (= (first (:relationships path1)) (:location-uri rel1)))))


(deftest ^{ :cypher true } test-cypher-query-example3
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        rel1  (relationships/create john sarah :friend)
        ids   (map :id [john sarah])
        { :keys [data columns] } (cypher/query "START x = node({ids}) RETURN x.name" { :ids ids })]
    (is (= ["John" "Sarah"] (vec (map first data))))))

(deftest ^{ :cypher true } test-cypher-query-example4
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        ids   (map :id [john sarah])
        { :keys [data columns] } (cypher/query "START x = node({ids}) RETURN x" { :ids ids })]
    (is (= ids (vec (map (comp :id instantiate-node-from first) data))))))

(deftest ^{ :cypher true } test-cypher-query-example5
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        ids   (vec (map :id [sarah john]))]
    (is (= ids (vec (map :id (nodes/multi-get ids)))))))
