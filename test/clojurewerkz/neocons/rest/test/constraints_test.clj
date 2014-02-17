;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
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

(neorest/connect! "http://localhost:7474/db/data/")

(def dummy-label :DummyPerson)
(def dummy-constraint {:label dummy-label :property-keys [:name] :type "UNIQUENESS"})

(deftest ^{:edge-features true} test-constraints
  (try
    (let [a (cts/create-unique dummy-label :name)]
      (is (= a dummy-constraint))
      (Thread/sleep 3000))
    (catch Exception e (.getMessage e))
    (finally
      (is (= (cts/get-unique dummy-label :name) [dummy-constraint]))
      (is (contains? (set (cts/get-unique dummy-label)) dummy-constraint))
      (is (contains? (set (cts/get-all dummy-label)) dummy-constraint))
      (is (contains? (set (cts/get-all)) dummy-constraint))
      (cts/drop-unique dummy-label :name)
      (is (not (contains?
                 (set (cts/get-unique dummy-label))
                 dummy-constraint))))))
