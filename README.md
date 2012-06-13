# Neocons, a Clojure client for the Neo4J REST API

Neocons is a feature rich idiomatic [Clojure client for the Neo4J REST API](http://clojureneo4j.info).


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
 * [Cypher queries](http://docs.neo4j.org/chunked/1.7/cypher-query-lang.html) (with Neo4J Server 1.6 and later)
 * Basic HTTP authentication, including [Heroku Neo4J add-on](https://devcenter.heroku.com/articles/neo4j) compatibility
 * Efficient multi-get via [Cypher queries](http://docs.neo4j.org/chunked/1.7/cypher-query-lang.html)
 * Convenience functions for working with relationships and paths


## Documentation & Examples

To get started and see what using Neocons feels like, please use our [Getting started with Clojure and Neo4J Server](http://clojureneo4j.info/articles/getting_started.html) guide.

Neocons is fairly rapidly approaching 1.0 and [documentation guides](http://clojureneo4j.info) work has recently (June 2012) started.
For more examples, see our test suite.


## Community

[Neocons has a mailing list](https://groups.google.com/group/clojure-neo4j). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## Project Maturity

## Project Maturity

Neocons is not a young project: it will be 1 year old in a few months, with active production use from week 1. It is now at 1.0.0-rc1,
all API parts are set in stone and 1.0 will be released as soon as all documentation guides are in good shape.



## Maven Artifacts

### The Most Recent Release

With Leiningen:

    [clojurewerkz/neocons "1.0.0-rc1"]

With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>neocons</artifactId>
      <version>1.0.0-rc1</version>
    </dependency>


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/neocons.png)](http://travis-ci.org/michaelklishin/neocons)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## Supported Clojure versions

Neocons is built from the ground up for Clojure 1.3 and up.


## Supported Neo4J Server versions

Neocons supports Neo4J Server 1.5.0 and later versions. For the [Cypher query language](http://docs.neo4j.org/chunked/1.7/cypher-query-lang.html) support, 1.6 is the minimum recommended
versions because Cypher is supported by the REST API directly without any plugins. Some features (in Cypher in particular)
may be specific to later version. We recommend using the most recent stable release. Neocons is actively tested against bleeding
edge Neo4J Server snapshots and we try to support important new features before stable server releases come out.


## Neocons Is a ClojureWerkz Project

Neocons is part of the [group of libraries known as ClojureWerkz](http://clojurewerkz.org), together with
[Monger](https://github.com/michaelklishin/monger), [Langohr](https://github.com/michaelklishin/langohr), [Elastisch](https://github.com/clojurewerkz/elastisch), [Quartzite](https://github.com/michaelklishin/quartzite) and several others.


## Development

Neocons uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make sure you have it installed and then run tests against
all supported Clojure versions using

    lein2 all test

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
