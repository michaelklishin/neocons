(ns clojurewerkz.neocons.rest.test-integration
  (:require [clojurewerkz.neocons.rest :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nn]
            [clojurewerkz.neocons.rest.relationships :as nr]
            [clojurewerkz.neocons.rest.paths         :as np])
  (:use clojure.test))

(neorest/connect! "http://localhost:7474/db/data/")


;;
;; Various examples using/demonstrating all
;; kinds of features
;;

(deftest ^{:examples true} test-example1
  (let [homepage  (nn/create {:url "http://clojurewerkz.org/"})
        community (nn/create {:url "http://clojurewerkz.org/articles/community.html"})
        about     (nn/create {:url "http://clojurewerkz.org/articles/about.html"})
        projects  (nn/create {:url "http://clojurewerkz.org/articles/projects.html"})
        rel1      (nr/create homepage community :links)
        rel2      (nr/create homepage about :links)
        rt        {:type :links :direction :out}]
    (is (np/exists-between? (:id homepage) (:id community) :relationships [rt]))
    (is (np/exists-between? (:id homepage) (:id about)     :relationships [rt]))
    (is (not (np/exists-between? (:id homepage) (:id projects) :relationships [rt])))
    (nr/replace-outgoing homepage [projects] :links)
    ;; give graph DB a moment to be updated, even on less powerful CI VMs. MK.
    (Thread/sleep 150)
    (is (np/exists-between? (:id homepage) (:id projects)     :relationships [rt]))
    (is (not (np/exists-between? (:id homepage) (:id community) :relationships [rt])))
    (is (not (np/exists-between? (:id homepage) (:id about)     :relationships [rt])))
    (nn/destroy-many [homepage community about projects])))

(deftest ^{:examples true} test-example2
  (nn/create-index "by-url" {:type "exact"})
  (nn/create-index "roots"  {:type "exact"})
  (let [homepage  (nn/create {:url "http://clojurewerkz.org/"} {"roots" ["root" true]})
        community (nn/create {:url "http://clojurewerkz.org/articles/community.html"}
                             {"by-url" [:url "http://clojurewerkz.org/articles/community.html"]})
        _             (nn/add-to-index homepage "by-url" :url "http://clojurewerkz.org/")
        homepage-alt  (nn/find-one "by-url" :url "http://clojurewerkz.org/")
        root-alt      (nn/find-one "roots" :root true)
        community-alt (nn/find-one "by-url" :url "http://clojurewerkz.org/articles/community.html")]
    (is (= (:id homepage)
           (:id homepage-alt)
           (:id root-alt)))
    (is (= (:id community)
           (:id community-alt)))
    (is (nil? (nn/find-one "by-url" :url "http://example99.com")))
    ;; Neo4J REST API returns nodes in different format via index and regular GET. Make sure we handle
    ;; both cases.
    (nr/create homepage-alt community-alt :links)
    (nn/delete-index "by-url")
    (nn/delete-index "roots")
    (nn/destroy-many [community-alt homepage-alt])))
