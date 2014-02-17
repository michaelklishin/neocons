;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.indexing-test
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as rels]
            [clojure.test :refer :all]))

(neorest/connect! "http://localhost:7474/db/data/")

;;
;; Indexes
;;

(deftest ^{:indexing true} test-create-a-new-node-index-with-default-configuration
  (let [name "node-index-1-default-configuration"]
    (nodes/create-index name)))

(deftest ^{:indexing true} test-create-a-new-rel-index-with-default-configuration
  (let [name "rel-index-1-default-configuration"]
    (rels/create-index name)))

(deftest ^{:indexing true} test-create-a-new-node-index-with-explicit-configuration
  (let [name "test-create-a-new-node-index-with-explicit-configuration-node-index-1"
        conf {:type "fulltext" :provider "lucene"}]
    (nodes/create-index name conf)
    (nodes/delete-index name)))

(deftest ^{:indexing true} test-create-a-new-unique-node-index-with-explicit-configuration
  (let [name "node-index-2b"
        conf {:unique true}]
    (nodes/create-index name conf)
    (nodes/delete-index name)))

(deftest ^{:indexing true} test-create-a-new-rel-index-with-explicit-configuration
  (let [name "rel-index-2"
        conf {:type "fulltext" :provider "lucene"}]
    (rels/create-index name conf)))

(deftest ^{:indexing true} test-create-a-new-unique-rel-index-with-explicit-configuration
  (let [name "rel-index-2b"
        conf {:unique true :provider "lucene" :type "fulltext"}]
    (rels/create-index name conf)))

(deftest ^{:indexing true} test-listing-node-indexes
  (let [name "node-index-3"
        idx  (nodes/create-index name)
        list (nodes/all-indexes)]
    (is (some (fn [i]
                (= name (:name i))) list))))

(deftest ^{:indexing true} test-listing-rel-indexes
  (let [name "rel-index-3"
        idx  (rels/create-index name)
        list (rels/all-indexes)]
    (is (some (fn [i]
                (= name (:name i))) list))))

(deftest ^{:indexing true} test-creating-and-immediately-deleting-a-node-index
  (let [name "node-index-4-default-configuration"
        idx  (nodes/create-index name)]
    (is (= name (:name idx)))
    (nodes/delete-index name)))

(deftest ^{:indexing true} test-creating-and-immediately-deleting-a-rel-index
  (let [name "rel-index-4-default-configuration"
        idx  (rels/create-index name)]
    (is (= name (:name idx)))
    (rels/delete-index name)))


(deftest ^{:indexing true} test-adding-a-node-to-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)))

