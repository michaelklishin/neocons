## Changes between Neocons 1.0.0-beta1 and 1.0.0-beta2

### More Efficient nodes/connected-out

`clojurewerkz.neocons.rest.nodes/connected-out` implementation is now based on `nodes/multi-get` and is much more efficient for nodes
with many outgoing relationships.

### nodes/multi-get

`clojurewerkz.neocons.rest.nodes/multi-get` function efficiently (in a single HTTP request) fetches multiple nodes by id.
It implemented on top of the [Cypher query language](http://docs.neo4j.org/chunked/1.6/cypher-query-lang.html) and thus requires Neo4J Server 1.6.0 or later.


### Leiningen 2

Neocons now uses [Leiningen 2](https://github.com/technomancy/leiningen/wiki/Upgrading).
