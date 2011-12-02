(ns clojurewerkz.neocons.test.rest
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [slingshot.slingshot :as slingshot])
  (:import [slingshot Stone])
  (:use [clojure.test]))


(neorest/connect! "http://localhost:7474/db/data/")


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
  (let [created-node (nodes/create)
        fetched-node (nodes/get (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))))

(deftest test-creating-and-immediately-accessing-a-node-with-properties
  (let [data         { :key "value" }
        created-node (nodes/create data)
        fetched-node (nodes/get (:id created-node))]
    (is (= (:id created-node) (:id fetched-node)))
    (is (= (:data created-node) data))))


(deftest test-accessing-a-non-existent-node
  (is (thrown? Exception
               (nodes/get 928398827))))


(deftest test-creating-and-deleting-a-node-with-properties
  (let [data         { :key "value" }
        created-node (nodes/create data)
        [deleted-id status] (nodes/delete (:id created-node))]
    (is (= 204 status))
    (is (= (:id created-node) deleted-id))))

(deftest test-attempting-to-delete-a-non-existent-node
  (is (thrown? Exception
               (nodes/delete 237737737))))


(deftest test-creating-and-getting-properties-of-one-node
  (let [data         { :key "value" }
        created-node (nodes/create data)
        fetched-data (nodes/get-properties (:id created-node))]
    (is (= data fetched-data))))

(deftest test-creating-and-getting-empty-properties-of-one-node
  (let [created-node (nodes/create)
        fetched-data (nodes/get-properties (:id created-node))]
    (is (= {} fetched-data))))


(deftest test-updating-a-single-node-property
  (let [node         (nodes/create { :age 26 })
        fetched-node (nodes/get (:id node))
        new-value    (nodes/set-property (:id node) :age 27)
        updated-node (nodes/get (:id fetched-node))]
    (is (= new-value (-> updated-node :data :age)))))


(deftest test-updating-node-properties
  (let [node         (nodes/create { :age 26 })
        fetched-node (nodes/get (:id node))
        new-data    (nodes/update (:id node) { :age 27 :gender "male" })
        updated-node (nodes/get (:id fetched-node))]
    (is (= new-data (-> updated-node :data)))))


(deftest test-deleting-all-properties-from-a-node
  (let [data         { :key "value" }
        created-node (nodes/create data)
        fetched-data (nodes/get-properties (:id created-node))]
    (is (= data fetched-data))
    (nodes/delete-properties (:id created-node))
    (is (= {} (nodes/get-properties (:id created-node))))))



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

(deftest test-creating-and-immediately-accessing-a-relationship-with-properties
  (let [data         { :one "uno" :two "due" }
        from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links data)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= (:data created-rel) (:data fetched-rel)))))

(deftest test-creating-and-deleting-a-relationship-without-properties
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        [deleted-id status] (relationships/delete (:id created-rel))]
    (is (= 204 status))))

(deftest test-creating-and-deleting-a-non-existent-relationship
  (is (thrown? slingshot.Stone
               (relationships/delete 87238467666))))

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
        _      (relationships/create node (nodes/create) :likes)
        _      (relationships/create node (nodes/create) :links)
        _      (relationships/create node (nodes/create) :follows)
        result (relationships/all-for node :types [:follows :likes])]
    (is (= 2 (count result)))))

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
  (is (= (sort ["links" "likes" "follows" "friend" "relative" "loves"]) (sort (relationships/all-types)))))


(deftest test-updating-relationship-properties
  (let [data         { :one "uno" :two "due" }
        from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links data)
        new-data     (relationships/update (:id created-rel) { :one "eine" :two "deux" })
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= new-data (:data fetched-rel)))))


(deftest test-deleting-a-specific-relationship-property
  (let [data         { :cost "high" :legendary true }
        from-node    (nodes/create { :name "Romeo" })
        to-node      (nodes/create { :name "Juliet" })
        created-rel  (relationships/create from-node to-node :loves data)
        _            (relationships/delete-property (:id created-rel) :cost)
        fetched-rel  (relationships/get (:id created-rel))]
    (is (= (:id created-rel) (:id fetched-rel)))
    (is (= (:type created-rel) (:type fetched-rel)))
    (is (= { :legendary true } (:data fetched-rel)))))


(deftest test-deleting-a-non-existent-relationship-property
  (let [data         { :cost "high" :legendary true }
        from-node    (nodes/create { :name "Romeo" })
        to-node      (nodes/create { :name "Juliet" })
        created-rel  (relationships/create from-node to-node :loves data)]
    (is (thrown? Exception
                 (relationships/delete-property (:id created-rel) :a-non-existent-rel-property)))))


(deftest test-deleting-a-property-on-non-existent-relationship
  (is (thrown? Exception
               (relationships/delete-property 8283787287 :a-non-existent-rel-property))))


;;
;; Indexes
;;

(deftest test-create-a-new-node-index-with-default-configuration
  (let [name "node-index-1-default-configuration"]
    (nodes/create-index name)))

(deftest test-create-a-new-node-index-with-explicit-configuration
  (let [name "node-index-2"
        conf { :type "fulltext" :provider "lucene" }]
    (nodes/create-index name conf)))

(deftest test-listing-node-indexes
  (let [name "node-index-3"
        idx  (nodes/create-index name)
        list (nodes/all-indexes)]
    (is (some (fn [i]
                (= name (:name i))) list))))

(deftest test-creating-and-immediately-deleting-a-node-index
  (let [name "node-index-4-default-configuration"
        idx  (nodes/create-index name)]
    (is (= name (:name idx)))
    (nodes/delete-index name)))


(deftest test-adding-a-node-to-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create { :uri uri })]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)))

(deftest test-remove-a-node-from-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create { :uri uri })]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx))))

(deftest test-remove-a-node-and-key-from-index
  (let [idx  (nodes/create-index "uris, urls and so on")
        uri  "http://arstechnica.com"
        home (nodes/create { :uri uri })]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx) "uri")))

(deftest test-remove-a-node-key-and-value-from-index
  (let [idx  (nodes/create-index "locations")
        home (nodes/create { :lat 20.0 })]
    (nodes/add-to-index (:id home) (:name idx) "lat" 20.0)
    (nodes/delete-from-index (:id home) (:name idx) "lat" 20.0)))

(deftest test-finding-nodes-using-an-index
  (let [node1 (nodes/create { :name "Wired" })
        node2 (nodes/create { :name "Craigslist" })
        url1  "http://wired.com"
        url2  "http://craigslist.org"
        idx   (nodes/create-index "by-url")]
    (nodes/delete-from-index (:id node1) (:name idx) "url")
    (nodes/delete-from-index (:id node2) (:name idx) "url")
    (nodes/add-to-index (:id node1) (:name idx) "url" url1)
    (nodes/add-to-index (:id node2) (:name idx) "url" url2)
    (let [ids (map :id (nodes/find (:name idx) :url url1))]
      (is (some (fn [id]
                  (= id (:id node1))) ids)))))
