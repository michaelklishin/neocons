(ns clojurewerkz.neocons.rest.test.batch-operations-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nn])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")


(deftest ^{:batching true} test-basic-batching-of-inserts
  (let [n     100
        rng   (range 0 n)
        xs    (doall (map (fn [x] {:n x}) rng))
        nodes (doall (nn/create-batch xs))]
    ;; ensure that we aren't tripped by laziness
    (is (= (count nodes) n))
    (is (= (count (map :id nodes)) n))
    (nn/delete-many (vec nodes))))
