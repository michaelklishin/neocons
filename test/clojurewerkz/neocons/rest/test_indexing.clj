(ns clojurewerkz.neocons.rest.test-indexing
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")

;;
;; Indexes
;;

(deftest ^{:indexing true} test-create-a-new-node-index-with-default-configuration
  (let [name "node-index-1-default-configuration"]
    (nodes/create-index name)))

(deftest ^{:indexing true} test-create-a-new-node-index-with-explicit-configuration
  (let [name "node-index-2"
        conf {:type "fulltext" :provider "lucene"}]
    (nodes/create-index name conf)))

(deftest ^{:indexing true} test-listing-node-indexes
  (let [name "node-index-3"
        idx  (nodes/create-index name)
        list (nodes/all-indexes)]
    (is (some (fn [i]
                (= name (:name i))) list))))

(deftest ^{:indexing true} test-creating-and-immediately-deleting-a-node-index
  (let [name "node-index-4-default-configuration"
        idx  (nodes/create-index name)]
    (is (= name (:name idx)))
    (nodes/delete-index name)))


(deftest ^{:indexing true} test-adding-a-node-to-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)))

(deftest ^{:indexing true} test-removing-a-node-from-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx))))

(deftest ^{:indexing true} test-removing-a-node-and-key-from-index
  (let [idx  (nodes/create-index "uris, urls and so on")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx) "uri")))

(deftest ^{:indexing true} test-removing-a-node-key-and-value-from-index
  (let [idx  (nodes/create-index "locations")
        home (nodes/create {:lat 20.0})]
    (nodes/add-to-index (:id home) (:name idx) "lat" 20.0)
    (nodes/delete-from-index (:id home) (:name idx) "lat" 20.0)))

(deftest ^{:indexing true} test-finding-nodes-using-an-index
  (let [node1 (nodes/create {:name "Wired"})
        node2 (nodes/create {:name "Craigslist"})
        url1  "http://wired.com"
        url2  "http://craigslist.org"
        idx   (nodes/create-index "by-url")]
    (nodes/delete-from-index (:id node1) (:name idx) "url")
    (nodes/delete-from-index (:id node2) (:name idx) "url")
    (nodes/add-to-index (:id node1) (:name idx) "url" url1)
    (nodes/add-to-index (:id node2) (:name idx) "url" url2)
    (let [ids (set (map :id (nodes/find (:name idx) :url url1)))]
      (is (ids (:id node1)))
      (is (not (ids (:id node2)))))))

(deftest ^{:indexing true} test-removing-a-node-removes-it-from-indexes
  (let [node1 (nodes/create {:name "Wired"})
        url1  "http://wired.com"
        idx   (nodes/create-index "by-url")]
    (nodes/delete-from-index (:id node1) (:name idx) "url")
    (nodes/add-to-index (:id node1) (:name idx) "url" url1)
    (nodes/delete (:id node1))
    (let [ids (set (map :id (nodes/find (:name idx) :url url1)))]
      (is (not (ids (:id node1)))))))

(deftest ^{:indexing true} test-finding-nodes-using-full-text-search-queries-over-index
  (let [puma  (nodes/create {:name "Puma"  :hq-location "Herzogenaurach, Germany"})
        apple (nodes/create {:name "Apple" :hq-location "Cupertino, CA, USA"})
        idx   (nodes/create-index "companies")]
    (nodes/delete-from-index (:id puma)  (:name idx))
    (nodes/delete-from-index (:id apple) (:name idx))
    (nodes/add-to-index (:id puma)  (:name idx) "country" "Germany")
    (nodes/add-to-index (:id apple) (:name idx) "country" "United States of America")
    (let [ids (set (map :id (nodes/query (:name idx) "country:Germany")))]
      (is (ids (:id puma)))
      (is (not (ids (:id apple)))))))
