## Changes between Neocons 1.0.0-beta2 and 1.0.0-beta3

### rest.cypher/empty?

`neocons.rest.cypher/empty?` is a new function that can be used to tell empty Cypher responses from
non-empty ones.


### rest.relationships/create-many, rest.relationships/maybe-delete

`neocons.rest.relationships/create-many` is a new function that creates multiple relationships
from one node to several other nodes. All relationships will be of the same type. Relationships
are created concurrently using [clojure.core/pmap](http://clojuredocs.org/clojure_core/clojure.core/pmap). As a consequence, this function is supposed
to be used when number of relationships created is in dozens, hundreds of thousands.

`neocons.rest.relationships/maybe-delete` is a new function that deletes a relationship if that exists and does nothing
otherwise.


### clj-http upgraded to 0.3.6

Neocons now uses clj-http 0.3.6.


## Changes between Neocons 1.0.0-beta1 and 1.0.0-beta2

### HTTP Authentication support

Neocons now supports basic HTTP authentication. Credentials can be passed to `neocons.rest.connect` and
`neocons.rest.connect!` functions as well as via `NEO4J_LOGIN` and `NEO4J_PASSWORD` environment variables
(to be Heroku-friendly).


### neocons.rest.connect and neocons.rest.connect! no longer accept java.net.URI instances

`neocons.rest.connect` and `neocons.rest.connect!` no longer accept java.net.URI instances. Please use strings from now on.
This makes implementation of HTTP authentication and Heroku add-on support much simpler at a price of this small
undocumented feature.


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
