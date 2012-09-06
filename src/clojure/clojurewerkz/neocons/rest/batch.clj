(ns clojurewerkz.neocons.rest.batch
  (:require [clojurewerkz.neocons.rest         :as rest]
            [clojurewerkz.neocons.rest.records :as rec]
            [cheshire.custom                   :as json]))



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
  [ops]
  (let [{:keys [status headers body]} (rest/POST (:batch-uri rest/*endpoint*) :body (json/encode ops))
        payload                       (map :body (json/decode body true))]
    (map rec/instantiate-record-from payload)))
