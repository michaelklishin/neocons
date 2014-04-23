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
            [clojurewerkz.neocons.rest.test.common   :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :once once-fixture)


(deftest test-creating-one-label
  (let [n (nodes/create *connection*)]
    (is (= (labels/get-all-labels *connection* n) []))
    (labels/add *connection* n :MyLabel)
    (is (= (labels/get-all-labels *connection* n) [:MyLabel]))))

(deftest test-creating-multiple-label
  (let [n (nodes/create *connection*)]
    (labels/add *connection* n [:MyLabel :MyOtherLabel])
    (is (= (labels/get-all-labels *connection* n) [:MyLabel :MyOtherLabel]))))

(deftest test-creating-invalid-label
  (let [n (nodes/create *connection*)]
    (is (thrown-with-msg? Exception #"Unable to add label"
                          (labels/add *connection* n "")))))

(deftest test-replacing-label
  (let [n (nodes/create *connection*)]
    (labels/add *connection* n :MyLabel)
    (labels/replace *connection* n [:MyOtherLabel :MyThirdLabel])
    (is (= (labels/get-all-labels *connection* n) [:MyOtherLabel :MyThirdLabel]))))

(deftest test-deleting-label
  (let [n (nodes/create *connection*)]
    (labels/add *connection* n :MyLabel)
    (labels/remove *connection* n :MyLabel)
    (is (= (labels/get-all-labels *connection* n) []))))

(deftest  test-get-all-nodes-with-label
  (let [n (nodes/create *connection*)]
    (labels/add *connection* n :MyLabel)
    (is (some #(= (:id %) (:id n)) (labels/get-all-nodes *connection* :MyLabel)))))

(deftest test-get-all-nodes-with-label-and-property
  (let [n (nodes/create *connection* {"name" "bob ross"})]
    (labels/add *connection* n :MyLabel)
    (is (some #(= (:id %) (:id n)) (labels/get-all-nodes *connection* :MyLabel :name "bob ross")))))

(deftest test-get-all-labels
  (let [n (nodes/create *connection*)]
    (labels/add *connection* n :MyLabel)
    (is (some #(= % :MyLabel) (labels/get-all-labels *connection*)))))

(deftest test-creating-delete-one-strange-label
  (let [n (nodes/create *connection*)]
    (is (= (labels/get-all-labels *connection* n) []))
    (labels/add *connection* n "A&B")
    (labels/remove *connection* n "A&B")
    (is (= (labels/get-all-labels *connection* n) []))))
