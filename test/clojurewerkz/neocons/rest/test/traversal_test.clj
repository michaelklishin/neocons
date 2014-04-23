;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.traversal-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojure.test :refer :all]
            [clojure.set :refer [subset?]]
            [clojurewerkz.neocons.rest.test.common   :refer :all]
            [clojurewerkz.neocons.rest.records :refer [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(use-fixtures :once once-fixture)

;;
;; Traversal
;;

(deftest ^{:traversal true} test-traversing-nodes-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create *connection* {:name "John"})
        adam (nodes/create *connection* {:name "Alan"})
        pete (nodes/create *connection* {:name "Peter"})
        _    (relationships/create *connection* john adam :friend)
        _    (relationships/create *connection* john pete :friend)
        _    (relationships/create *connection* adam pete :friend)
        xs1  (nodes/traverse *connection* (:id john) :relationships [{:direction "all" :type "friend"}])
        ids1 (vec (map :id xs1))
        xs2  (nodes/traverse *connection* (:id john) :relationships [{:direction "all" :type "enemy"}])
        ids2 (map :id xs2)]
    (is (= [(:id john) (:id adam) (:id pete)] ids1))
    (is (= [(:id john)] ids2))))


(deftest ^{:traversal true} test-traversing-relationships-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create *connection* {:name "John"})
        adam (nodes/create *connection* {:name "Alan"})
        pete (nodes/create *connection* {:name "Peter"})
        rel1 (relationships/create *connection* john adam :friend)
        rel2 (relationships/create *connection* john pete :friend)
        rel3 (relationships/create *connection* adam pete :friend)
        xs1  (relationships/traverse *connection* (:id john) :relationships [{:direction "all" :type "friend"}])
        ids1 (vec (map :id xs1))
        xs2  (relationships/traverse *connection* (:id john) :relationships [{:direction "all" :type "enemy"}])
        ids2 (map :id xs2)]
    (is (= [(:id rel1) (:id rel2)] ids1))
    (is (empty? ids2))))


(deftest ^{:traversal true} test-traversing-nodes-using-all-but-start-node-return-filter-out-relationships-and-no-pagination
  (let [john (nodes/create *connection* {:name "John"})
        adam (nodes/create *connection* {:name "Alan"})
        pete (nodes/create *connection* {:name "Peter"})
        _    (relationships/create *connection* john adam :friend)
        _    (relationships/create *connection* adam pete :friend)
        xs1  (nodes/traverse *connection* (:id john) :relationships [{:direction "out" :type "friend"}]
                             :return-filter {:language "builtin" :name "all_but_start_node"})
        ids1 (vec (map :id xs1))
        xs2  (nodes/traverse *connection* (:id adam) :relationships [{:direction "out" :type "friend"}]
                             :return-filter {:language "builtin" :name "all_but_start_node"})
        ids2 (vec (map :id xs2))]
    (is (= [(:id adam) (:id pete)] ids1))
    (is (= [(:id pete)] ids2))))


(deftest ^{:traversal true} test-traversing-nodes-using-all-but-start-node-return-filter-in-relationships-and-no-pagination
  (let [john (nodes/create *connection* {:name "John"})
        adam (nodes/create *connection* {:name "Alan"})
        pete (nodes/create *connection* {:name "Peter"})
        _    (relationships/create *connection* john adam :friend)
        _    (relationships/create *connection* adam pete :friend)
        xs   (nodes/traverse *connection* (:id john) :relationships [{:direction "in" :type "friend"}]
                             :return-filter {:language "builtin" :name "all_but_start_node"})
        ids  (vec (map :id xs))]
    (is (empty? ids))))


(deftest ^{:traversal true} test-traversing-paths-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create *connection* {:name "John"})
        adam (nodes/create *connection* {:name "Alan"})
        pete (nodes/create *connection* {:name "Peter"})
        rel1 (relationships/create *connection* john adam :friend)
        rel2 (relationships/create *connection* adam pete :friend)
        xs1  (paths/traverse *connection* (:id john) :relationships [{:direction "all" :type "friend"}])
        xs2  (paths/traverse *connection* (:id john) :relationships [{:direction "all" :type "enemy"}])]
    (is (= 3 (count xs1)))
    (is (= 1 (count xs2)))
    (let [path1 (first xs1)
          path2 (second xs1)
          path3 (last xs1)]
      (is (= 0 (:length path1)))
      (is (= 1 (:length path2)))
      (is (= 2 (:length path3)))
      (is (= 1 (count (:nodes path1))))
      (is (= 2 (count (:nodes path2))))
      (is (= 3 (count (:nodes path3))))
      (is (= 0 (count (:relationships path1))))
      (is (= 1 (count (:relationships path2))))
      (is (= 2 (count (:relationships path3))))
      (is (= (:location-uri john) (:start path1)))
      (is (= (:location-uri john) (:end   path1)))
      (is (= (:location-uri john) (:start path2)))
      (is (= (:location-uri adam) (:end   path2)))
      (is (= (:location-uri john) (:start path3)))
      (is (= (:location-uri pete) (:end   path3))))))


