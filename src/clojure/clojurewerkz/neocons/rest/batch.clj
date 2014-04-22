;; Copyright (c) 2011-2014 Michael S. Klishin, Alex Petrov, and The ClojureWerkz
;; Team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns clojurewerkz.neocons.rest.batch
  "Batch operation execution"
  (:require [clojurewerkz.neocons.rest         :as rest]
            [clojurewerkz.neocons.rest.records :as rec]
            [cheshire.core                     :as json])
  (:import  clojurewerkz.neocons.rest.Connection))



;;
;; API
;;

(defn perform
  "Submits a batch of operations for execution, returning a lazy sequence of results. Operations must include
   two keys:

   :method (\"POST\", \"GET\", etc)
   :to (a path relative to the database root URI)

   and may or may not include

   :data (a map of what would be in the request body in cases non-batch API was used)
   :id   (request id that is used to refer to previously executed operations in the same batch)

   If you need to insert a batch of nodes at once, consider using neocons.rest.nodes/create-batch.

   See http://docs.neo4j.org/chunked/milestone/rest-api-batch-ops.html for more information."
  [^Connection connection ops]
  (let [{:keys [status headers body]} (rest/POST connection (get-in connection [:endpoint :batch-uri])
                                                 :body (json/encode ops))
        payload                       (map :body (json/decode body true))]
    (map rec/instantiate-record-from payload)))
