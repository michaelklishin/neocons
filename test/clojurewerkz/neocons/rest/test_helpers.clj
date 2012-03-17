(ns clojurewerkz.neocons.rest.test-helpers
  (:use clojure.test
        clojurewerkz.neocons.rest.helpers))

(deftest test-id-extraction
  (is (= 1 (extract-id "http://localhost:7474/db/data/node/1")))
  (is (= 10 (extract-id"http://localhost:7474/db/data/node/10")))
  (is (= 100 (extract-id"http://localhost:7474/db/data/node/100")))
  (is (= 1000 (extract-id"http://localhost:7474/db/data/node/1000")))
  (doseq [id (range 1 10000)]
    (is (= id (extract-id (str "http://localhost:7474/db/data/node/" id))))))
