(ns clojurewerkz.neocons.rest.test.index-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.index         :as idx]
            [clojure.test :refer :all]))

(neorest/connect! "http://localhost:7474/db/data/")

(def dummy-label :DummyPerson)

(deftest ^{:edge-features true} test-indexes
  (if (= (idx/get-all dummy-label)  [])
    (let [a (idx/create dummy-label :name)]
      (is (= a {:label dummy-label, :property_keys ["name"]}))
      (idx/drop dummy-label :name)
      (is (= (idx/get-all dummy-label) [])))
    (let [b (idx/drop dummy-label :name)]
      (is (= [] (idx/get-all dummy-label)))
      (is (=(idx/create dummy-label :name)
             {:label dummy-label, :property_keys ["name"]})))))
