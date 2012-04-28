(ns clojurewerkz.neocons.rest.test-mutating-cypher
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            clojurewerkz.neocons.rest.records
            [clojurewerkz.neocons.rest.cypher        :as cy])
  (:use clojure.test)
  (:import clojurewerkz.neocons.rest.records.Node))

(neorest/connect! "http://localhost:7474/db/data/")

(defn- exists?
  [^Node node]
  (try
    (nodes/get (:id node))
    true
    (catch Exception e
      false)))

(def ^{:private true}
  missing? (complement exists?))


;;
;; Mutating Cypher queries
;;

(deftest ^{:cypher true :edge-features true} test-purging-a-node-via-mutating-cypher-case1
  (let [john (nodes/create {:name "John" :age 28 :location "New York City, NY"})
        beth (nodes/create {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        gael (nodes/create {:name "Gaël"      :age 31 :location "Montpellier"})
        rel1 (relationships/create john beth :knows)
        rel2 (relationships/create john gael :knows)]
    (is (thrown? Exception
                 (nodes/delete (:id john))))
    (is (cy/empty? (cy/query "START n = node({sid}) MATCH n-[r]-() DELETE n, r" {:sid (:id john)})))
    (is (missing? john))
    (is (exists? beth))
    (is (exists? gael))))
