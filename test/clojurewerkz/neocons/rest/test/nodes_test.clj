(ns clojurewerkz.neocons.rest.test.nodes-test
  (:require [clojurewerkz.neocons.rest       :as neorest]
            [clojurewerkz.neocons.rest.nodes :as nodes])
  (:use clojure.test
        [clojurewerkz.neocons.rest.records :only [instantiate-node-from]]))

(neorest/connect! "http://localhost:7474/db/data/")


;;
;; Working with nodes
;;

(deftest test-creating-and-immediately-accessing-a-node-without-properties
  (let [created-node (nodes/create)
        fetched-node (nodes/get (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))))

(deftest test-creating-and-immediately-accessing-a-node-with-properties
  (let [data         {:key "value"}
        created-node (nodes/create data)
        fetched-node (nodes/get (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))
    (is (= (:data created-node) data))))


(deftest test-accessing-a-non-existent-node
  (is (thrown? Exception
               (nodes/get 928398827))))


(deftest test-creating-and-deleting-a-node-with-properties
  (let [data         {:key "value"}
        created-node (nodes/create data)
        [deleted-id status] (nodes/delete (:id created-node))]
    (is (= 204 status))
    (is (= (:id created-node) deleted-id))))

(deftest test-attempting-to-delete-a-non-existent-node
  (is (thrown? Exception
               (nodes/delete 237737737))))


(deftest test-creating-and-getting-properties-of-one-node
  (let [data         {:key "value"}
        created-node (nodes/create data)
        fetched-data (nodes/get-properties (:id created-node))]
    (is (= data fetched-data))))

(deftest test-creating-and-getting-empty-properties-of-one-node
  (let [created-node (nodes/create)
        fetched-data (nodes/get-properties (:id created-node))]
    (is (= {} fetched-data))))


(deftest test-updating-a-single-node-property
  (let [node         (nodes/create {:age 26})
        fetched-node (nodes/get (:id node))
        new-value    (nodes/set-property (:id node) :age 27)
        updated-node (nodes/get (:id fetched-node))]
    (is (= new-value (-> updated-node :data :age)))))


(deftest test-updating-node-properties
  (let [node         (nodes/create {:age 26})
        fetched-node (nodes/get (:id node))
        new-data    (nodes/update (:id node) {:age 27 :gender "male"})
        updated-node (nodes/get (:id fetched-node))]
    (is (= new-data (-> updated-node :data)))))


(deftest test-deleting-all-properties-from-a-node
  (let [data         {:key "value"}
        created-node (nodes/create data)
        fetched-data (nodes/get-properties (:id created-node))]
    (is (= data fetched-data))
    (nodes/delete-properties (:id created-node))
    (is (= {} (nodes/get-properties (:id created-node))))))
