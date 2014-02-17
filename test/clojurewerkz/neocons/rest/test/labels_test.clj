;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.labels-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.labels        :as labels]
            [clojure.test :refer :all]))

(neorest/connect! "http://localhost:7474/db/data/")

(deftest ^{:edge-features true} test-creating-one-label
  (let [n (nodes/create)]
    (is (= (labels/get-all-labels n) []))
    (labels/add n :MyLabel)
    (is (= (labels/get-all-labels n) [:MyLabel]))))

(deftest ^{:edge-features true} test-creating-multiple-label
  (let [n (nodes/create)]
    (labels/add n [:MyLabel :MyOtherLabel])
    (is (= (labels/get-all-labels n) [:MyLabel :MyOtherLabel]))))

(deftest ^{:edge-features true} test-creating-invalid-label
  (let [n (nodes/create)]
    (is (thrown-with-msg? Exception #"Unable to add label"
                          (labels/add n "")))))

(deftest ^{:edge-features true} test-replacing-label
  (let [n (nodes/create)]
    (labels/add n :MyLabel)
    (labels/replace n [:MyOtherLabel :MyThirdLabel])
    (is (= (labels/get-all-labels n) [:MyOtherLabel :MyThirdLabel]))))

(deftest ^{:edge-features true} test-deleting-label
  (let [n (nodes/create)]
    (labels/add n :MyLabel)
    (labels/remove n :MyLabel)
    (is (= (labels/get-all-labels n) []))))

(deftest ^{:edge-features true} test-get-all-nodes-with-label
  (let [n (nodes/create)]
    (labels/add n :MyLabel)
    (is (some #(= (:id %) (:id n)) (labels/get-all-nodes :MyLabel)))))

(deftest ^{:edge-features true} test-get-all-nodes-with-label-and-property
  (let [n (nodes/create {"name" "bob ross"})]
    (labels/add n :MyLabel)
    (is (some #(= (:id %) (:id n)) (labels/get-all-nodes :MyLabel :name "bob ross")))))

(deftest ^{:edge-features true} test-get-all-labels
  (let [n (nodes/create)]
    (labels/add n :MyLabel)
    (is (some #(= % :MyLabel) (labels/get-all-labels)))))

(deftest ^{:edge-features true} test-creating-delete-one-strange-label
  (let [n (nodes/create)]
    (is (= (labels/get-all-labels n) []))
    (labels/add n "A&B")
    (labels/remove n "A&B")
    (is (= (labels/get-all-labels n) []))))
