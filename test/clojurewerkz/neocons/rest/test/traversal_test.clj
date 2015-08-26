;; Copyright (c) 2011-2015! Michael S. Klishin, Alex Petrov, and The ClojureWerkz
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
            [clojurewerkz.neocons.rest.records :refer [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest ^{:traversal true} test-traversing-nodes-using-all-return-filter-all-relationships-and-no-pagination
    (let [john (nodes/create conn {:name "John"})
          adam (nodes/create conn {:name "Alan"})
          pete (nodes/create conn {:name "Peter"})
          _    (relationships/create conn john adam :friend)
          _    (relationships/create conn john pete :friend)
          _    (relationships/create conn adam pete :friend)
          xs1  (nodes/traverse conn (:id john) :relationships [{:direction "all" :type "friend"}])
          ids1 (set (map :id xs1))
          xs2  (nodes/traverse conn (:id john) :relationships [{:direction "all" :type "enemy"}])
          ids2 (map :id xs2)]
      (is (= #{(:id john) (:id adam) (:id pete)} ids1))
      (is (= [(:id john)] ids2))))


  (deftest ^{:traversal true} test-traversing-relationships-using-all-return-filter-all-relationships-and-no-pagination
    (let [john (nodes/create conn {:name "John"})
          adam (nodes/create conn {:name "Alan"})
          pete (nodes/create conn {:name "Peter"})
          rel1 (relationships/create conn john adam :friend)
          rel2 (relationships/create conn john pete :friend)
          rel3 (relationships/create conn adam pete :friend)
          xs1  (relationships/traverse conn (:id john) :relationships [{:direction "all" :type "friend"}])
          ids1 (set (map :id xs1))
          xs2  (relationships/traverse conn (:id john) :relationships [{:direction "all" :type "enemy"}])
          ids2 (map :id xs2)]
      (is (= #{(:id rel1) (:id rel2)} ids1))
      (is (empty? ids2))))


  (deftest ^{:traversal true} test-traversing-nodes-using-all-but-start-node-return-filter-out-relationships-and-no-pagination
    (let [john (nodes/create conn {:name "John"})
          adam (nodes/create conn {:name "Alan"})
          pete (nodes/create conn {:name "Peter"})
          _    (relationships/create conn john adam :friend)
          _    (relationships/create conn adam pete :friend)
          xs1  (nodes/traverse conn (:id john) :relationships [{:direction "out" :type "friend"}]
                               :return-filter {:language "builtin" :name "all_but_start_node"})
          ids1 (vec (map :id xs1))
          xs2  (nodes/traverse conn (:id adam) :relationships [{:direction "out" :type "friend"}]
                               :return-filter {:language "builtin" :name "all_but_start_node"})
          ids2 (vec (map :id xs2))]
      (is (= [(:id adam) (:id pete)] ids1))
      (is (= [(:id pete)] ids2))))


  (deftest ^{:traversal true} test-traversing-nodes-using-all-but-start-node-return-filter-in-relationships-and-no-pagination
    (let [john (nodes/create conn {:name "John"})
          adam (nodes/create conn {:name "Alan"})
          pete (nodes/create conn {:name "Peter"})
          _    (relationships/create conn john adam :friend)
          _    (relationships/create conn adam pete :friend)
          xs   (nodes/traverse conn (:id john) :relationships [{:direction "in" :type "friend"}]
                               :return-filter {:language "builtin" :name "all_but_start_node"})
          ids  (vec (map :id xs))]
      (is (empty? ids))))


  (deftest ^{:traversal true} test-traversing-paths-using-all-return-filter-all-relationships-and-no-pagination
    (let [john (nodes/create conn {:name "John"})
          adam (nodes/create conn {:name "Alan"})
          pete (nodes/create conn {:name "Peter"})
          rel1 (relationships/create conn john adam :friend)
          rel2 (relationships/create conn adam pete :friend)
          xs1  (paths/traverse conn (:id john) :relationships [{:direction "all" :type "friend"}])
          xs2  (paths/traverse conn (:id john) :relationships [{:direction "all" :type "enemy"}])]
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
    (let [john  (nodes/create conn {:name "John"      :age 28 :location "New York City, NY"})
          liz   (nodes/create conn {:name "Liz"       :age 27 :location "Buffalo, NY"})
          beth  (nodes/create conn {:name "Elizabeth" :age 30 :location "Chicago, IL"})
          bern  (nodes/create conn {:name "Bernard"   :age 33 :location "Zürich"})
          gael  (nodes/create conn {:name "Gaël"      :age 31 :location "Montpellier"})
          alex  (nodes/create conn {:name "Alex"      :age 24 :location "Toronto, ON"})
          rel1  (relationships/create conn john liz  :knows)
          rel2  (relationships/create conn liz  beth :knows)
          rel3  (relationships/create conn liz  bern :knows)
          rel4  (relationships/create conn bern gael :knows)
          rel5  (relationships/create conn gael beth :knows)
          rel6  (relationships/create conn beth gael :knows)
          rel7  (relationships/create conn john gael :knows)
          rt    {:type "knows" :direction "out"}
          xs1   (paths/all-shortest-between conn (:id john) (:id liz)  :relationships [rt] :max-depth 1)
          path1 (first xs1)
          xs2   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 1)
          xs3   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 2)
          path3 (first xs3)
          xs4   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 3)
          path4 (first xs4)
          xs5   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 7)
          path5 (first xs5)
          path6 (last  xs5)
          path7 (paths/shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 7)
          path8 (paths/shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 1)
          path9 (paths/shortest-between conn (:id john) (:id alex) :relationships [rt] :max-depth 1)]
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
      (is (paths/node-in? conn (:id john) path7))
      (is (not (paths/node-in? conn (:id bern) path7)))
      (is (paths/node-in? conn john path7))
      (is (not (paths/node-in? conn bern path7)))
      (is (paths/included-in? conn john path7))
      (is (or (paths/included-in? conn rel1 path7)
              (paths/included-in? conn rel7 path7)))
      (is (or (paths/relationship-in? conn (:id rel1) path7)
              (paths/relationship-in? conn (:id rel7) path7)))
      (is (or (paths/relationship-in? conn rel1 path7)
              (paths/relationship-in? conn rel7 path7)))
      (is (not (paths/included-in? conn rel4 path7)))
      (is (not (paths/relationship-in? conn (:id rel4) path7)))
      (is (not (paths/relationship-in? conn rel4 path7)))
      (is (paths/exists-between? conn (:id john) (:id liz) :relationships [rt] :max-depth 7))
      (is (not (paths/exists-between? conn (:id beth) (:id bern) :relationships [rt] :max-depth 7)))
      (is (nil? path9)))))
