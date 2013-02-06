(ns clojurewerkz.neocons.rest.test.traversal-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths         :as paths])
  (:use clojure.test
        [clojure.set :only [subset?]]
        [clojure.pprint :only [pprint]]
        [clojurewerkz.neocons.rest.records :only [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(neorest/connect! "http://localhost:7474/db/data/")

;;
;; Traversal
;;

(deftest ^{:traversal true} test-traversing-nodes-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create {:name "John"})
        adam (nodes/create {:name "Alan"})
        pete (nodes/create {:name "Peter"})
        _    (relationships/create john adam :friend)
        _    (relationships/create john pete :friend)
        _    (relationships/create adam pete :friend)
        xs1  (nodes/traverse (:id john) :relationships [{:direction "all" :type "friend"}])
        ids1 (vec (map :id xs1))
        xs2  (nodes/traverse (:id john) :relationships [{:direction "all" :type "enemy"}])
        ids2 (map :id xs2)]
    (is (= [(:id john) (:id adam) (:id pete)] ids1))
    (is (= [(:id john)] ids2))))


(deftest ^{:traversal true} test-traversing-relationships-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create {:name "John"})
        adam (nodes/create {:name "Alan"})
        pete (nodes/create {:name "Peter"})
        rel1 (relationships/create john adam :friend)
        rel2 (relationships/create john pete :friend)
        rel3 (relationships/create adam pete :friend)
        xs1  (relationships/traverse (:id john) :relationships [{:direction "all" :type "friend"}])
        ids1 (vec (map :id xs1))
        xs2  (relationships/traverse (:id john) :relationships [{:direction "all" :type "enemy"}])
        ids2 (map :id xs2)]
    (is (= [(:id rel1) (:id rel2)] ids1))
    (is (empty? ids2))))


(deftest ^{:traversal true} test-traversing-nodes-using-all-but-start-node-return-filter-out-relationships-and-no-pagination
  (let [john (nodes/create {:name "John"})
        adam (nodes/create {:name "Alan"})
        pete (nodes/create {:name "Peter"})
        _    (relationships/create john adam :friend)
        _    (relationships/create adam pete :friend)
        xs1  (nodes/traverse (:id john) :relationships [{:direction "out" :type "friend"}] :return-filter {:language "builtin" :name "all_but_start_node"})
        ids1 (vec (map :id xs1))
        xs2  (nodes/traverse (:id adam) :relationships [{:direction "out" :type "friend"}] :return-filter {:language "builtin" :name "all_but_start_node"})
        ids2 (vec (map :id xs2))]
    (is (= [(:id adam) (:id pete)] ids1))
    (is (= [(:id pete)] ids2))))


(deftest ^{:traversal true} test-traversing-nodes-using-all-but-start-node-return-filter-in-relationships-and-no-pagination
  (let [john (nodes/create {:name "John"})
        adam (nodes/create {:name "Alan"})
        pete (nodes/create {:name "Peter"})
        _    (relationships/create john adam :friend)
        _    (relationships/create adam pete :friend)
        xs   (nodes/traverse (:id john) :relationships [{:direction "in" :type "friend"}] :return-filter {:language "builtin" :name "all_but_start_node"})
        ids  (vec (map :id xs))]
    (is (empty? ids))))


(deftest ^{:traversal true} test-traversing-paths-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create {:name "John"})
        adam (nodes/create {:name "Alan"})
        pete (nodes/create {:name "Peter"})
        rel1 (relationships/create john adam :friend)
        rel2 (relationships/create adam pete :friend)
        xs1  (paths/traverse (:id john) :relationships [{:direction "all" :type "friend"}])
        xs2  (paths/traverse (:id john) :relationships [{:direction "all" :type "enemy"}])]
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
  (let [john (nodes/create {:name "John" :age 28 :location "New York City, NY"})
        liz  (nodes/create {:name "Liz"  :age 27 :location "Buffalo, NY"})
        beth (nodes/create {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        bern (nodes/create {:name "Bernard"   :age 33 :location "Zürich"})
        gael (nodes/create {:name "Gaël"      :age 31 :location "Montpellier"})
        alex (nodes/create {:name "Alex"      :age 24 :location "Toronto, ON"})
        rel1 (relationships/create john liz  :knows)
        rel2 (relationships/create liz  beth :knows)
        rel3 (relationships/create liz  bern :knows)
        rel4 (relationships/create bern gael :knows)
        rel5 (relationships/create gael beth :knows)
        rel6 (relationships/create beth gael :knows)
        rel7 (relationships/create john gael :knows)
        rt   {:type "knows" :direction "out"}
        xs1   (paths/all-shortest-between (:id john) (:id liz)  :relationships [rt] :max-depth 1)
        path1 (first xs1)
        xs2   (paths/all-shortest-between (:id john) (:id beth) :relationships [rt] :max-depth 1)
        xs3   (paths/all-shortest-between (:id john) (:id beth) :relationships [rt] :max-depth 2)
        path3 (first xs3)
        xs4   (paths/all-shortest-between (:id john) (:id beth) :relationships [rt] :max-depth 3)
        path4 (first xs4)
        xs5   (paths/all-shortest-between (:id john) (:id beth) :relationships [rt] :max-depth 7)
        path5 (first xs5)
        path6 (last  xs5)
        path7 (paths/shortest-between (:id john) (:id beth) :relationships [rt] :max-depth 7)
        path8 (paths/shortest-between (:id john) (:id beth) :relationships [rt] :max-depth 1)
        path9 (paths/shortest-between (:id john) (:id alex) :relationships [rt] :max-depth 1)]
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
    (is (paths/node-in? (:id john) path7))
    (is (not (paths/node-in? (:id bern) path7)))
    (is (paths/node-in? john path7))
    (is (not (paths/node-in? bern path7)))
    (is (paths/included-in? john path7))
    (is (paths/included-in? rel1 path7))
    (is (paths/relationship-in? (:id rel1) path7))
    (is (paths/relationship-in? rel1 path7))
    (is (not (paths/included-in? rel4 path7)))
    (is (not (paths/relationship-in? (:id rel4) path7)))
    (is (not (paths/relationship-in? rel4 path7)))
    (is (paths/exists-between? (:id john) (:id liz) :relationships [rt] :max-depth 7))
    (is (not (paths/exists-between? (:id beth) (:id bern) :relationships [rt] :max-depth 7)))
    (is (nil? path9))))
