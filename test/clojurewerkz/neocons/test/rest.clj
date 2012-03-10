(ns clojurewerkz.neocons.test.rest
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nodes]
            [clojurewerkz.neocons.rest.relationships :as relationships]
            [clojurewerkz.neocons.rest.paths         :as paths]
            [clojurewerkz.neocons.rest.cypher        :as cypher]
            [slingshot.slingshot :as slingshot])
  (:import [slingshot ExceptionInfo])
  (:use [clojure.test]
        [clojure.set :only [subset?]]
        [clojure.pprint :only [pprint]]
        [clojurewerkz.neocons.rest.records :only [instantiate-node-from instantiate-rel-from instantiate-path-from]]))

(println (str "Using Clojure version " *clojure-version*))
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

(deftest test-avoiding-destroying-the-same-relationship-without-properties-twice
  (let [from-node    (nodes/create)
        to-node      (nodes/create)
        created-rel  (relationships/create from-node to-node :links)
        rt           { :type "links" :direction "out" }]
    (is (relationships/first-outgoing-between from-node to-node [:links]))
    (is (nil? (relationships/first-outgoing-between from-node to-node [:loves])))
    (is (= created-rel (relationships/first-outgoing-between from-node to-node [:links])))
    (is (paths/exists-between? (:id from-node) (:id to-node) :relationships [rt] :max-depth 1))
    (relationships/maybe-delete-outgoing from-node to-node [:links])
    (is (nil? (relationships/first-outgoing-between from-node to-node [:links])))
    (is (not (paths/exists-between? (:id from-node) (:id to-node) :relationships [rt] :max-depth 1)))
    (relationships/maybe-delete-outgoing from-node to-node [:links])))

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
  ;; this should be slingshot.ExceptionInfo on 1.3 but
  ;; clojure.lang.ExceptionInfo on 1.4.0[-beta1]. This Slingshot shit is annoying. MK.
  (is (thrown? Exception
               (relationships/delete 87238467666)))
  (try
    (relationships/delete 87238467666)
    (catch Exception e
      (let [d (.getData e)]
        (is (= (-> d :object :status) 404))))))

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
  (is (subset? #{"links" "likes" "follows" "friend" "relative" "loves"} (set (relationships/all-types)))))


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

(deftest ^{:indexing true} test-create-a-new-node-index-with-default-configuration
  (let [name "node-index-1-default-configuration"]
    (nodes/create-index name)))

(deftest ^{:indexing true} test-create-a-new-node-index-with-explicit-configuration
  (let [name "node-index-2"
        conf { :type "fulltext" :provider "lucene" }]
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
        home (nodes/create { :uri uri })]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)))

(deftest ^{:indexing true} test-removing-a-node-from-index
  (let [idx  (nodes/create-index "uris")
        uri  "http://arstechnica.com"
        home (nodes/create { :uri uri })]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx))))

(deftest ^{:indexing true} test-removing-a-node-and-key-from-index
  (let [idx  (nodes/create-index "uris, urls and so on")
        uri  "http://arstechnica.com"
        home (nodes/create { :uri uri })]
    (nodes/add-to-index (:id home) (:name idx) "uri" uri)
    (nodes/delete-from-index (:id home) (:name idx) "uri")))

(deftest ^{:indexing true} test-removing-a-node-key-and-value-from-index
  (let [idx  (nodes/create-index "locations")
        home (nodes/create { :lat 20.0 })]
    (nodes/add-to-index (:id home) (:name idx) "lat" 20.0)
    (nodes/delete-from-index (:id home) (:name idx) "lat" 20.0)))

