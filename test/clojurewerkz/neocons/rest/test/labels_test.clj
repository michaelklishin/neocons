(ns clojurewerkz.neocons.rest.test.labels-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.labels        :as labels])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

(deftest test-creating-one-label
  (let [n (nodes/create)]
    (is (= (labels/get-all-labels n) []))
    (labels/add n "MyLabel")
    (is (= (labels/get-all-labels n) ["MyLabel"]))))

(deftest test-creating-multiple-label
  (let [n (nodes/create)]
    (labels/add n ["MyLabel", "MyOtherLabel"])
    (is (= (labels/get-all-labels n) ["MyLabel", "MyOtherLabel"]))))

(deftest test-creating-invalid-label
  (let [n (nodes/create)]
    (is (thrown-with-msg? Exception #"null value not supported"
                          (labels/add n nil)))))

(deftest test-replacing-label
  (let [n (nodes/create)]
    (labels/add n "MyLabel")
    (labels/replace n ["MyOtherLabel", "MyThirdLabel"])
    (is (= (labels/get-all-labels n) ["MyOtherLabel", "MyThirdLabel"]))))

(deftest test-deleting-label
  (let [n (nodes/create)]
    (labels/add n "MyLabel")
    (labels/remove n "MyLabel")
    (is (= (labels/get-all-labels n) []))))

(deftest test-get-all-nodes-with-label
  (let [n (nodes/create)]
    (labels/add n "MyLabel")
    (is (some #(= (:id %) (:id n)) (labels/get-all-nodes "MyLabel")))))

(deftest test-get-all-nodes-with-label-and-property
  (let [n (nodes/create {"name" "bob ross"})]
    (labels/add n "MyLabel")
    (is (some #(= (:id %) (:id n)) (labels/get-all-nodes "MyLabel" "name" "bob ross")))))

(deftest test-get-all-labels
  (let [n (nodes/create)]
    (labels/add n "MyLabel")
    (is (some #(= % "MyLabel") (labels/get-all-labels)))))
