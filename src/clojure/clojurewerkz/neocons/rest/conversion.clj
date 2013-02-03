(ns clojurewerkz.neocons.rest.conversion)

(defprotocol Identifier
  (^Long to-id [input] "Coerces the input to an id that Neo4J can use to identify nodes, relationships and so on"))

(extend-protocol Identifier
  clojure.lang.IPersistentMap
  (to-id [^Node node]
    (:id node))

  Long
  (to-id [^Long id]
    id)

  nil
  (to-id [id]
    nil))
