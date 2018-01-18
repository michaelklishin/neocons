# Neocons, a Clojure client for the Neo4J REST API

Neocons is a feature rich idiomatic [Clojure client for the Neo4J REST API](http://clojureneo4j.info).


## Supported Features

Neocons currently supports the following features via the Bolt Protocol:

* [Cypher queries](http://docs.neo4j.org/chunked/stable/cypher-query-lang.html)
* Sessions
* Transactions

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
 * [Cypher queries](http://docs.neo4j.org/chunked/stable/cypher-query-lang.html)
 * Basic HTTP authentication, including [Heroku GrapheneDB add-on](https://devcenter.heroku.com/articles/graphenedb) compatibility
 * Efficient multi-get via [Cypher queries](http://docs.neo4j.org/chunked/stable/cypher-query-lang.html)
 * Convenience functions for working with relationships and paths
 * Neo4J 2.0 [transactions](http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html)
 * Neo4J 2.0 [labels](http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html)
 * Neo4J 2.0 [constraints](http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html)


## Documentation & Examples

To get started and see what using Neocons feels like, please use our [Getting started with Clojure and Neo4J Server](http://clojureneo4j.info/articles/getting_started.html) guide.

[Documentation guides](http://clojureneo4j.info) are mostly complete.
For more examples, see our [test suite](test).


## Community

[Neocons has a mailing list](https://groups.google.com/group/clojure-neo4j). Feel free to join it and ask any questions you may have.

To subscribe for announcements of releases, important changes and so on, please follow [@ClojureWerkz](https://twitter.com/#!/clojurewerkz) on Twitter.


## Project Maturity

Neocons is not a young project: first released in October 2011, it's been
in production use from week 1.

It now supports Neo4j 3.x & 2.x release(s) and the API is stable. Documentation
is in good shape, too.



## Maven Artifacts

### The Most Recent Release

Neocons artifacts are [released to Clojars](https://clojars.org/clojurewerkz/neocons). If you are using Maven, add the following repository
definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

### The Most Recent Stable Release

With Leiningen:

    [tuddman/neocons "3.2.1-SNAPSHOT"]

With Maven:

    <dependency>
      <groupId>clojurewerkz</groupId>
      <artifactId>neocons</artifactId>
      <version>3.2.0</version>
    </dependency>



## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/neocons.png)](http://travis-ci.org/michaelklishin/neocons)


CI is hosted by [travis-ci.org](http://travis-ci.org)


## Supported Clojure Versions

Neocons requires Clojure 1.8+.
The most recent stable Clojure release (1.9) is highly recommended.


## Supported Neo4J Server Versions

### Neocons 3.2

Neocons `3.2` targets Neo4j Server 3.0.x & 3.1.x,  and includes Neo4j's Bolt Protocol.

### Neocons 3.1

Neocons `3.1` targets Neo4J Server 2.2.

### Neocons 3.0

Neocons `3.0` targets Neo4J Server 2.0 and later versions.
`1.9.x` compatibility may be less than complete.

### Neocons 2.0

Neocons `2.0` targets Neo4J Server 2.0, although 
the test suite also passes against 1.9.x.

There are incompatible changes in 1.9 and 2.0 in mutating Cypher
syntax, so Neo4j Server 1.8 compatibility is less than perfect.

### Neocons 1.1

Neocons `1.1` supports Neo4J Server 1.5.0 and later versions. For the
[Cypher query
language](http://docs.neo4j.org/chunked/stable/cypher-query-lang.html)
support, 1.6 is the minimum recommended versions because Cypher is
supported by the REST API directly without any plugins. Some features
(in Cypher in particular) may be specific to a later version. We
recommend using the most recent stable release. Neocons is actively
tested against bleeding edge Neo4J Server snapshots and we try to
support important new features before stable server releases come out.

If you use OpsCode Chef, there is a [Neo4J Server Chef cookbook](https://github.com/michaelklishin/neo4j-server-chef-cookbook).


## Neocons Is a ClojureWerkz Project

Neocons is part of the [group of libraries known as ClojureWerkz](http://clojurewerkz.org), together with
[Monger](https://github.com/michaelklishin/monger), [Langohr](https://github.com/michaelklishin/langohr), [Welle](https://github.com/michaelklishin/welle), [Quartzite](https://github.com/michaelklishin/quartzite), [Validateur](https://github.com/michaelklishin/validateur) and several others.


## Development

Neocons uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make sure you have it installed and then run tests against
all supported Clojure versions using

    lein all test

Then create a branch and make your changes on it. Once you are done with your changes and all tests pass, submit
a pull request on Github.

The tests require a Neo4j on localhost on port 7474. An easy way to
arrange for this if you do not generally run a local server may be to
use docker:

`docker run --publish=7474:7474 --publish=7687:7687 --volume=$HOME/neo4j/data:/data neo4j`

and pass the default credentials to lein on the command line

`NEO4J_LOGIN=neo4j NEO4J_PASSWORD=admin lein test`

## License

Copyright (C) 2011-2018 Michael S. Klishin, Alex Petrov, and the ClojureWerkz team.

Double licensed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html) (the same as Clojure) or
the [Apache Public License 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).


## FAQ

### Will embedding be supported in the future?

Because Neo4J is GPL software, linking against it will require your project to be open source. While there are
perfectly valid use cases for this, Neocons was developed to be used in commercial projects and Neocons
authors strongly prefer business-friendly licenses and Clojure community commitment to the Eclipse Public License v1.

Neocons namespace structure leave the door open for future Neo4J Server features like the binary protocol but it
is highly unlikely that it will ever cover embedding, should Neo4J license stay GPL forever.

If you need a solid well-maintained EPLv1-licensed embeddable graph database for Clojure, please
take a look at [Titanium](http://titanium.clojurewerkz.org) and [Jiraph](https://github.com/flatland/jiraph).
