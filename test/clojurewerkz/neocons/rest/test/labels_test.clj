;; Copyright (c) 2011-2018 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
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

(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest test-creating-one-label
    (let [n (nodes/create conn)]
      (is (= (labels/get-all-labels conn n) []))
      (labels/add conn n :MyLabel)
      (is (= (labels/get-all-labels conn n) [:MyLabel]))))

  (deftest test-creating-multiple-label
    (let [n (nodes/create conn)]
      (labels/add conn n [:MyLabel :MyOtherLabel])
      (is (= (set (labels/get-all-labels conn n)) #{:MyLabel :MyOtherLabel}))))

  (deftest test-creating-invalid-label
    (let [n (nodes/create conn)]
      (is (thrown-with-msg? Exception #"Unable to add label"
                            (labels/add conn n "")))))

  (deftest test-replacing-label
    (let [n (nodes/create conn)]
      (labels/add conn n :MyLabel)
      (labels/replace conn n [:MyOtherLabel :MyThirdLabel])
      (is (= (set (labels/get-all-labels conn n)) #{:MyOtherLabel :MyThirdLabel}))))

  (deftest test-deleting-label
    (let [n (nodes/create conn)]
      (labels/add conn n :MyLabel)
      (labels/remove conn n :MyLabel)
      (is (= (labels/get-all-labels conn n) []))))

  (deftest  test-get-all-nodes-with-label
    (let [n (nodes/create conn)]
      (labels/add conn n :MyLabel)
      (is (some #(= (:id %) (:id n)) (labels/get-all-nodes conn :MyLabel)))))

  (deftest test-get-all-nodes-with-label-and-property
    (let [n (nodes/create conn {"name" "bob ross"})]
      (labels/add conn n :MyLabel)
      (is (some #(= (:id %) (:id n)) (labels/get-all-nodes conn :MyLabel :name "bob ross")))))

  (deftest test-get-all-labels
    (let [n (nodes/create conn)]
      (labels/add conn n :MyLabel)
      (is (some #(= % :MyLabel) (labels/get-all-labels conn)))))

  (deftest test-creating-delete-one-strange-label
    (let [n (nodes/create conn)]
      (is (= (labels/get-all-labels conn n) []))
      (labels/add conn n "A&B")
      (labels/remove conn n "A&B")
      (is (= (labels/get-all-labels conn n) [])))))
