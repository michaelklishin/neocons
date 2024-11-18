---
title: "Neocons, a Clojure client for Neo4J REST API: Using the Cypher Query Language"
layout: article
---

## About this guide

 * What is Cypher
 * Using Cypher to traverse graphs, retrieve nodes, relationships and path
 * Using mutating Cypher in Neo4J 1.8 to create and mutate graphs

This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a> (including images & stylesheets). The source is available [on Github](https://github.com/clojurewerkz/neocons.docs).


## What version of Neocons does this guide cover?

This guide covers Neocons 2.0.


## Cypher Overview

One of the most powerful features of Neo4J is [Cypher, a query language (like SQL) for querying, traversing and mutating (Neo4J Server 1.8+)
graphs](http://docs.neo4j.org/chunked/milestone/cypher-query-lang.html). Cypher makes queries like "return me all friends of my friends" or "return me all pages this page links to that were updated
less than 24 hours ago" possible in a couple of lines of code. As such, operations and ad hoc queries in the Clojure REPL or Neo4J shell
with Cypher are very common.

Cypher also enables several operations Neo4J Server REST API does not provide to be executed efficiently. One common example is
the "multi-get" operation that returns a collection of nodes by ids.

Cypher queries are performed using `clojurewerkz.neocons.rest.cypher/tquery` and `clojurewerkz.neocons.rest.cypher/query` functions.
`clojurewerkz.neocons.rest.cypher/tquery` is more common because it returns data in a more convenient tabular form, while
`/query` returns columns and result rows separately.

Covering Cypher itself is out of scope for this tutorial so lets just take a look at a couple of examples. Here is how
to find all Amy's friends via Cypher:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy"})
        bob   (nn/create conn {:username "bob"})
        _     (nrl/create conn amy bob :friend {:source "college"})
        res   (cy/tquery conn "START person=node({sid}) MATCH person-[:friend]->friend RETURN friend" {:sid (:id amy)})]
    (println res)))
```

And here is how to get back usernames and ages of multiple people using Cypher:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.cypher :as cy]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy" :age 27})
        bob   (nn/create conn {:username "bob" :age 28})
        _     (nrl/create conn amy bob :friend {:source "college"})
        res   (cy/tquery conn "START x = node({ids}) RETURN x.username, x.age" {:ids (map :id [amy bob])})]
    (println res)))
```

The latter query is roughly equivalent to

``` sql
SELECT username, age FROM nodes WHERE id IN (…);
```

in SQL.

Cypher is fundamental to Neo4J is the most powerful (and easy to use) tool for many common cases.



## Retrieving nodes via Cypher

TBD


## Retrieving relationships via Cypher

TBD


## Retrieving paths via Cypher

TBD


## Creating nodes via Cypher

TBD


## Creating relationships via Cypher

TBD


## Updating nodes via Cypher

TBD



## What to Read Next

Congratulations, this is the last guide. For the definitive reference on Cypher, see [Neo4J documentation on Cypher](http://docs.neo4j.org/chunked/stable/cypher-query-lang.html).

Take a look at [other guides](/articles/guides.html), they cover all kinds of topics.



## Tell Us What You Think!

Please take a moment to tell us what you think about this guide on Twitter or the [Neocons mailing list](https://groups.google.com/forum/#!forum/clojure-neo4j)

Let us know what was unclear or what has not been covered. Maybe you do not like the guide style or grammar or discover spelling mistakes. Reader feedback is key to making the documentation better.
