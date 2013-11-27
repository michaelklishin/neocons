(ns clojurewerkz.neocons.rest.test.batch-operations-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nn]
            [clojurewerkz.neocons.rest.batch         :as b]
            [clojure.test :refer :all]))

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
               res (doall (b/perform ops))]
           (is (= (count res) (count ops)))))