(deftest ^{:indexing true} test-finding-nodes-using-an-index
  (let [node1 (nodes/create { :name "Wired" })
        node2 (nodes/create { :name "Craigslist" })
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
  (let [node1 (nodes/create { :name "Wired" })
        url1  "http://wired.com"
        idx   (nodes/create-index "by-url")]
    (nodes/delete-from-index (:id node1) (:name idx) "url")
    (nodes/add-to-index (:id node1) (:name idx) "url" url1)
    (nodes/delete (:id node1))
    (let [ids (set (map :id (nodes/find (:name idx) :url url1)))]
      (is (not (ids (:id node1)))))))

(deftest ^{:indexing true} test-finding-nodes-using-full-text-search-queries-over-index
  (let [puma  (nodes/create { :name "Puma"  :hq-location "Herzogenaurach, Germany"})
        apple (nodes/create { :name "Apple" :hq-location "Cupertino, CA, USA"})
        idx   (nodes/create-index "companies")]
    (nodes/delete-from-index (:id puma)  (:name idx))
    (nodes/delete-from-index (:id apple) (:name idx))
    (nodes/add-to-index (:id puma)  (:name idx) "country" "Germany")
    (nodes/add-to-index (:id apple) (:name idx) "country" "United States of America")
    (let [ids (set (map :id (nodes/query (:name idx) "country:Germany")))]
      (is (ids (:id puma)))
      (is (not (ids (:id apple)))))))



;;
;; Traversal
;;

(deftest test-traversing-nodes-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create { :name "John" })
        adam (nodes/create { :name "Alan" })
        pete (nodes/create { :name "Peter" })
        _    (relationships/create john adam :friend)
        _    (relationships/create john pete :friend)
        _    (relationships/create adam pete :friend)
        xs1  (nodes/traverse (:id john) :relationships [{ :direction "all" :type "friend" }])
        ids1 (vec (map :id xs1))
        xs2  (nodes/traverse (:id john) :relationships [{ :direction "all" :type "enemy" }])
        ids2 (map :id xs2)]
    (is (= [(:id john) (:id adam) (:id pete)] ids1))
    (is (= [(:id john)] ids2))))


(deftest test-traversing-relationships-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create { :name "John" })
        adam (nodes/create { :name "Alan" })
        pete (nodes/create { :name "Peter" })
        rel1 (relationships/create john adam :friend)
        rel2 (relationships/create john pete :friend)
        rel3 (relationships/create adam pete :friend)
        xs1  (relationships/traverse (:id john) :relationships [{ :direction "all" :type "friend" }])
        ids1 (vec (map :id xs1))
        xs2  (relationships/traverse (:id john) :relationships [{ :direction "all" :type "enemy" }])
        ids2 (map :id xs2)]
    (is (= [(:id rel1) (:id rel2)] ids1))
    (is (empty? ids2))))


(deftest test-traversing-nodes-using-all-but-start-node-return-filter-out-relationships-and-no-pagination
  (let [john (nodes/create { :name "John" })
        adam (nodes/create { :name "Alan" })
        pete (nodes/create { :name "Peter" })
        _    (relationships/create john adam :friend)
        _    (relationships/create adam pete :friend)
        xs1  (nodes/traverse (:id john) :relationships [{ :direction "out" :type "friend" }] :return-filter { :language "builtin" :name "all_but_start_node" })
        ids1 (vec (map :id xs1))
        xs2  (nodes/traverse (:id adam) :relationships [{ :direction "out" :type "friend" }] :return-filter { :language "builtin" :name "all_but_start_node" })
        ids2 (vec (map :id xs2))]
    (is (= [(:id adam) (:id pete)] ids1))
    (is (= [(:id pete)] ids2))))


(deftest test-traversing-nodes-using-all-but-start-node-return-filter-in-relationships-and-no-pagination
  (let [john (nodes/create { :name "John" })
        adam (nodes/create { :name "Alan" })
        pete (nodes/create { :name "Peter" })
        _    (relationships/create john adam :friend)
        _    (relationships/create adam pete :friend)
        xs   (nodes/traverse (:id john) :relationships [{ :direction "in" :type "friend" }] :return-filter { :language "builtin" :name "all_but_start_node" })
        ids  (vec (map :id xs))]
    (is (empty? ids))))


