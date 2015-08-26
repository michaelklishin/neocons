;; Copyright (c) 2011-2015 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.integration-test
  (:require [clojurewerkz.neocons.rest :as neorest]
            [clojurewerkz.neocons.rest.nodes         :as nn]
            [clojurewerkz.neocons.rest.relationships :as nr]
            [clojurewerkz.neocons.rest.paths         :as np]
            [clojure.test :refer :all]))

;;
;; Various examples using/demonstrating all
;; kinds of features
;;


(let [conn (neorest/connect "http://localhost:7474/db/data/")]
  (deftest ^{:examples true} test-example1
    (let [homepage  (nn/create conn {:url "http://clojurewerkz.org/"})
          community (nn/create conn {:url "http://clojurewerkz.org/articles/community.html"})
          about     (nn/create conn {:url "http://clojurewerkz.org/articles/about.html"})
          projects  (nn/create conn {:url "http://clojurewerkz.org/articles/projects.html"})
          rel1      (nr/create conn homepage community :links)
          rel2      (nr/create conn homepage about :links)
          rt        {:type :links :direction :out}]
      (is (np/exists-between? conn (:id homepage) (:id community) :relationships [rt]))
      (is (np/exists-between? conn (:id homepage) (:id about)     :relationships [rt]))
      (is (not (np/exists-between? conn (:id homepage) (:id projects) :relationships [rt])))
      (nr/replace-outgoing conn homepage [projects] :links)
      ;; give graph DB a moment to be updated, even on less powerful CI VMs. MK.
      (Thread/sleep 150)
      (is (np/exists-between? conn (:id homepage) (:id projects)     :relationships [rt]))
      (is (not (np/exists-between? conn (:id homepage) (:id community) :relationships [rt])))
      (is (not (np/exists-between? conn (:id homepage) (:id about)     :relationships [rt])))
      (nn/destroy-many conn [homepage community about projects])))

  (deftest ^{:examples true} test-example2
    (nn/create-index conn "by-url" {:type "exact"})
    (nn/create-index conn "roots"  {:type "exact"})
    (let [homepage  (nn/create conn {:url "http://clojurewerkz.org/"} {"roots" ["root" true]})
          community (nn/create conn {:url "http://clojurewerkz.org/articles/community.html"}
                               {"by-url" [:url "http://clojurewerkz.org/articles/community.html"]})
          _             (nn/add-to-index conn homepage "by-url" :url "http://clojurewerkz.org/")
          homepage-alt  (nn/find-one conn "by-url" "url" "http://clojurewerkz.org/")
          root-alt      (nn/find-one conn "roots" "root" true)
          community-alt (nn/find-one conn "by-url" "url" "http://clojurewerkz.org/articles/community.html")]
      (is (= (:id homepage)
             (:id homepage-alt)
             (:id root-alt)))
      (is (= (:id community)
             (:id community-alt)))
      (is (nil? (nn/find-one conn "by-url" "url" "http://example99.com")))
      ;; Neo4J REST API returns nodes in different format via index and regular GET. Make sure we handle
      ;; both cases.
      (nr/create conn homepage-alt community-alt :links)
      (nn/delete-index conn "by-url")
      (nn/delete-index conn "roots")
      (nn/destroy-many conn [community-alt homepage-alt]))))