(deftest ^{:indexing true} test-adding-a-node-to-index-with-value-with-spaces
  (let [idx  (nodes/create-index "things")
        s    "a value with spaces"
        k    "a key with spaces"
        n    (nodes/create {:value s})
        _    (nodes/add-to-index (:id n) (:name idx) k s)
        n'   (nodes/find-one (:name idx) k s)]
    (is (= s (-> n' :data :value)))))

(deftest ^{:indexing true} test-adding-a-node-to-index-with-value-with-colons
  (let [idx  (nodes/create-index "things")
        s    "a:value with spaces"
        k    "a:key with spaces"
        n    (nodes/create {:value s})
        _    (nodes/add-to-index (:id n) (:name idx) k s)
        n'   (nodes/find-one (:name idx) k s)]
    (is (= s (-> n' :data :value)))))

(deftest ^{:indexing true} test-adding-a-node-to-index-as-unique
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index home (:name idx) "uri" uri true)
    (nodes/add-to-index home (:name idx) "uri" uri true)))

(deftest ^{:indexing true} test-adding-a-rel-to-index
  (let [idx   (rels/create-index "uris")
        uri1  "http://arstechnica.com"
        page1 (nodes/create {:uri uri1})
        uri2  "http://apple.com/ipad"
        page2 (nodes/create {:uri uri2})
        rel   (rels/create page1 page2 :links)]
    (rels/add-to-index (:id rel) (:name idx) "active" "true")))

(deftest ^{:indexing true} test-adding-a-rel-to-index-as-unique
  (let [idx   (rels/create-index "uris")
        uri1  "http://arstechnica.com"
        page1 (nodes/create {:uri uri1})
        uri2  "http://apple.com/ipad"
        page2 (nodes/create {:uri uri2})
        rel   (rels/create page1 page2 :links)]
    (rels/add-to-index rel (:name idx) "active" "true" true)))

(deftest ^{:indexing true} test-removing-a-node-from-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx))))

(deftest ^{:indexing true} test-removing-a-rel-from-index
  (let [idx   (rels/create-index "uris")
        uri1  "http://arstechnica.com"
        page1 (nodes/create {:uri uri1})
        uri2  "http://apple.com/ipad"
        page2 (nodes/create {:uri uri2})
        rel   (rels/create page1 page2 :links)]
    (rels/add-to-index (:id rel) (:name idx) "active" "true")
    (rels/delete-from-index (:id rel) (:name idx))))

(deftest ^{:indexing true} test-removing-a-node-and-key-from-index
  (let [idx  (nodes/create-index "uris, urls and so on")
        uri  "http://arstechnica.com"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx) "uri")))

(deftest ^{:indexing true} test-removing-a-rel-and-key-from-index
  (let [idx   (rels/create-index "uris")
        uri1  "http://arstechnica.com"
        page1 (nodes/create {:uri uri1})
        uri2  "http://apple.com/ipad"
        page2 (nodes/create {:uri uri2})
        rel   (rels/create page1 page2 :links)]
    (rels/add-to-index (:id rel) (:name idx) "active" "true")
    (rels/delete-from-index (:id rel) (:name idx) "active")))

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
    (let [ids (set (map :id (nodes/find (:name idx) "url" url1)))]
      (is (ids (:id node1)))
      (is (not (ids (:id node2)))))))

(deftest ^{:indexing true} test-finding-rels-using-an-index
  (let [node1 (nodes/create {:name "Wired" :url "http://wired.com"})
        url   "http://craigslist.org"
        node2 (nodes/create {:name "Craigslist" :url url})
        idx   (rels/create-index "by-target-url")
        rel   (rels/create node1 node2 :links {:url url})]
    (rels/delete-from-index (:id rel) (:name idx) "target-url")
    (rels/add-to-index (:id rel) (:name idx) "target-url" url)
    (let [ids (set (map :id (rels/find (:name idx) "target-url" url)))]
      (is (ids (:id rel))))))

(deftest ^{:indexing true} test-finding-a-node-with-url-unsafe-key-to-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com/search/?query=Diablo+III"
        home (nodes/create {:uri uri})]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/find-one (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx))))

(deftest ^{:indexing true} test-removing-a-node-removes-it-from-indexes
  (try
    (nodes/create-index "by-url")
    (catch Exception e
      (comment We need to make sure we wipe previously created index with a different configuration, ignore cases when it simply does not exist)))
  (let [node1 (nodes/create {:name "Wired"})
        url1  "http://wired.com"
        idx   (nodes/create-index "by-url")]
    (nodes/delete-from-index (:id node1) (:name idx) "url")
    (nodes/add-to-index (:id node1) (:name idx) "url" url1)
    (nodes/delete (:id node1))
    (let [ids (set (map :id (nodes/find (:name idx) "url" url1)))]
      (is (not (ids (:id node1)))))))

(deftest ^{:indexing true} test-finding-nodes-using-full-text-search-queries-over-index
  (try
    (nodes/delete-index "companies")
    (catch Exception e
      (comment We need to make sure we wipe previously created index with a different configuration, ignore cases when it simply does not exist)))
  (let [puma  (nodes/create {:name "Puma"  :hq-location "Herzogenaurach, Germany"})
        apple (nodes/create {:name "Apple" :hq-location "Cupertino, CA, USA"})
        idx   (nodes/create-index "companies" {:type :fulltext})]
    (nodes/delete-from-index (:id puma)  (:name idx))
    (nodes/delete-from-index (:id apple) (:name idx))
    (nodes/add-to-index (:id puma)  (:name idx) "country" "Germany")
    (nodes/add-to-index (:id apple) (:name idx) "country" "United States of America")
    (let [ids (set (map :id (nodes/query (:name idx) "country:Germany")))]
      (is (ids (:id puma)))
      (is (not (ids (:id apple)))))
    (nodes/delete-index "companies")))


(deftest ^{:indexing true} test-finding-nodes-using-full-text-search-queries-over-index-example2
  (try
    (nodes/delete-index "companies")
    (catch Exception e
      (comment We need to make sure we wipe previously created index with a different configuration, ignore cases when it simply does not exist)))         
  (let [neocons (nodes/create {:name "Neocons"  :description "Neocons is an idiomatic Clojure client for the Neo4J Server REST interface"})
        monger  (nodes/create {:name "Monger" :description "Monger is a Clojure MongoDB driver for a more civilized age"})
        idx     (nodes/create-index "companies" {:type :fulltext :provider "lucene"})]
    (nodes/delete-from-index (:id neocons)  (:name idx))
    (nodes/delete-from-index (:id monger) (:name idx))
    (nodes/add-to-index (:id neocons) (:name idx) "description" "Neocons is an idiomatic Clojure client for the Neo4J Server REST interface")
    (nodes/add-to-index (:id monger)  (:name idx) "description" "Monger is a Clojure MongoDB driver for a more civilized age")
    (let [ids (set (map :id (nodes/query (:name idx) "description:*civilized*")))]
      (is (ids (:id monger)))
      (is (not (ids (:id neocons)))))
    (nodes/delete-index "companies")))
