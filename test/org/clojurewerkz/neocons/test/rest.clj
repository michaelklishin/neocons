(ns org.clojurewerkz.neocons.test.rest
  (:require [org.clojurewerkz.neocons.rest               :as neorest]
            [org.clojurewerkz.neocons.rest.nodes         :as nodes]
            [org.clojurewerkz.neocons.rest.relationships :as relationships])
  (:use [clojure.test]))


;;
;; Connections/Discovery
;;

(deftest test-connection-and-discovery-using-connect-with-string-uri
  (let [endpoint (neorest/connect "http://localhost:7474/db/data/")]
    (is (:version                endpoint))
    (is (:node-uri               endpoint))
    (is (:batch-uri              endpoint))
    (is (:relationship-types-uri endpoint))))


(deftest test-connection-and-discovery-using-connect!-with-string-uri
  (neorest/connect! "http://localhost:7474/db/data/")
  (is (:version                neorest/*endpoint*))
  (is (:node-uri               neorest/*endpoint*))
  (is (:batch-uri              neorest/*endpoint*))
  (is (:relationship-types-uri neorest/*endpoint*)))



;;
;; Working with nodes
;;

(deftest test-creating-and-immediately-accessing-a-node-without-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [created-node (nodes/create)
        fetched-node (nodes/get (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))))

(deftest test-creating-and-immediately-accessing-a-node-with-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [data         { :key "value" }
        created-node (nodes/create :data data)
        fetched-node (nodes/get (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))
    (is (= (:data created-node) data))))


(deftest test-accessing-a-non-existing-node
  (neorest/connect! "http://localhost:7474/db/data/")
  (is (nil? (nodes/get 928398827))))


(deftest test-creating-and-deleting-a-node-with-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [data         { :key "value" }
        created-node        (nodes/create :data data)
        [deleted-id status] (nodes/delete (:id created-node))]
    (is (= (:id created-node) deleted-id))
    (is (nil? (nodes/get deleted-id)))
    (is (= 204 status))))

(deftest test-attempting-to-delete-a-non-existing-node
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [[deleted-id status] (nodes/delete 237737737)]
    (is (nil? deleted-id))
    (is (= 404 status))))


;;
;; Working with relationships
;;

(deftest test-creating-and-immediately-accessing-a-relationship-without-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))))

(deftest test-creating-and-immediately-accessing-a-relationship-with-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [data         { :one "uno" :two "due" }
        from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links :data data)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= (:data created-rel) (:data fetched-rel)))))

(deftest test-creating-and-deleting-a-relationship-without-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        [deleted-id status] (relationships/delete (:id created-rel))]
    (is (= (:id created-rel) deleted-id))
    (is (= 204 status))))

(deftest test-creating-and-deleting-a-non-existing-relationship
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [[deleted-id status] (relationships/delete 87238467666)]
    (is (nil? deleted-id))
    (is (= 404 status))))

(deftest test-listing-all-relationships-on-a-node-that-doesnt-have-any
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        result (relationships/all-for node)]
    (is (empty? result))))

(deftest test-listing-all-relationships-on-a-node-that-has-3-relationships
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :links)
        _      (relationships/create node (nodes/create) :links)
        _      (relationships/create node (nodes/create) :links)
        result (relationships/all-for node)]
    (is (= 3 (count result)))))

(deftest test-listing-all-relationships-of-specific-kind-on-a-node
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :likes)
        _      (relationships/create node (nodes/create) :links)
        _      (relationships/create node (nodes/create) :follows)
        result (relationships/all-for node :types [:follows :likes])]
    (is (= 2 (count result)))))

(deftest test-listing-incoming-relationships-on-a-node-that-doesnt-have-any
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        result (relationships/incoming-for node)]
    (is (empty? result))))

(deftest test-listing-incoming-relationships-on-a-node-that-has-2-incoming-relationships
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        _      (relationships/create (nodes/create) node :links)
        _      (relationships/create (nodes/create) node :links)
        result (relationships/incoming-for node)]
    (is (= 2 (count result)))))

(deftest test-listing-outgoing-relationships-on-a-node-that-doesnt-have-any
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        result (relationships/outgoing-for node)]
    (is (empty? result))))

(deftest test-listing-outgoing-relationships-on-a-node-that-has-1-outgoing-relationship
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [node   (nodes/create)
        _      (relationships/create node (nodes/create) :links)
        result (relationships/all-for node)]
    (is (= 1 (count result)))))