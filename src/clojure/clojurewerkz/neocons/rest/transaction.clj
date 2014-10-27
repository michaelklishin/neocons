;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.transaction
  "Transaction management functions (Neo4J 2.0+ only)."
  (:require [clojurewerkz.neocons.rest          :as rest]
            [clojurewerkz.neocons.rest.records  :as records]
            [cheshire.core                      :as json]
            [clojurewerkz.support.http.statuses :refer [missing?]])
  (:import [clojurewerkz.neocons.rest Connection])
  (:refer-clojure :exclude [rest]))


(defn- instantiate-transaction
  [^String commit ^String location ^String expires]
  {:commit commit :location location :expires expires})

(defn statement
  "Populates a Cypher statement to be sent as part of a transaction."
  ([^String query]
     {:query query :parameters nil})
  ([^String query parameters]
     {:query query :parameters parameters})
  ([^String query parameters result-data-contents]
     {:query query :parameters parameters :result-data-contents result-data-contents}))

(defn tx-statement-from
  [{:keys [query parameters result-data-contents] :as m}]
  (into {:statement query :parameters parameters}
        (when (contains? m :result-data-contents)
          {:resultDataContents result-data-contents})))

(defn tx-payload-from
  [xs]
  {:statements (filter :statement (map tx-statement-from xs))} )

(defn- raise-on-any-errors
  [payload]
  (let [error (:errors payload)]
    (if (not= error [])
      (throw (Exception. (str "Transaction failed and rolled back. Error: " error))))))

(defn- make-request
  [^Connection conn xs uri]
  (let [req-body                      (json/encode (tx-payload-from xs))
        {:keys [status headers body]} (rest/POST conn uri :body req-body)
        payload                       (json/decode body true)]
    (raise-on-any-errors payload)
    [status headers payload]))

(defn- make-cypher-responses
  [payload]
  (map records/instantiate-cypher-query-response-from (:results payload)))


(defn- real-execute
  [^Connection conn transaction xs uri]
  (let [[status headers payload]          (make-request conn xs uri)]
    (when-not (missing? status)
      [(instantiate-transaction
        (:commit payload)
        (:location transaction)
        (get-in payload [:transaction :expires]))
       (make-cypher-responses payload)])))


(defn begin
  "Starts a transaction with the given cypher statements and returns a transaction record along with
  the result of the cypher statements. 0-arity function call starts a transaction without any cypher statements.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-a-transaction"
  ([^Connection connection]
     (begin connection []))
  ([^Connection connection xs]
     (let [[status headers payload]       (make-request connection xs (get-in connection [:endpoint :transaction-uri]))
           neo-trans                      (instantiate-transaction
                                           (:commit payload)
                                           (headers "location")
                                           (get-in payload [:transaction :expires]))]
       (when-not (missing? status)
         [neo-trans (make-cypher-responses payload)]))))


(defn begin-tx
  "Starts a transaction without any cypher statements and returns it."
  [^Connection connection]
  (first (begin connection [])))


(defn execute
  "Executes cypher statements in an existing transaction and returns the new transaction record along
  with the cypher results. If no cypher statements are give, the effect is to keep the transaction alive
  (prevent it from timing out).

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-execute-statements-in-an-open-transaction"

  ([^Connection connection transaction] (execute connection transaction []))
  ([^Connection connection transaction xs] (real-execute connection transaction xs (:location transaction))))

(defn commit
  "Commits an existing transaction with optional cypher statements which are applied
  before the transaction is committed. It returns the result of the cypher statements.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-commit-an-open-transaction"

  ([^Connection connection transaction]
     (commit connection transaction []))
  ([^Connection connection transaction xs]
     (let [[_ result] (real-execute connection transaction xs (:commit transaction))]
       result)))

(defn rollback
  "Rolls back an existing transaction.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-rollback-an-open-transaction"

  [^Connection connection transaction]
  (let [{:keys [status headers body]}   (rest/DELETE connection (:location transaction))
        payload                         (json/decode body true)]
    (raise-on-any-errors payload)
    (:results payload)))

(defn in-transaction
  "It takes multiple statements and starts a transaction and commits them in a single HTTP request.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-and-commit-a-transaction-in-one-request

  A simple example is given below:

  (tx/in-transaction connection
  (tx/statement \"CREATE (n {props}) RETURN n\" {:props {:name \"My Node\"}})
  (tx/statement \"CREATE (n {props}) RETURN n\" {:props {:name \"My Another Node\"}}))"
  [^Connection connection & coll]
  (let [uri                          (str (get-in connection [:endpoint :transaction-uri]) "/commit")
        [status headers payload]     (make-request connection coll uri)]
    (raise-on-any-errors payload)
    (when-not (missing? status)
      (make-cypher-responses payload))))


(defmacro with-transaction
  "A basic macro which gives a fine grained control of working in a transaction without manually
  committing or checking for exceptions.

  If commit-on-success? is true, then the given transaction is committed on success. Else the user
  is responsible for manually committing/rolling back the transaction. At any stage if there is an
  error, the transaction is rolled back if necessary.

  A simple example is given below:

  (let [transaction (tx/begin-tx)]
  (tx/with-transaction
  connection
  transaction
  true
  (let [[_ result] (tx/execute connection transaction [(tx/statement \"CREATE (n) RETURN ID(n)\")])]
  (println result))))"
  [^Connection connection transaction commit-on-success? & body]
  `(try
     (let [ret# (do ~@body)]
       (when ~commit-on-success?
         (commit ~connection ~transaction))
       ret#)
     (catch Exception e#
       ((when-not (re-find #"Transaction failed and rolled back" (. e# getMessage))
          (rollback ~connection ~transaction))
        (throw e#)))))
