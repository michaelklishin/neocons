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
  (:refer-clojure :exclude [rest]))


(defn- instantiate-transaction
  [^String commit ^String location ^String expires]
  {:commit commit :location location :expires expires})

(defn statement
  ([^String query]
     {:query query :parameters nil})
  ([^String query parameters]
     {:query query :parameters parameters}))

(defn tx-statement-from
  [m]
  {:statement (:query m) :parameters (:parameters m)})

(defn tx-payload-from
  [xs]
  {:statements (filter :statement (map tx-statement-from xs))} )

(defn- raise-on-any-errors
  [payload]
  (let [error (:errors payload)]
    (if (not= error [])
      (throw (Exception. (str "Transaction failed and rolled back. Error: " error))))))

(defn- make-request
  [xs uri]
  (let [req-body                      (json/encode (tx-payload-from xs))
        {:keys [status headers body]} (rest/POST uri :body req-body)
        payload                       (json/decode body true)]
    (raise-on-any-errors payload)
    [status headers payload]))

(defn- make-cypher-responses
  [payload]
  (map records/instantiate-cypher-query-response-from (:results payload)))

(defn- real-execute
  [transaction xs uri]
  (let [[status headers payload]          (make-request xs uri)]
    (if (missing? status)
      nil
      [(instantiate-transaction
         (:commit payload)
         (:location transaction)
         (get-in payload [:transaction :expires]))
       (make-cypher-responses payload)])))

(defn begin
  "Starts a transaction with the given cypher statements and returns a transaction record along with
  the result of the cypher statements. 0-arity function call starts a transaction without any cypher statements.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-a-transaction"
  ([]
     (begin []))
  ([xs]
     (let [[status headers payload]       (make-request xs (:transaction-uri rest/*endpoint*))
           neo-trans                       (instantiate-transaction
                                            (:commit payload)
                                            (headers "location")
                                            (get-in payload [:transaction :expires]))]
       (if (missing? status)
         nil
         [neo-trans (make-cypher-responses payload)]))))

(defn begin-tx
  "Starts a transaction without any cypher statements and returns it."
  []
  (first (begin [])))

(defn execute
  "Executes cypher statements in an existing transaction and returns the new transaction record along
  with the cypher results. If no cypher statements are give, the effect is to keep the transaction alive
  (prevent it from timing out).

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-execute-statements-in-an-open-transaction"

  ([transaction] (execute transaction []))
  ([transaction xs] (real-execute transaction xs (:location transaction))))

(defn commit
  "Commits an existing transaction with optional cypher statements which are applied
  before the transaction is committed. It returns the result of the cypher statements.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-commit-an-open-transaction"

  ([transaction]
     (commit transaction []))
  ([transaction xs]
     (let [[_ result] (real-execute transaction xs (:commit transaction))]
       result)))

(defn rollback
  "Rolls back an existing transaction.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-rollback-an-open-transaction"

  [transaction]
  (let [{:keys [status headers body]}   (rest/DELETE (:location transaction))
        payload                         (json/decode body true)]
    (raise-on-any-errors payload)
    (:results payload)))

(defn in-transaction
  "It takes multiple statements and starts a transaction and commits them in a single HTTP request.

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-and-commit-a-transaction-in-one-request

  A simple example is given below:

    (tx/in-transaction
      (tx/statement \"CREATE (n {props}) RETURN n\" {:props {:name \"My Node\"}})
      (tx/statement \"CREATE (n {props}) RETURN n\" {:props {:name \"My Another Node\"}}))"
  [ & coll]
  (let [uri                          (str (:transaction-uri rest/*endpoint*) "/commit")
        [status headers payload]      (make-request coll uri)]
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
    transaction
    true
    (let [[_ result] (tx/execute transaction [(tx/statement \"CREATE (n) RETURN ID(n)\")])]
      (println result))))"
  [transaction commit-on-success? & body]
  `(try
     (let [ret# (do ~@body)]
       (when ~commit-on-success?
         (commit ~transaction))
       ret#)
     (catch Exception e#
       ((when-not (re-find #"Transaction failed and rolled back" (. e# getMessage))
          (rollback ~transaction))
        (throw e#)))))
