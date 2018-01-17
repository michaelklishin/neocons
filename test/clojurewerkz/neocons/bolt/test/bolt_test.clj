;; Copyright (c) 2011-2015 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.bolt.test.bolt-test
  (:require [clojurewerkz.neocons.bolt :as neobolt]
            [clojure.test :refer :all])
  (:import (java.util Map)
           (org.neo4j.driver.v1 Driver Session Transaction)))

(deftest test-connection
  (with-open [driver (neobolt/connect "bolt://localhost")]
    (is (instance? Driver driver))))

(deftest test-session
  (with-open [driver (neobolt/connect "bolt://localhost")]
    (with-open [session (neobolt/create-session driver)]
      (is (instance? Session session)))))

(deftest test-simple-query
  (with-open [driver (neobolt/connect "bolt://localhost")]
    (with-open [session (neobolt/create-session driver)]
      (let [res   (neobolt/query session "CREATE (n {name: {name}}) RETURN n.name AS name;" {"name" "Arthur"})]
        (is (= res [{"name" "Arthur"}]))))))

(deftest test-transaction-successful
  (with-open [driver (neobolt/connect "bolt://localhost")]
    (with-open [session (neobolt/create-session driver)]
      (let [n  (str (gensym "alicesuccess"))]
        (with-open [tx (neobolt/begin-tx session)]
          (is (instance? Transaction tx))
          (is (= [{"name" n}]
                 (neobolt/query tx "CREATE (n:Person {name: {name}}) RETURN n.name AS name;"
                                {"name" n})))
          (neobolt/tx-successful tx))
        (let [res  (neobolt/query session "MATCH (n:Person {name: {name}}) RETURN n.name AS name"
                                  {"name" n})]
          (is (pos? (count res))))))))

(deftest test-transaction-failure
  (with-open [driver (neobolt/connect "bolt://localhost")]
    (with-open [session (neobolt/create-session driver)]
      (let [n  (str (gensym "bobfail"))]
        (with-open [tx (neobolt/begin-tx session)]
          (is (instance? Transaction tx))
          (is (= [{"name" n}]
                 (neobolt/query tx "CREATE (n:Person {name: {name}}) RETURN n.name AS name;"
                                {"name" n})))
          (neobolt/tx-failure tx))
        (let [res  (neobolt/query session "MATCH (n:Person {name: {name}}) RETURN n.name AS name"
                                  {"name" n})]
          (is (zero? (count res))))))))