(deftest test-traversing-paths-using-all-return-filter-all-relationships-and-no-pagination
  (let [john (nodes/create { :name "John" })
        adam (nodes/create { :name "Alan" })
        pete (nodes/create { :name "Peter" })
        rel1 (relationships/create john adam :friend)
        rel2 (relationships/create adam pete :friend)
        xs1  (paths/traverse (:id john) :relationships [{ :direction "all" :type "friend" }])
        xs2  (paths/traverse (:id john) :relationships [{ :direction "all" :type "enemy" }])]
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

(deftest test-shortest-path-algorithm-1
  (let [john (nodes/create { :name "John" :age 28 :location "New York City, NY" })
        liz  (nodes/create { :name "Liz"  :age 27 :location "Buffalo, NY" })
        beth (nodes/create { :name "Elizabeth" :age 30 :location "Chicago, IL" })
        bern (nodes/create { :name "Bernard"   :age 33 :location "Zürich" })
        gael (nodes/create { :name "Gaël"      :age 31 :location "Montpellier" })
        alex (nodes/create { :name "Alex"      :age 24 :location "Toronto, ON" })
        rel1 (relationships/create john liz  :knows)
        rel2 (relationships/create liz  beth :knows)
        rel3 (relationships/create liz  bern :knows)
        rel4 (relationships/create bern gael :knows)
        rel5 (relationships/create gael beth :knows)
        rel6 (relationships/create beth gael :knows)
        rel7 (relationships/create john gael :knows)
        rt   { :type "knows" :direction "out" }
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


;;
;; More sophisticated examples
;;

(deftest test-deletion-of-nodes-with-relationships
  (let [john (nodes/create { :name "John" :age 28 :location "New York City, NY" })
        beth (nodes/create { :name "Elizabeth" :age 30 :location "Chicago, IL" })
        gael (nodes/create { :name "Gaël"      :age 31 :location "Montpellier" })
        rel1 (relationships/create john beth :knows)
        rel5 (relationships/create beth gael :knows)
        rt   { :type "knows" :direction "out" }]
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
  (let [john (nodes/create { :name "John" :age 28 :location "New York City, NY" })
        beth (nodes/create { :name "Elizabeth" :age 30 :location "Chicago, IL" })
        gael (nodes/create { :name "Gaël"      :age 31 :location "Montpellier" })
        rel1 (relationships/create john beth :knows)
        rel5 (relationships/create beth gael :knows)
        rt   { :type "knows" :direction "out" }]
    (is (paths/exists-between? (:id john) (:id gael) :relationships [rt] :max-depth 3))
    (relationships/purge-outgoing beth)
    (is (not (paths/exists-between? (:id john) (:id gael) :relationships [rt] :max-depth 3)))))



;;
;; Cypher queries
;;

(deftest ^{ :cypher true } test-cypher-query-example1
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        joe   (nodes/create { :name "Joe" })
        maria (nodes/create { :name "Maria" })
        steve (nodes/create { :name "Steve" })
        rel1  (relationships/create john sarah :friend)
        rel2  (relationships/create john joe :friend)
        rel3  (relationships/create sarah maria :friend)
        rel4  (relationships/create joe steve :friend)
        { :keys [data columns] } (cypher/query "START john=node({sid}) MATCH john-[:friend]->()-[:friend]->fof RETURN john, fof" { :sid (:id john) })
        row1  (map instantiate-node-from (first  data))
        row2  (map instantiate-node-from (second data))]
    (is (= 2 (count data)))
    (is (= ["john" "fof"] columns))
    (is (= (:id john)    (:id (first row1))))
    (is (= (:data john)  (:data (first row1))))
    (is (= (:id maria)   (:id (last row1))))
    (is (= (:data maria) (:data (last row1))))
    (is (= (:id john)    (:id (first row2))))
    (is (= (:data john)  (:data (first row2))))
    (is (= (:id steve)   (:id (last row2))))
    (is (= (:data steve) (:data (last row2))))))


(deftest ^{ :cypher true } test-cypher-query-example2
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        rel1  (relationships/create john sarah :friend)
        { :keys [data columns] } (cypher/query "START x = node({sid}) MATCH path = (x--friend) RETURN path, friend.name" { :sid (:id john) })
        row1  (map instantiate-path-from (first data))
        path1 (first row1)]
    (is (= 1 (count data)))
    (is (= ["path" "friend.name"] columns))
    (is (= 1 (:length path1)))
    (is (= (:start path1) (:location-uri john)))
    (is (= (:end   path1) (:location-uri sarah)))
    (is (= (first (:relationships path1)) (:location-uri rel1)))))


(deftest ^{ :cypher true } test-cypher-query-example3
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        rel1  (relationships/create john sarah :friend)
        ids   (map :id [john sarah])
        { :keys [data columns] } (cypher/query "START x = node({ids}) RETURN x.name" { :ids ids })]
    (is (= ["John" "Sarah"] (vec (map first data))))))

(deftest ^{ :cypher true } test-cypher-query-example4
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        ids   (map :id [john sarah])
        { :keys [data columns] } (cypher/query "START x = node({ids}) RETURN x" { :ids ids })]
    (is (= ids (vec (map (comp :id instantiate-node-from first) data))))))

(deftest ^{ :cypher true } test-cypher-query-example5
  (let [john  (nodes/create { :name "John" })
        sarah (nodes/create { :name "Sarah" })
        ids   (vec (map :id [sarah john]))]
    (is (= ids (vec (map :id (nodes/multi-get ids)))))))
