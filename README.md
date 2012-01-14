# Neocons, a Clojure client for the Neo4J REST API

Neocons is an experimental idiomatic Clojure client for the Neo4J REST API.


## Supported Features

Neocons currently supports the following features (all via REST API, so [you can use open source Neo4J Server edition for commercial projects](http://neo4j.org/licensing-guide/)):

 * Create, read, update and delete nodes
 * Create, read, update and delete relationships
 * Fetch relationships for given node
 * Create and delete indexes
 * Index nodes
 * Query node indexes for exact matches and using full text search queries
 * Query automatic node index
 * Traverse nodes, relationships and paths
 * Find shortest path or all paths between nodes
 * Predicates over paths, for example, if they include specific nodes/relationships
 * [Cypher queries](http://docs.neo4j.org/chunked/1.6.M03/cypher-query-lang.html) (with Neo4J Server 1.6.M02 and later)


## Usage

Neocons is a very young project and until 1.0 is released and documentation guides are written,
it may be challenging to use for anyone except the author. For code examples, see our test
suite.

Once the library matures, we will update this document.


## Maven Artifacts

With Leiningen:

    [clojurewerkz/neocons "1.0.0-SNAPSHOT"]

New snapshots are released to [clojars.org](https://clojars.org/clojurewerkz/neocons) every few days.


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/neocons.png)](http://travis-ci.org/michaelklishin/neocons)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## Supported Clojure versions

Neocons is built from the ground up for Clojure 1.3 and up.


## Supported Neo4J Server versions

Neocons supports Neo4J Server 1.5.0 and later versions. For the [Cypher query language](http://docs.neo4j.org/chunked/1.6.M03/cypher-query-lang.html) support, 1.6.M02 is the minimum recommended
versions because Cypher is supported by the REST API directly without any plugins.



## License

Copyright (C) 2011-2012 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
