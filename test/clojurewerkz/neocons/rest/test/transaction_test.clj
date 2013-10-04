(ns clojurewerkz.neocons.rest.test.transaction-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.records :as records]
            [clojurewerkz.neocons.rest.transaction   :as neotrans])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

(deftest test-converting-from-statement-to-map
  (are [x y] (= y (neotrans/statement-to-map x))
      (records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})
      {:statement "CREATE (n {props}) RETURN n"
       :parameters {:props {:name "My Node"}}}))

(deftest test-converting-from-statements-to-map
  (are [x y] (= y (neotrans/statements-to-map x))
      [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})]
      {:statements [{:statement "CREATE (n {props}) RETURN n"
                    :parameters {:props {:name "My Node"}}}]}
      [] {:statements []}))

(deftest ^{:edge-features true} test-empty-transaction-rollback
  (let [[transaction y] (neotrans/begin)]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)
    (is (= [] y))
    (= (neotrans/rollback transaction) {"results" [] "errors" []})))

(deftest ^{:edge-features true} test-transaction-rollback
  (let [[transaction result] (neotrans/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])
    (= (neotrans/rollback transaction) {"results" [] "errors" []})))

(deftest ^{:edge-features true} test-transaction-commit-empty
  (let [[transaction result] (neotrans/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (= (neotrans/commit transaction) [])))

(deftest ^{:edge-features true} test-transaction-commit
  (let [[transaction result] (neotrans/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (let [result (neotrans/commit transaction [(records/instantiate-statement "CREATE n RETURN id(n)" nil)] )]
      (= (:columns result) ["id(n)"])
      (= (count (:data result)) 1))))

(deftest ^{:edge-features true} test-transaction-continue-commit
  (let [[transaction result] (neotrans/begin [(records/instantiate-statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (let [[a b] (neotrans/execute transaction [(records/instantiate-statement "CREATE n RETURN id(n)" nil)] )]
      (are [x] (not (nil? (x a)))
           :commit
           :location
           :expires)
      (= (count (:data b)) 1)
      (= (:columns b) ["id(n)"]))

    (= (neotrans/commit transaction) [])))
