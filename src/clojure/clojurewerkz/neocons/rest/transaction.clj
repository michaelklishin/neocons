(ns clojurewerkz.neocons.rest.transaction
  (:require [clojurewerkz.neocons.rest          :as rest]
            [clojurewerkz.neocons.rest.records  :as records]
            [cheshire.custom                    :as json]
            [clojurewerkz.support.http.statuses :refer :all]))


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

(defn begin
  "Starts a transaction with the given cypher statements and returns a transaction record along with
  the result of the cypher statements. 0-arity creates a transaction without any cypher statements.
  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-a-transaction"
  ([] (begin []))
  ([xs]
   (let [[status headers payload]       (make-request xs (:transaction-uri rest/*endpoint*))
        neo-trans                       (instantiate-transaction
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

  ([transaction] (execute transaction []))
  ([transaction xs] (execute transaction xs (:location transaction)))
  ([transaction xs uri]
    (let [[status headers payload]          (make-request xs uri)]
      (if (missing? status)
        nil
        [(instantiate-transaction
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
  (let [{:keys [status headers body]}   (rest/DELETE (:location transaction))
        payload                         (json/decode body true)]
    (raise-on-any-errors payload)
    (:results payload)))

(defn in-transaction
  "It takes multiple statements and starts a transaction and commits them in a single HTTP request.
  For more information, see http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html#rest-api-begin-and-commit-a-transaction-in-one-request"
  [ & coll]
  (let [uri                          (str (:transaction-uri rest/*endpoint*) "/commit")
       [status headers payload]      (make-request coll uri)]
    (raise-on-any-errors payload)
    (when-not (missing? status)
      (make-cypher-responses payload))))


(defmacro with-transaction
  [commit-on-sucess name & body]
  `(let [trans#    (begin)
         ~name     (first trans#)]
     (try
       ~@body
       (when ~commit-on-sucess
         (commit ~name))
       (catch Exception e#
         ((when-not (re-find #"Transaction failed and rolled back" (. e# getMessage))
            (rollback ~name))
          (throw e#))))))
