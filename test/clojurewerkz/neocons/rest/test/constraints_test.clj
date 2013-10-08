(ns clojurewerkz.neocons.rest.test.constraints-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.constraints   :as cts])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

(def dummy-label (str (gensym "DummyPerson")))
(def dummy-constraint {:label dummy-label, :property-keys ["name"], :type "UNIQUENESS"})

; (deftest ^{:edge-features true} test-constraints
;   (try
;     (let [a (cts/create-unique dummy-label "name")]
;       (is (= a {:label dummy-label, :type "UNIQUENESS", :property-keys ["name"]})))
;     (catch Exception e (println (.getMessage e)))))

; (deftest ^{:edge-features true} test-get-constraints
;   (is (= (cts/get-unique dummy-label "name") [dummy-constraint]))
;   (is (contains? (set (cts/get-unique dummy-label)) dummy-constraint))
;   (is (contains? (set (cts/get-all dummy-label)) dummy-constraint))
;   (is (contains? (set (cts/get-all)) dummy-constraint)))

; (deftest ^{:edge-features true} test-drop-constraint
;   (do
;     (cts/drop dummy-label "name")
;     (is (not (contains?
;                (set (cts/get-unique dummy-label))
;                dummy-constraint)))))