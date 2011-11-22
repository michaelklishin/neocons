(ns org.clojurewerkz.neocons.test.rest
  (:require [org.clojurewerkz.neocons.rest       :as neorest]
            [org.clojurewerkz.neocons.rest.nodes :as nodes])
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
        fetched-node (nodes/find (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))))

(deftest test-creating-and-immediately-accessing-a-node-with-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [data         { :key "value" }
        created-node (nodes/create :data data)
        fetched-node (nodes/find (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))
    (is (= (:data created-node) data))))


(deftest test-accessing-a-non-existing-node
  (neorest/connect! "http://localhost:7474/db/data/")
  (is (nil? (nodes/find 928398827))))


(deftest test-creating-and-deleting-a-node-with-properties
  (neorest/connect! "http://localhost:7474/db/data/")
  (let [data         { :key "value" }
        created-node (nodes/create :data data)
        deleted-id   (nodes/delete (:id created-node))]
    (is (= (:id created-node) deleted-id))
    (is (nil? (nodes/find deleted-id)))))
