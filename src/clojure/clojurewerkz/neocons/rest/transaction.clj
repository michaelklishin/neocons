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
  ([transaction] (execute transaction []))
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
  ([transaction] (commit transaction []))
  ([transaction xs]
   (let [[_ result] (execute transaction xs (:commit transaction))]
    result)))

(defn rollback
  [transaction]
  (let [{:keys [status headers body]} (rest/DELETE (:location transaction))]
    body))
