(ns clojurewerkz.neocons.rest.test.transaction-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.records       :as records]
            [clojurewerkz.neocons.rest.transaction   :as tx])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

(deftest test-converting-from-tx-statement-from
  (are [x y] (= y (tx/tx-statement-from x))
      (records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})
      {:statement "CREATE (n {props}) RETURN n"
       :parameters {:props {:name "My Node"}}}))

(deftest test-converting-from-tx-payload-from
  (are [x y] (= y (tx/tx-payload-from x))
      [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})]
      {:statements [{:statement "CREATE (n {props}) RETURN n"
                    :parameters {:props {:name "My Node"}}}]}
      [] {:statements []}))

(deftest ^{:edge-features true} test-empty-transaction-rollback
  (let [[transaction y] (tx/begin)]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)
    (is (= [] y))
    (= (tx/rollback transaction) [])))

(deftest ^{:edge-features true} test-transaction-rollback
  (let [[transaction result] (tx/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])
    (= (tx/rollback transaction) [])))

(deftest ^{:edge-features true} test-transaction-commit-empty
  (let [[transaction result] (tx/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (= (tx/commit transaction) [])))

(deftest ^{:edge-features true} test-transaction-commit
  (let [[transaction result] (tx/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (let [result (tx/commit transaction [(records/instantiate-statement "CREATE n RETURN id(n)" nil)] )]
      (= (:columns result) ["id(n)"])
      (= (count (:data result)) 1))))

(deftest ^{:edge-features true} test-transaction-continue-commit
  (let [[transaction result] (tx/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (let [[a b] (tx/execute transaction [(records/instantiate-statement "CREATE n RETURN id(n)" nil)] )]
      (are [x] (not (nil? (x a)))
           :commit
           :location
           :expires)
      (= (count (:data b)) 1)
      (= (:columns b) ["id(n)"]))

    (= (tx/commit transaction) [])))

(deftest ^{:edge-features true} test-transaction-fail-rollback
  (let [[transaction result] (tx/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (is (thrown-with-msg? Exception #"Transaction failed and rolled back"
                          (tx/execute
                            transaction
                            [(records/instantiate-statement "CREATE n RETURN id(m)" nil)])))))
