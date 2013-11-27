(ns clojurewerkz.neocons.rest.test.spatial-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.spatial       :as sp]
            [clojure.test :refer :all]))

(neorest/connect! "http://localhost:7474/db/data/")

(deftest ^{:spatial true} test-create-layer
  (let [created-layer-1 (sp/add-simple-point-layer "test-layer-1")
        created-layer-2 (sp/add-simple-point-layer "test-layer-2" "x" "y")]
    (is (= "test-layer-1" (:layer (:data created-layer-1))))
    (is (= "test-layer-2" (:layer (:data created-layer-2))))
    (is (= "y:x" (:geomencoder_config (:data created-layer-2))))))

(deftest ^{:spatial true} test-add-node-to-layer
  (let [created-layer-1 (sp/add-simple-point-layer "test-layer-add")
        test-node (nodes/create {:latitude 60.1 :longitude 15.4 :foo "bar"})
        added-node (sp/add-node-to-layer "test-layer-add" test-node)]
    (is (= [15.4 60.1 15.4 60.1] (:bbox (:data added-node))))))

(deftest ^{:spatial true} test-find-within-distance
  (let [created-layer-1 (sp/add-simple-point-layer "test-layer-search")
        test-node (nodes/create {:latitude -60.1 :longitude -15.4 :foo "target test node"})
        added-node (sp/add-node-to-layer "test-layer-search" test-node)
        found (sp/find-within-distance "test-layer-search" -15.0 -60.0 100)]
    (is (= "target test node" (:foo (:data (first found)))))))
