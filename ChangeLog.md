## Changes between Neocons 1.0.0-beta1 and 1.0.0-beta2

### HTTP Authentication support

Neocons now supports basic HTTP authentication. Credentials can be passed to `neocons.rest.connect` and
`neocons.rest.connect!` functions as well as via `NEO4J_LOGIN` and `NEO4J_PASSWORD` environment variables
(to be Heroku-friendly).


### clj-http upgraded to 0.3.4

Neocons now uses clj-http 0.3.4.


### cypher/tableize and cypher/tquery

New function `cypher/tableize` transforms Cypher query responses (that list columns and row sets separately) into tables,
much like SQL queries do. The following test demonstrates how it works:

``` clojure
(deftest ^{:cypher true} test-tableize
  (let [columns ["x.name" "x.age"]
        rows    [["John" 27] ["Sarah" 28]]]
    (is (= [{"x.name" "John" "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}] (vec (cy/tableize columns rows))))))
```

`cypher/tquery` combines `cypher/query` and `cypher/tableize`: it executes Cypher queries and returns results
formatted as table.


### More Efficient nodes/connected-out

`clojurewerkz.neocons.rest.nodes/connected-out` implementation is now based on `nodes/multi-get` and is much more efficient for nodes
with many outgoing relationships.


### nodes/multi-get

`clojurewerkz.neocons.rest.nodes/multi-get` function efficiently (in a single HTTP request) fetches multiple nodes by id.
It implemented on top of the [Cypher query language](http://docs.neo4j.org/chunked/1.6/cypher-query-lang.html) and thus requires Neo4J Server 1.6.0 or later.


### Leiningen 2

Neocons now uses [Leiningen 2](https://github.com/technomancy/leiningen/wiki/Upgrading).
