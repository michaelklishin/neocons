(ns clojurewerkz.neocons.rest.test.common
  (:require [clojurewerkz.neocons.rest :as neorest]))

(def ^{:dynamic true} *connection*)

(defn once-fixture
  [f]
  (binding [*connection*  (neorest/connect "http://localhost:7474/db/data/")]
    (f)))
