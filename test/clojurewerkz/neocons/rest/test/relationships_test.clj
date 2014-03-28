;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.relationships-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojure.test :refer :all]
            [clojure.set :refer [subset?]]
            [clojurewerkz.neocons.rest.records :refer [instantiate-node-from instantiate-rel-from]]))

(neorest/connect! "http://localhost:7474/db/data/")

(def ^{:private true}
  ids-from (partial map :id))

;;
;; Working with relationships
;;

(deftest test-creating-and-immediately-accessing-a-relationship-without-properties
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))))

(deftest test-creating-the-same-relationship-without-properties-twice
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel   (relationships/create from-node to-node :links)
        created-rel2  (relationships/create from-node to-node :links)
        fetched-rel   (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (not (= (:id created-rel) (:id created-rel2))))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= (:type created-rel) (:type created-rel2)))))

(deftest test-avoiding-creating-the-same-relationship-without-properties-twice
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel   (relationships/maybe-create from-node to-node :links)
        created-rel2  (relationships/maybe-create from-node to-node :links)
        fetched-rel   (relationships/get (:id created-rel))]
    (is (= created-rel created-rel2))
    (is (= (:id created-rel)   (:id fetched-rel)))
    (is (= (:id created-rel)   (:id created-rel2)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= (:type created-rel) (:type created-rel2)))))

(deftest test-avoiding-creating-the-same-relationship-with-ids
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel   (relationships/maybe-create (:id from-node) (:id to-node) :links)
        created-rel2  (relationships/maybe-create (:id from-node) (:id to-node) :links)
        fetched-rel   (relationships/get (:id created-rel))]
    (is (= created-rel created-rel2))
    (is (= (:id created-rel)   (:id fetched-rel)))
    (is (= (:id created-rel)   (:id created-rel2)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= (:type created-rel) (:type created-rel2)))))

(deftest test-avoiding-destroying-the-same-relationship-without-properties-twice
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        rt           {:type "links" :direction "out"}]
    (is (relationships/first-outgoing-between from-node to-node [:links]))
    (is (nil? (relationships/first-outgoing-between from-node to-node [:loves])))
    (is (= created-rel (relationships/first-outgoing-between from-node to-node [:links])))
    (is (paths/exists-between? (:id from-node) (:id to-node) :relationships [rt] :max-depth 1))
    (relationships/maybe-delete-outgoing from-node to-node [:links])
    (is (nil? (relationships/first-outgoing-between from-node to-node [:links])))
    (is (not (paths/exists-between? (:id from-node) (:id to-node) :relationships [rt] :max-depth 1)))
    (relationships/maybe-delete-outgoing from-node to-node [:links])))

(deftest test-creating-and-immediately-accessing-a-relationship-with-properties
  (let [data         {:one "uno" :two "due"}
        from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links data)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= (:data created-rel) (:data fetched-rel)))))

(deftest test-creating-and-immediately-accessing-a-unique-relationship-in-index
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-r1   (relationships/create-unique-in-index from-node to-node :links "edges" "test" "test-1")
        created-r2   (relationships/create-unique-in-index from-node to-node :links "edges" "test" "test-1")]
    (is (= (:id created-r1) (:id created-r2)))
    (is (= (:type created-r1) (:type created-r2)))
    (is (relationships/find-one "edges" "test" "test-1"))))

(deftest test-creating-and-immediately-accessing-a-unique-relationship-in-index-with-properties
  (let [data         {:one "uno" :two "due"}
        from-node    (nodes/create)
        to-node      (nodes/create)
        created-r1   (relationships/create-unique-in-index from-node to-node :links "edges" "test" "test-2" data)
        created-r2   (relationships/create-unique-in-index from-node to-node :links "edges" "test" "test-2" data)
        fetched-r1   (relationships/get (:id created-r1))
        fetched-r2   (relationships/get (:id created-r2))]
    (is (= (:id created-r1) (:id created-r2)))
    (is (= (:type created-r1) (:type created-r2)))
    (is (= (:data created-r1) (:data created-r2)))
    (is (= (:data created-r1) (:data fetched-r2)))
    (is (= (:id created-r2) (:id fetched-r1)))))

(deftest test-creating-and-deleting-a-relationship-without-properties
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        [deleted-id status] (relationships/delete (:id created-rel))]
    (is (= 204 status))))

(deftest test-creating-and-deleting-a-non-existent-relationship
  ;; this should be slingshot.ExceptionInfo on 1.3 but
  ;; clojure.lang.ExceptionInfo on 1.4.0+. This Slingshot shit is annoying. MK.
  (is (thrown? Exception
               (relationships/delete 87238467666)))
  (try
    (relationships/delete 87238467666)
    (catch Exception e
      (let [d (.getData e)]
        (is (= (-> d :object :status) 404))))))

(deftest test-creating-multiple-relationships-at-once
  (let [from-node    (nodes/create)
        to-node1     (nodes/create)
        to-node2     (nodes/create)
        to-node3     (nodes/create)
        created-rels (relationships/create-many from-node [to-node1 to-node2 to-node3] :links)]
    (is (= 3 (count created-rels)))
    ;; give graph DB a moment to be updated, even on less powerful CI VMs. MK.
    (Thread/sleep 150)
    (is (= (sort (ids-from created-rels))
           (sort (ids-from (relationships/outgoing-for from-node :types [:links])))))))

(deftest test-listing-all-relationships-on-a-node-that-doesnt-have-any
  (let [node   (nodes/create)
        result (relationships/all-for node)]
    (is (empty? result))))

