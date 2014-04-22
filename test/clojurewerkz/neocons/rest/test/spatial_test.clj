;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.spatial-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.spatial       :as sp]
            [clojurewerkz.neocons.rest.test.common   :refer :all]
            [clojure.test :refer :all]))

(use-fixtures :once once-fixture)

(deftest ^{:spatial true} test-create-layer
  (let [created-layer-1 (sp/add-simple-point-layer *connection* "test-layer-1")
        created-layer-2 (sp/add-simple-point-layer *connection* "test-layer-2" "x" "y")]
    (is (= "test-layer-1" (:layer (:data created-layer-1))))
    (is (= "test-layer-2" (:layer (:data created-layer-2))))
    (is (= "y:x" (:geomencoder_config (:data created-layer-2))))))

(deftest ^{:spatial true} test-add-node-to-layer
  (let [created-layer-1 (sp/add-simple-point-layer *connection* "test-layer-add")
        test-node (nodes/create *connection* {:latitude 60.1 :longitude 15.4 :foo "bar"})
        added-node (sp/add-node-to-layer *connection* "test-layer-add" test-node)]
    (is (= [15.4 60.1 15.4 60.1] (:bbox (:data added-node))))))

(deftest ^{:spatial true} test-find-within-distance
  (let [created-layer-1 (sp/add-simple-point-layer *connection* "test-layer-search")
        test-node (nodes/create *connection* {:latitude -60.1 :longitude -15.4 :foo "target test node"})
        added-node (sp/add-node-to-layer *connection* "test-layer-search" test-node)
        found (sp/find-within-distance *connection* "test-layer-search" -15.0 -60.0 100)]
    (is (= "target test node" (:foo (:data (first found)))))))
