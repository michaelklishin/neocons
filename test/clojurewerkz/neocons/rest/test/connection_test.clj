;; Copyright (c) 2011-2018 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.test.connection-test
  (:require [clojurewerkz.neocons.rest :as neorest]
            [clojure.test :refer :all]))

(println (str "Using Clojure version " *clojure-version*))


;;
;; Connections/Discovery
;;

(deftest test-connection-and-discovery-using-connect-with-string-uri
  (let [connection (neorest/connect "http://localhost:7474/db/data/")
        endpoint   (:endpoint connection)]
    (is (:version                endpoint))
    (is (:node-uri               endpoint))
    (is (:batch-uri              endpoint))
    (is (:relationship-types-uri endpoint))))

(deftest test-passing-clj-http-options-to-connect
  (let [connection (neorest/connect "http://localhost:7474/db/data"
                                    (get (System/getenv) "NEO4J_LOGIN")
                                    (get (System/getenv) "NEO4J_PASSWORD")
                                    {:save-request? true})]
    (is (:endpoint connection))
    (is (get-in connection [:options :save-request?]))))
