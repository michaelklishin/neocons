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

(deftest test-accessing-a-non-existing-node
  (neorest/connect! "http://localhost:7474/db/data/")
  (is (nil? (nodes/find 928398827))))
