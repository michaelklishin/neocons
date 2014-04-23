;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.batch-operations-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nn]
            [clojurewerkz.neocons.rest.batch         :as b]
            [clojure.test :refer :all]))

(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest ^{:batching true} test-basic-batching-of-inserts
    (let [n     100
          rng   (range 0 n)
          xs    (doall (map (fn [x] {:n x}) rng))
          nodes (doall (nn/create-batch conn xs))]
      ;; ensure that we aren't tripped by laziness
      (is (= (count nodes) n))
      (is (= (count (map :id nodes)) n))
      (nn/delete-many conn (vec nodes))))



  (deftest ^{:batching true} test-generic-batch-operations-support
    (let [ops [{:method "POST"
                :to     "/node"
                :body   {}
                :id     0}
               {:method "POST"
                :to     "/node"
                :body   {}
                :id     1}
               {:method "POST",
                :to     "{0}/relationships",
                :body   {:to   "{1}"
                         :data {}
                         :type "knows"}
                :id     2}]
          res (doall (b/perform conn ops))]
      (is (= (count res) (count ops))))))
