(ns clojurewerkz.neocons.rest.test.index-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.index         :as idx])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

(def dummy-label (str (gensym "DummyPerson")))

(deftest ^{:edge-features true} test-indexes
  (if (= (idx/get-all-indexes dummy-label)  [])
    (let [a (idx/create dummy-label "name")]
      (is (= a {:label dummy-label, :property-keys ["name"]}))
      (idx/drop dummy-label "name")
      (is (= (idx/get-all-indexes dummy-label) [])))
    (let [b (idx/drop dummy-label "name")]
      (is (= [] (idx/get-all-indexes dummy-label)))
      (is (=(idx/create dummy-label "name")
             {:label "DummyPerson4820", :property-keys ["name"]})))))