;;
;; Shortest path algorithm
;;

(deftest ^{:traversal true} test-shortest-path-algorithm-1
  (let [john (nodes/create *connection* {:name "John"      :age 28 :location "New York City, NY"})
        liz  (nodes/create *connection* {:name "Liz"       :age 27 :location "Buffalo, NY"})
        beth (nodes/create *connection* {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        bern (nodes/create *connection* {:name "Bernard"   :age 33 :location "Zürich"})
        gael (nodes/create *connection* {:name "Gaël"      :age 31 :location "Montpellier"})
        alex (nodes/create *connection* {:name "Alex"      :age 24 :location "Toronto, ON"})
        rel1 (relationships/create *connection* john liz  :knows)
        rel2 (relationships/create *connection* liz  beth :knows)
        rel3 (relationships/create *connection* liz  bern :knows)
        rel4 (relationships/create *connection* bern gael :knows)
        rel5 (relationships/create *connection* gael beth :knows)
        rel6 (relationships/create *connection* beth gael :knows)
        rel7 (relationships/create *connection* john gael :knows)
        rt   {:type "knows" :direction "out"}
        xs1   (paths/all-shortest-between *connection* (:id john) (:id liz)  :relationships [rt] :max-depth 1)
        path1 (first xs1)
        xs2   (paths/all-shortest-between *connection* (:id john) (:id beth) :relationships [rt] :max-depth 1)
        xs3   (paths/all-shortest-between *connection* (:id john) (:id beth) :relationships [rt] :max-depth 2)
        path3 (first xs3)
        xs4   (paths/all-shortest-between *connection* (:id john) (:id beth) :relationships [rt] :max-depth 3)
        path4 (first xs4)
        xs5   (paths/all-shortest-between *connection* (:id john) (:id beth) :relationships [rt] :max-depth 7)
        path5 (first xs5)
        path6 (last  xs5)
        path7 (paths/shortest-between *connection* (:id john) (:id beth) :relationships [rt] :max-depth 7)
        path8 (paths/shortest-between *connection* (:id john) (:id beth) :relationships [rt] :max-depth 1)
        path9 (paths/shortest-between *connection* (:id john) (:id alex) :relationships [rt] :max-depth 1)]
    (is (empty? xs2))
    (is (nil? path8))
    (is (= 1 (count xs1)))
    (is (= 2 (count xs3)))
    (is (= 2 (count xs4)))
    (is (= 2 (count xs5)))
    (is (= (:start path1) (:location-uri john)))
    (is (= (:end   path1) (:location-uri liz)))
    (is (= 2 (:length path3)))
    (is (= (:start path3) (:location-uri john)))
    (is (= (:end   path3) (:location-uri beth)))
    (is (= 2 (:length path4)))
    (is (= (:start path4) (:location-uri john)))
    (is (= (:end   path4) (:location-uri beth)))
    (is (= 2 (:length path5)))
    (is (= (:start path5) (:location-uri john)))
    (is (= (:end   path5) (:location-uri beth)))
    (is (= 2 (:length path6)))
    (is (= (:start path6) (:location-uri john)))
    (is (= (:end   path6) (:location-uri beth)))
    (is (= 2 (:length path7)))
    (is (= (:start path7) (:location-uri john)))
    (is (= (:end   path7) (:location-uri beth)))
    (is (paths/node-in? *connection* (:id john) path7))
    (is (not (paths/node-in? *connection* (:id bern) path7)))
    (is (paths/node-in? *connection* john path7))
    (is (not (paths/node-in? *connection* bern path7)))
    (is (paths/included-in? *connection* john path7))
    (is (paths/included-in? *connection* rel1 path7))
    (is (paths/relationship-in? *connection* (:id rel1) path7))
    (is (paths/relationship-in? *connection* rel1 path7))
    (is (not (paths/included-in? *connection* rel4 path7)))
    (is (not (paths/relationship-in? *connection* (:id rel4) path7)))
    (is (not (paths/relationship-in? *connection* rel4 path7)))
    (is (paths/exists-between? *connection* (:id john) (:id liz) :relationships [rt] :max-depth 7))
    (is (not (paths/exists-between? *connection* (:id beth) (:id bern) :relationships [rt] :max-depth 7)))
    (is (nil? path9))))