(deftest test-listing-all-relationships-on-a-node-that-has-3-relationships
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :links)
        _      (relationships/create node (nodes/create) :links)
        _      (relationships/create node (nodes/create) :links)
        result (relationships/all-for node)]
    (is (= 3 (count result)))))

(deftest test-listing-all-relationships-of-specific-kind
  (let [node   (nodes/create)
        rel1   (relationships/create node (nodes/create) :likes)
        rel2   (relationships/create node (nodes/create) :links)
        rel3   (relationships/create node (nodes/create) :follows)
        result (relationships/all-for node :types [:follows :likes])]
    (is (= 2 (count result)))
    (is (= (sort [(:id rel1) (:id rel3)]) (vec (sort (map :id result)))))))

(deftest test-listing-incoming-relationships-on-a-node-that-doesnt-have-any
  (let [node   (nodes/create)
        result (relationships/incoming-for node)]
    (is (empty? result))))

(deftest test-listing-incoming-relationships-on-a-node-that-has-2-incoming-relationships
  (let [node   (nodes/create)
        _      (relationships/create (nodes/create) node :friend)
        _      (relationships/create (nodes/create) node :relative)
        result (relationships/incoming-for node :types [:friend])]
    (is (= 1 (count result)))))

(deftest test-listing-incoming-relationships-of-specific-kind
  (let [node   (nodes/create)
        _      (relationships/create (nodes/create) node :links)
        _      (relationships/create (nodes/create) node :links)
        result (relationships/incoming-for node)]
    (is (= 2 (count result)))))

(deftest test-listing-outgoing-relationships-on-a-node-that-doesnt-have-any
  (let [node   (nodes/create)
        result (relationships/outgoing-for node)]
    (is (empty? result))))

(deftest test-listing-outgoing-relationships-on-a-node-that-has-1-outgoing-relationship
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :links)
        result (relationships/outgoing-for node)]
    (is (= 1 (count result)))))

(deftest test-listing-outgoing-relationships-of-specific-kind
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :friend)
        _      (relationships/create node (nodes/create) :relative)
        result (relationships/outgoing-for node :types [:relative])]
    (is (= 1 (count result)))))


(deftest test-listing-of-relationship-types
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :friend)
        _      (relationships/create node (nodes/create) :relative)]
    (is (subset? #{"friend" "relative"} (set (relationships/all-types))))))


(deftest test-updating-relationship-properties
  (let [data         {:one "uno" :two "due"}
        from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links data)
        new-data     (relationships/update (:id created-rel) {:one "eine" :two "deux"})
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= new-data (:data fetched-rel)))))


(deftest test-deleting-a-specific-relationship-property
  (let [data         {:cost "high" :legendary true}
        from-node    (nodes/create {:name "Romeo"})
        to-node      (nodes/create {:name "Juliet"})
        created-rel  (relationships/create from-node to-node :loves data)
        _            (relationships/delete-property (:id created-rel) :cost)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= {:legendary true} (:data fetched-rel)))))


(deftest test-deleting-a-non-existent-relationship-property
  (let [data         {:cost "high" :legendary true}
        from-node    (nodes/create {:name "Romeo"})
        to-node      (nodes/create {:name "Juliet"})
        created-rel  (relationships/create from-node to-node :loves data)]
    (is (thrown? Exception
                 (relationships/delete-property (:id created-rel) :a-non-existent-rel-property)))))


(deftest test-deleting-a-property-on-non-existent-relationship
  (is (thrown? Exception
               (relationships/delete-property 8283787287 :a-non-existent-rel-property))))


;;
;; More sophisticated examples
;;

(deftest test-deletion-of-nodes-with-relationships
  (let [john (nodes/create {:name "John" :age 28 :location "New York City, NY"})
        beth (nodes/create {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        gael (nodes/create {:name "Gaël"      :age 31 :location "Montpellier"})
        rel1 (relationships/create john beth :knows)
        rel5 (relationships/create beth gael :knows)
        rt   {:type "knows" :direction "out"}]
    (is (paths/exists-between? (:id john) (:id gael) :relationships [rt] :max-depth 3))
    (is (set (nodes/all-connected-out (:id john))) (:id beth))
    (is (nodes/connected-out? (:id john) (:id beth)))
    (is (not (nodes/connected-out? (:id john) (:id gael))))
    (is (set (nodes/all-connected-out (:id beth))) (:id gael))
    (is (nodes/connected-out? (:id beth) (:id gael)))
    ;; deletion of a node affects reachability of two other nodes. MK.
    (is (thrown? Exception
                 (nodes/delete (:id beth))))
    (relationships/purge-all beth)
    (nodes/delete (:id beth))
    (is (not (paths/exists-between? (:id john) (:id gael) :relationships [rt] :max-depth 3)))))

(deftest test-purging-of-all-outgoing-relationships
  (let [john (nodes/create {:name "John" :age 28 :location "New York City, NY"})
        beth (nodes/create {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        gael (nodes/create {:name "Gaël"      :age 31 :location "Montpellier"})
        rel1 (relationships/create john beth :knows)
        rel5 (relationships/create beth gael :knows)
        rt   {:type "knows" :direction "out"}]
    (is (paths/exists-between? (:id john) (:id gael) :relationships [rt] :max-depth 3))
    (relationships/purge-outgoing beth)
    (is (not (paths/exists-between? (:id john) (:id gael) :relationships [rt] :max-depth 3)))))
