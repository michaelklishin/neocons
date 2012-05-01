(ns clojurewerkz.neocons.rest.test-connection
  (:require [clojurewerkz.neocons.rest :as neorest])
  (:use clojure.test))

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
