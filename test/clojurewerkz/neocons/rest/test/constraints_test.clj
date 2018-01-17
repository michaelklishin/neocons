;; Copyright (c) 2011-2018 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.constraints-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.constraints   :as cts]
            [clojure.test :refer :all]))

(def dummy-label :DummyPerson)
(def dummy-constraint {:label dummy-label :property_keys ["name"] :type "UNIQUENESS"})

(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest test-constraints
    (try
      (let [a (cts/create-unique conn dummy-label :name)]
        (is (= a dummy-constraint))
        (Thread/sleep 3000))
      (catch Exception e (.getMessage e))
      (finally
        (is (= (cts/get-unique conn dummy-label :name) [dummy-constraint]))
        (is (contains? (set (cts/get-unique conn dummy-label)) dummy-constraint))
        (is (contains? (set (cts/get-all conn dummy-label)) dummy-constraint))
        (is (contains? (set (cts/get-all conn)) dummy-constraint))
        (cts/drop-unique conn dummy-label :name)
        (is (not (contains?
                  (set (cts/get-unique conn dummy-label))
                  dummy-constraint)))))))
