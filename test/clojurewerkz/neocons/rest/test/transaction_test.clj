(ns clojurewerkz.neocons.rest.test.transaction-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.transaction   :as tx])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

(deftest test-converting-from-tx-statement-from
  (are [x y] (= y (tx/tx-statement-from x))
      (tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})
      {:statement "CREATE (n {props}) RETURN n"
       :parameters {:props {:name "My Node"}}}))

(deftest test-converting-from-tx-payload-from
  (are [x y] (= y (tx/tx-payload-from x))
      [(tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})]
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
  (let [[transaction result] (tx/begin [(tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])
    (= (tx/rollback transaction) [])))

(deftest ^{:edge-features true} test-transaction-commit-empty
  (let [[transaction result] (tx/begin [(tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (= (tx/commit transaction) [])))

(deftest ^{:edge-features true} test-transaction-commit
  (let [[transaction result] (tx/begin [(tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (let [result (tx/commit transaction [(tx/statement "CREATE n RETURN id(n)" nil)] )]
      (= (:columns result) ["id(n)"])
      (= (count (:data result)) 1))))

(deftest ^{:edge-features true} test-transaction-continue-commit
  (let [[transaction result] (tx/begin [(tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (let [[a b] (tx/execute transaction [(tx/statement "CREATE n RETURN id(n)" nil)] )]
      (are [x] (not (nil? (x a)))
           :commit
           :location
           :expires)
      (= (count (:data b)) 1)
      (= (:columns b) ["id(n)"]))

    (= (tx/commit transaction) [])))

(deftest ^{:edge-features true} test-transaction-fail-rollback
  (let [[transaction result] (tx/begin [(tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})])]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)

    (= (:data result) [{:row [{:name "My Node"}]}])
    (= (:columns result) ["n"])

    (is (thrown-with-msg? Exception #"Transaction failed and rolled back"
                          (tx/execute
                            transaction
                            [(tx/statement "CREATE n RETURN id(m)" nil)])))))

(deftest ^{:edge-features true} test-simple-transaction
  (let [result (tx/in-transaction
                  (tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Node"}})
                  (tx/statement "CREATE (n {props}) RETURN n" {:props {:name "My Another Node"}}))]
    (is (= (count result) 2))
    (is (= (:data (first result)) [{:row [{:name "My Node"}]}]))
    (is (= (:columns (first result)) ["n"] ))
    (is (= (:data (second result)) [{:row [{:name "My Another Node"}]}]))
    (is (= (:columns (second result)) ["n"] ))))

(deftest ^{:edge-features true} test-empty-begin-tx
  (let [transaction (tx/begin-tx)]
    (are [x] (not (nil? (x transaction)))
         :commit
         :location
         :expires)))


(deftest ^{:edge-features true} test-with-transaction-commit-success
  (let [transaction (tx/begin-tx)]
    (is (= (tx/with-transaction
             transaction
             true
             (let [[_ r] (tx/execute
                           transaction
                           [(tx/statement "CREATE (n {props}) RETURN n"
                                          {:props {:name "My Node"}})])]
               (= (count (:data r)) 1)
               (= (:data r) [{:row [{:name "My Node"}]}])
               (= (:columns r) ["n"]))))
        [])))


(deftest ^{:edge-features true} test-with-transaction-rollback-success
  (let [transaction (tx/begin-tx)]
    (tx/with-transaction
      transaction
      false
      (let [[_ r] (tx/execute
                    transaction
                    [(tx/statement "CREATE (n {props}) RETURN n"
                                   {:props {:name "My Node"}})])]
        (= (count (:data r)) 1)
        (= (:data r) [{:row [{:name "My Node"}]}])
        (= (:columns r) ["n"]))
      (is (= (tx/rollback transaction)
             [])))))

(deftest ^{:edge-features true} test-with-transaction-manual-failure
  (let [transaction (tx/begin-tx)]
    (is (thrown-with-msg?
          Exception #"Rolling back"
          (tx/with-transaction
            transaction
            true
            (tx/execute
              transaction [(tx/statement "CREATE (n) RETURN ID(n)")])
            (throw (Exception. "Rolling back")))))))

(deftest ^{:edge-features true} test-with-transaction-transaction-failure
  (let [transaction (tx/begin-tx)]
    (is (thrown-with-msg?
          Exception #"STATEMENT_SYNTAX_ERROR"
          (tx/with-transaction
            transaction
            true
            (tx/execute
              transaction [(tx/statement "CREATE (n) RETURN ID(m)")]))))))
