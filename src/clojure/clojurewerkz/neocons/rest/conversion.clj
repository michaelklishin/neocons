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

(defn kw-to-string
  "Converts a single keyword/string or a list of keywords/strings to string/strings"
  {:no-doc true}
  [x]
  (if (coll? x)
    (map name x)
    (name x)))

(defn string-to-kw
  "Converts a single string or a list of strings to keyword/keywords."
  {:no-doc true}
  [x]
  (if (coll? x)
    (map keyword x)
    (keyword x)))

(defn map-values-to-kw
  "Converts a subset of values of a map into keywords"
  {:no-doc true}
  [m keyseq]
  (into m
        (map
          (fn [[x y]] [x (string-to-kw y)])
          (select-keys m keyseq))))
