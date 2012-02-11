# Neocons, a Clojure client for the Neo4J REST API

Neocons is a young idiomatic Clojure client for the Neo4J REST API.


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
 * [Cypher queries](http://docs.neo4j.org/chunked/1.6/cypher-query-lang.html) (with Neo4J Server 1.6 and later)


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

Neocons supports Neo4J Server 1.5.0 and later versions. For the [Cypher query language](http://docs.neo4j.org/chunked/1.6/cypher-query-lang.html) support, 1.6 is the minimum recommended
versions because Cypher is supported by the REST API directly without any plugins.


## Development

Install [lein-multi](https://github.com/maravillas/lein-multi) with

    lein plugin install lein-multi 1.1.0

then run tests against Clojure 1.3.0 and 1.4.0[-beta1] using

    lein multi javac, test

Then create a branch and make your changes on it. Once you are done with your changes and all tests pass, submit
a pull request on Github.


## License

Copyright (C) 2011-2012 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.


## FAQ

### Will embedding be supported in the future?

Because Neo4J is GPL software, linking against it will require your project to be open source. While there are
perfectly valid use cases for this, Neocons was developed to be used in commercial projects and Neocons
authors strongly prefer business-friendly licenses and Clojure community commitment to the Eclipse Public License v1.

Neocons namespace structure leave the door open for future Neo4J Server features like the binary protocol but it
is highly unlikely that it will ever cover embedding, should Neo4J license stay GPL forever.

If you need a solid well-maintained EPLv1-licensed embeddable graph database for Clojure 1.3.0 and later, please
use [Jiraph](https://github.com/flatland/jiraph).
