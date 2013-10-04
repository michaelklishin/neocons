(ns clojurewerkz.neocons.rest.transaction
  (:require [clojurewerkz.neocons.rest         :as rest]
            [clojurewerkz.neocons.rest.records :as records]
            [cheshire.custom                   :as json])
  (:use     clojurewerkz.support.http.statuses))


(defn statement-to-map
  [stmt]
  {:statement (:query stmt) :parameters (:parameters stmt)})

(defn statements-to-map
  [stmts]
  {:statements (filter :statement (map statement-to-map stmts))} )

(defn make-request
  [xs uri]
  (let [statements                      (json/encode (statements-to-map xs))
        {:keys [status headers body]}   (rest/POST uri :body statements)]
    [status headers body]))

(defn make-cypher-responses
  [payload]
  (map records/instantiate-cypher-query-response-from (:results payload)))

(defn begin
  "Starts a transaction with the given cypher statements and returns a transaction record along with
  the result of the cypher statements. 0-arity creates a transaction without any cypher statements.
  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-a-transaction"
  ([] (begin []))
  ([xs]
   (let [[status headers body]          (make-request xs (:transaction-uri rest/*endpoint*))
        payload                         (json/decode body true)
        neo-trans                       (records/instantiate-transaction
                                          (:commit payload)
                                          (headers "location")
                                          (get-in payload [:transaction :expires]))]
    (if (missing? status)
      nil
      [neo-trans (make-cypher-responses payload)]))))

(defn execute
  "Executes cypher statements in an existing transaction and returns the new transaction record along
  with the cypher results. If no cypher statement is give, the effect is to keep the transaction alive
  (prevent it from timing out).

  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-execute-statements-in-an-open-transaction"

  [transaction] (execute transaction []))
  ([transaction xs] (execute transaction xs (:location transaction)))
  ([transaction xs uri]
    (let [[status headers body]          (make-request xs uri)
          payload                        (json/decode body true)]
      (if (missing? status)
        nil
        [(records/instantiate-transaction
           (:commit payload)
           (:location transaction)
           (get-in payload [:transaction :expires]))
         (make-cypher-responses payload)]))))

(defn commit
  "Commits an existing transaction with option cypher statements which are applied
  before the transaction is committed.
  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-commit-an-open-transaction"

  ([transaction] (commit transaction []))
  ([transaction xs]
   (let [[_ result] (execute transaction xs (:commit transaction))]
    result)))

(defn rollback
  "Rolls back an existing transaction.
  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-rollback-an-open-transaction"

  [transaction]
  (let [{:keys [status headers body]} (rest/DELETE (:location transaction))]
    body))
