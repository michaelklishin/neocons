## Changes between Neocons 1.0.0-beta3 and 1.0.0-rc1

### More robust relationships/create

`clojurewerkz.neocons.rest.relationships/create` is now more robust and handles cases when given nodes
may only have `:id` set on them. This may happen in part a regression and in part because of edge cases
in the REST API in our own code.


### clj-http upgraded to 0.4.0

Neocons now uses clj-http 0.4.0.


## Changes between Neocons 1.0.0-beta2 and 1.0.0-beta3

### clojurewerkz.neocons.rest.nodes/find-one

`clojurewerkz.neocons.rest.nodes/find-one` finds a single node in an index. Supposed to be used with
unique indexes.


### clojurewerkz.neocons.rest.relationships/replace-outgoing

`clojurewerkz.neocons.rest.relationships/replace-outgoing` deletes outgoing relationships of a certain
type on a node and creates new relationships of the same type with another set of nodes.


### clojurewerkz.neocons.rest.nodes/destroy-many

`clojurewerkz.neocons.rest.nodes/destroy-many` is a new function that destroys multiple nodes using
`clojurewerkz.neocons.rest.nodes/destroy`


### clojurewerkz.neocons.rest.nodes/destroy

A new function that purges all node relationships and immediately deletes the node using `clojurewerkz.neocons.rest.nodes/delete`.


### clojurewerkz.neocons.rest.relationships/delete-many

`clojurewerkz.neocons.rest.relationships/delete-many` deletes multiple relationships using
`clojurewerkz.neocons.rest.relationships/delete`.


### clojurewerkz.neocons.rest.nodes/delete-many

`clojurewerkz.neocons.rest.nodes/delete-many` is a new function that deletes multiple nodes using
`clojurewerkz.neocons.rest.nodes/delete`. For a node to be deleted, it must have no relationships.

To purge all node relationships and immediately delete the node, use `clojurewerkz.neocons.rest.nodes/destroy`.


### clojurewerkz.neocons.rest.nodes/delete, /update, /set-property are now polymorphic

`clojurewerkz.neocons.rest.nodes/delete`, `/update` and `/set-property` are now polymorhic: they accept
both `clojurewerkz.neocons.rest.record.Node` instances and node ids as longs.


### rest.nodes/multi-get is renamed to rest.nodes/get-many

`clojurewerkz.neocons.rest.nodes/multi-get` has been renamed to `clojurewerkz.neocons.rest.nodes/get-many` to be consistent with
similar functions in other namespaces. `clojurewerkz.neocons.rest.nodes/multi-get` is not yet removed (for ease of upgrading)
but is deprecated and will be removed completely in the future.


### Relationship record fields renamed

Two `Relationship` record fields were renamed to match REST API responses better:

 * `:start-uri` is now `:start`
 * `:end-uri` is now `:end`


### rest.relationships/starts-with?, rest.relationships/ends-with?

`clojurewerkz.neocons.rest.relationships/starts-with?` and `clojurewerkz.neocons.rest.relationships/ends-with?` predicates check whether
given relationships starts (or ends, respectively) with a node with the given id. This is often useful
for automated testing of logic that creates relationships.


### rest.cypher/empty?

`clojurewerkz.neocons.rest.cypher/empty?` is a new function that can be used to tell empty Cypher responses from
non-empty ones.


### rest.relationships/create-many, rest.relationships/maybe-delete

`clojurewerkz.neocons.rest.relationships/create-many` is a new function that creates multiple relationships
from one node to several other nodes. All relationships will be of the same type. Relationships
are created concurrently using [clojure.core/pmap](http://clojuredocs.org/clojure_core/clojure.core/pmap). As a consequence, this function is supposed
to be used when number of relationships created is in dozens, hundreds of thousands.

`clojurewerkz.neocons.rest.relationships/maybe-delete` is a new function that deletes a relationship if that exists and does nothing
otherwise.


### clj-http upgraded to 0.3.6

Neocons now uses clj-http 0.3.6.



## Changes between Neocons 1.0.0-beta1 and 1.0.0-beta2

### HTTP Authentication support

Neocons now supports basic HTTP authentication. Credentials can be passed to `clojurewerkz.neocons.rest.connect` and
`clojurewerkz.neocons.rest.connect!` functions as well as via `NEO4J_LOGIN` and `NEO4J_PASSWORD` environment variables
(to be Heroku-friendly).


### neocons.rest.connect and neocons.rest.connect! no longer accept java.net.URI instances

`clojurewerkz.neocons.rest.connect` and `clojurewerkz.neocons.rest.connect!` no longer accept java.net.URI instances. Please use strings from now on.
This makes implementation of HTTP authentication and Heroku add-on support much simpler at a price of this small
undocumented feature.


### clj-http upgraded to 0.3.4

Neocons now uses clj-http 0.3.4.


### cypher/tableize and cypher/tquery

New function `clojurewerkz.neocons.rest.cypher/tableize` transforms Cypher query responses (that list columns and row sets separately) into tables,
much like SQL queries do. The following test demonstrates how it works:

``` clojure
(deftest ^{:cypher true} test-tableize
  (let [columns ["x.name" "x.age"]
        rows    [["John" 27] ["Sarah" 28]]]
    (is (= [{"x.name" "John" "x.age" 27}
            {"x.name" "Sarah" "x.age" 28}] (vec (cy/tableize columns rows))))))
```

`clojurewerkz.neocons.rest.cypher/tquery` combines `clojurewerkz.neocons.rest.cypher/query` and `clojurewerkz.neocons.rest.cypher/tableize`: it executes Cypher queries and returns results
formatted as table.


### More Efficient nodes/connected-out

`clojurewerkz.neocons.rest.nodes/connected-out` implementation is now based on `clojurewerkz.neocons.rest.nodes/multi-get` and is much more efficient for nodes
with many outgoing relationships.


### nodes/multi-get

`clojurewerkz.neocons.rest.nodes/multi-get` function efficiently (in a single HTTP request) fetches multiple nodes by id.
It implemented on top of the [Cypher query language](http://docs.neo4j.org/chunked/1.6/cypher-query-lang.html) and thus requires Neo4J Server 1.6.0 or later.


### Leiningen 2

Neocons now uses [Leiningen 2](https://github.com/technomancy/leiningen/wiki/Upgrading).
