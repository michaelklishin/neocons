;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.index-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.index         :as idx]
            [clojure.test :refer :all]
            [clojurewerkz.neocons.rest.test.common   :refer :all]))

(use-fixtures :once once-fixture)

(def dummy-label :DummyPerson)

(deftest ^{:edge-features true} test-indexes
  (if (= (idx/get-all *connection* dummy-label)  [])
    (let [a (idx/create *connection* dummy-label :name)]
      (is (= a {:label dummy-label, :property_keys ["name"]}))
      (idx/drop *connection* dummy-label :name)
      (is (= (idx/get-all *connection* dummy-label) [])))
    (let [b (idx/drop *connection* dummy-label :name)]
      (is (= [] (idx/get-all *connection* dummy-label)))
      (is (=(idx/create *connection* dummy-label :name)
             {:label dummy-label, :property_keys ["name"]})))))
