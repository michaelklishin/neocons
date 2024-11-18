---
title: "Neocons, a Clojure client for Neo4J REST API: Getting Started"
layout: article
---

## About this guide

This guide combines an overview of Neocons with a quick tutorial that helps you to get started with it.
It should take about 15 minutes to read and study the provided code examples. This guide covers:

 * Features of Neocons
 * Clojure and Neo4J Server version requirements
 * How to add Neocons dependency to your project
 * A very brief introduction to graph databases and theory
 * Basic operations (creating nodes and relationships, fetching nodes, using Cypher queries, traversing graph paths)

This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a> (including images & stylesheets). The source is available [on Github](https://github.com/clojurewerkz/neocons.docs).


## What version of Neocons does this guide cover?

This guide covers Neocons 3.2.


## Neocons Overview

Neocons is an idiomatic Clojure client for the Neo4J Server REST API. It is simple and easy to use, strives to support
every Neo4J Server feature, makes working with Cypher queries a joy, takes a "batteries included" approach and is well maintained.


### What Neocons is not

Neocons is a REST API client, it currently does not support working with embedded Neo4J databases. Neocons was designed for
commercial products and using embedded open source Neo4J editions is not legal without obtaining a commercial license or
open sourcing your entire application.

Neocons is not an ORM/ODM. It does not provide graph visualization features, although this is an interesting area to explore
in the future versions. Neocons may or may not be Web Scale and puts correctness and productivity above sky high benchmarks.


## Supported Clojure versions

Neocons 3.2 requires Clojure 1.8+.


## Supported Neo4J Server Versions

Neocons 3.2 targets Neo4J Server 3.x.

Most features would work with Neo4J Server 1.6.0 but some features
(including the Cypher language) may be specific to more recent
versions. The most recent Neo4J Server is thus recommended.


## Adding Neocons Dependency To Your Project

### With Leiningen

``` clojure
[clojurewerkz/neocons "3.2.0"]
```

### With Maven

Add Clojars repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

And then the dependency:

``` xml
<dependency>
  <groupId>clojurewerkz</groupId>
  <artifactId>neocons</artifactId>
  <version>"3.2.0</version>
</dependency>
```

It is recommended to stay up-to-date with new versions. New releases
and important changes are announced
[@ClojureWerkz](http://twitter.com/ClojureWerkz).


## Connecting to Neo4J

### Basics

Before you use Neocons, you need to connect to Neo4J Server. "Connect" here means "perform service discovery" since REST/HTTP services like Neo4J Server
do not have a concept of persistent stateful connection, but we use a more common term for database clients here. For that, you use
`clojurewerkz.neocons.rest/connect` function:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]))

;; connects to the default Neo4J Server host/port/path
(def conn (nr/connect "http://localhost:7474/db/data/"))
```

Beginning from Neocons 3.0.0, the connect method returns a `Connection` record. This record has information about
neo4j service enpoints along with information about authentication parameters and a map containing (optional) http parameters.


### Authenticating

Neo4J REST API uses [HTTP authentication](http://www.ietf.org/rfc/rfc2617.txt) to authenticate clients. Authentication is mandatory in PaaS environments such as Heroku.
With Neocons, you can either pass credentials as user info in the connection URL:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]))

;; connects to a Neo4J Server neo4j.megacorp.internal with
;; username of "neocons" and password of "SEcRe7"
(def conn (nr/connect "http://neocons:SEcRe7@neo4j.megacorp.internal/db/data/"))
```

Alternatively, if the connection URL does not have user info but `NEO4J_LOGIN` and `NEO4J_PASSWORD` environment variables are set,
Neocons will use them.

Related Neo4J Server guide: [Securing Access to Neo4J Server](http://docs.neo4j.org/chunked/stable/security-server.html)


## A very short intro to graph databases

Graph is a data structure that represents connections (or lack of them) between things. Connected things
are called "nodes" or "vertices" and connections are called "relationships" or "edges". Nodes may have properties
(like person name or age), same for relationships (for example, when two people first met each other). There
may be more than one relationship between two nodes. Relationships are directed (have a start and an end; for example,
Web pages link to each other).


## Creating nodes

Nodes are created using the `clojurewerkz.neocons.rest.nodes/create` function:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]))

(defn -main
  [& args]
  ;; creates a node without properties
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        node  (nn/create conn)]
    (println node)))
```

Nodes typically have properties. They are passed to `clojurewerkz.neocons.rest.nodes/create` as maps:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]))

(defn -main
  [& args]
  ;; creates a node wit two properties
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        node  (nn/create conn {:url "http://clojureneo4j.info" :domain "clojureneo4j.info"})]
    (println node)))
```

### Nodes are just Clojure maps

The function returns a new node which is a Clojure record but for all intents and purposes should be treated and handled
as a map. In Neo4J, nodes have identifiers so `:id` key for created and fetched nodes is always set. Node identifiers
are used by various Neocons API functions.

Fetched nodes also have the `:location-uri` and `:data` fields. `:location-uri` returns a URI from which a node can be fetched
again with a GET request. Location URI is typically not used by applications. `:data`, however, contains node properties
and is very commonly used.


## Creating relationships

Now that we know how to create nodes, lets create two nodes representing two Web pages that link to each other and add a directed
relationship between them:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        page1 (nn/create conn {:url "http://clojurewerkz.org"})
        page2 (nn/create conn {:url "http://clojureneo4j.info"})
        ;; a relationship that indicates that page1 links to page2
        rel   (nrl/create conn page1 page2 :links)]
    (println rel)))
```

Relationships can have properties, just like nodes:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy"})
        bob   (nn/create conn {:username "bob"})
        rel   (nrl/create conn amy bob :friend {:source "college"})]
    (println rel)))
```

### Relationships are just Clojure maps

Similarly to nodes, relationships are technically records with a few mandatory attributes:

 * `:id`
 * `:start`
 * `:end`
 * `:type`
 * `:data`
 * `:location-uri`

`:id`, `:data` and `:location-uri` serve the same purpose as with nodes. `:start` and `:end` return location URIs of nodes on both
ends of a relationship. `:type` returns relationship type (like "links" or "friend" or "connected-to").

Just like nodes, created relationships have `:id` set on them.

`clojurewerkz.neocons.rest.relationships/create-many` is a function that lets you creates multiple relationships for a node,
all with the same direction and type. It is covered in the [Populating the graph guide](/articles/populating.html).


## Fetching nodes

Now that we know how to populate a small graph, lets look at how you query it. The most basic operation is to fetch
a node by id with `clojurewerkz.neocons.rest.nodes/get`:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy"})]
    (println (nn/get conn (:id amy)))))
```

It returns a node value that is a Clojure map.

It's possible to fetch a node by id stored elsewhere:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]))

(defn -main
  [& args]

  ;; fetches a node with id 42
  (let [conn  (nr/connect "http://localhost:7474/db/data/")]
    (println (nn/get conn 42))))
```


## Fetching relationships

`clojurewerkz.neocons.rest.relationships/get` fetches a single relationship by id:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy"})
        bob   (nn/create conn {:username "bob"})
        rel   (nrl/create conn amy bob :friend {:source "college"})]
    (println (nrl/get conn (:id rel)))))
```

Neocons also provides other ways of fetching relationships (based on the start node and type, for example) that will be described
in detail the [Traversing the graph](/articles/traversing.html) guide. `clojurewerkz.neocons.rest.relationships/outgoing-for` and
`clojurewerkz.neocons.rest.relationships/incoming-for` are two such functions, lets take a look at them:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy"})
        bob   (nn/create conn {:username "bob"})
        _     (nrl/create conn amy bob :friend {:source "college"})]
    (println (nrl/outgoing-for conn amy :types [:friend]))))
```

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        amy   (nn/create conn {:username "amy"})
        bob   (nn/create conn {:username "bob"})
        _     (nrl/create conn amy bob :friend {:source "college"})]
    (println (nrl/incoming-for conn bob :types [:friend]))))
```

Both accept a node and a collection of relationship types you are interested in as the `:types` option, returning a collection
of relationships.


## Using Cypher queries

One of the most powerful features of Neo4J is Cypher, a query language (like SQL) for querying, traversing and mutating (Neo4J Server 1.8+)
graphs. Cypher makes queries like "return me all friends of my friends" or "return me all pages this page links to that were updated
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
        amy   (nn/create conn  {:username "amy"})
        bob   (nn/create conn {:username "bob"})
        _     (nrl/create conn amy bob :friend {:source "college"})
        res (cy/tquery conn "START person=node({sid}) MATCH person-[:friend]->friend RETURN friend" {:sid (:id amy)})]
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

Cypher is fundamental to Neo4J is the most powerful (and easy to use) tool for many common cases. As such, Neocons documentation
has a whole guide dedicated to it: [The Cypher query language](/articles/cypher.html).


## Traversing the graph

To traverse a graph means to extract information from it, often by starting at a node and following 0 or more relationships. There
are certain well known algorithms that can be executed on graphs: for example, finding shortest path(s) between two nodes. A lot of Neo4J
power comes from support for some of those algorithms and flexible graph traversal in general.

### Traversal overview

Traversing the graph means following relationships between nodes and accumulating nodes (node traversal) or relationships (relationship
traversal) along the way. In the end, the client is returned a collection of nodes, relationships or *paths*. Path is a data structure which
has a start node, an end node, a length and collections of nodes and/or relationships.

Traversing supports various options that define whether returned node set should be unique or not, what order ([depth first](http://en.wikipedia.org/wiki/Depth-first_search)
or [breadth first](http://en.wikipedia.org/wiki/Breadth-first_search)) traversing should happen in,
when to terminate the traversal and so on.


### Node traversal

Node traversal with Neoncons is performed using the `clojurewerkz.neocons.rest.nodes/traverse` function. It takes several arguments:
a node to start traversing from, relationships to follow and additional options like what nodes to return:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        john  (nn/create conn {:name "John"})
        adam  (nn/create conn {:name "Alan"})
        pete  (nn/create conn {:name "Peter"})
        _     (nrl/create conn john adam :friend)
        _     (nrl/create conn adam pete :friend)]
    (println (nn/traverse conn (:id john) :relationships [{:direction "out" :type "friend"}] :return-filter {:language "builtin" :name "all_but_start_node"}))))
```


### Relationship traversal

To perform relationship traversal, use the `clojurewerkz.neocons.rest.relationships/traverse` function. It is very similar to its
counterpart that traverses nodes:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        john  (nn/create conn {:name "John"})
        adam  (nn/create conn {:name "Alan"})
        pete  (nn/create conn {:name "Peter"})
        _     (nrl/create conn john adam :friend)
        _     (nrl/create conn adam pete :friend)]
    (println (nrl/traverse conn (:id john) :relationships [{:direction "out" :type "friend"}]))))
```


### Working with paths

To perform a path traversal, use the `clojurewerkz.neocons.rest.paths/traverse` function. Several predicate functions
make it easy to determine whether a particular node or relationship belong to a path:

* `clojurewerkz.neocons.rest.paths/node-in?`,
* `clojurewerkz.neocons.rest.paths/relationship-in?`
* `clojurewerkz.neocons.rest.paths/included-in?`

Finally, `clojurewerkz.neocons.rest.paths/shortest-between` and `clojurewerkz.neocons.rest.paths/all-shortest-between` calculate and return
the shortest path(s) between two nodes.

For more examples, see the [Traversing the graph](/articles/traversing.html) guide.


### Checking if a path exists

Another common operation is checking whether a path between two nodes exists at all.

`clojurewerkz.neocons.rest.paths/exists-between?` checks whether there is a path from node A to node B:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrl]
            [clojurewerkz.neocons.rest.paths :as np]))

(defn -main
  [& args]
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        john  (nn/create conn {:name "John"})
        beth  (nn/create conn {:name "Elizabeth"})
        gael  (nn/create conn {:name "Gaël"})
        _     (nrl/create conn john beth :knows)
        _     (nrl/create conn beth gael :knows)
        rt    {:type "knows" :direction "out"}]
    (println (np/exists-between? conn (:id john) (:id gael) :relationships [rt] :max-depth 3))))
```

Relationship types that can be used (followed) during traversal are given via the `:relationships` option.


### Traversing vs Cypher queries

While traversing features are powerful, they predate a newer, more generic and even more powerful Neo4J Server feature: the Cypher query language.
With the introduction and several revisions on Cypher, in some situations traversing the graph is no longer necessary. Please keep this in mind.
That said, some problems are easier to solve using traversing than sophisticated Cypher queries.


## Debugging HTTP calls

Neocons uses [clj-http](https://github.com/dakrone/clj-http) library for handling the actual HTTP interaction. clj-http exposes various options like `:debug` which can be accessed in the `:options` map of the `Connection` record.

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]))

(defn -main
  [& args]
  (let [conn   (nr/connect "http://localhost:7474/db/data/")
        conn2  (assoc-in conn [:options :debug] true)]
    (println (nr/create conn2 {:name "John"}))))
```

A possible output of the above program is:

```
Request: org.apache.http.entity.StringEntity
{:user-info nil,
 :use-header-maps-in-response? true,
 :body-type org.apache.http.entity.StringEntity,
 :debug true,
 :headers
 {"content-type" "application/json",
  "accept" "application/json",
  "accept-encoding" "gzip, deflate"},
 :server-port 7474,
 :http-url "http://localhost:7474/db/data/node",
 :throw-entire-message? true,
 :content-type :json,
 :character-encoding "UTF-8",
 :uri "/db/data/node",
 :server-name "localhost",
 :query-string nil,
 :body
 {:streaming false,
  :repeatable true,
  :contentType #<BasicHeader Content-Type: text/plain; charset=UTF-8>,
  :contentLength 15,
  :contentEncoding nil,
  :content
  #<ByteArrayInputStream java.io.ByteArrayInputStream@68f0d46>,
  :class org.apache.http.entity.StringEntity,
  :chunked false},
 :scheme :http,
 :request-method :post}
HttpRequest:
{:config nil,
 :method "POST",
 :requestLine
 #<BasicRequestLine POST http://localhost:7474/db/data/node HTTP/1.1>,
 :aborted false,
 :params
 #<BasicHttpParams org.apache.http.params.BasicHttpParams@1b47e68c>,
 :protocolVersion #<HttpVersion HTTP/1.1>,
 :URI #<URI http://localhost:7474/db/data/node>,
 :class org.apache.http.client.methods.HttpPost,
 :allHeaders
 [#<BasicHeader Connection: close>,
  #<BasicHeader content-type: application/json>,
  #<BasicHeader accept: application/json>,
  #<BasicHeader accept-encoding: gzip, deflate>],
 :entity #<StringEntity org.apache.http.entity.StringEntity@5c6ca8e5>}

#clojurewerkz.neocons.rest.records.Node{:id 2, :location-uri "http://localhost:7474/db/data/node/2", :data {:name "John"}, :relationships-uri nil, :create-relationship-uri "http://localhost:7474/db/data/node/2/relationships"}

```

For more information about the possible options, see [clj-http](https://github.com/dakrone/clj-http).


## Wrapping up

Congratulations, you now can use Neocons to perform fundamental operations with Neo4J. Now you know enough to start building
a real application. There are many features that we haven't covered here; Cypher alone is worth a long guide full of examples.
They will be explored in the rest of the guides.

We hope you find Neocons reliable, consistent and easy to use. In case you need help, please ask on the [mailing list](https://groups.google.com/forum/#!forum/clojure-neo4j)
and follow us on Twitter [@ClojureWerkz](http://twitter.com/ClojureWerkz).


## What to read next

The documentation is organized as a number of guides, covering all kinds of topics.

We recommend that you read the following guides first, if possible, in this order:

 * [Populating the graph](/articles/populating.html)
 * [Traversing the graph](/articles/traversing.html)
 * [The Cypher query language](/articles/cypher.html)


## Tell Us What You Think!

Please take a moment to tell us what you think about this guide on
Twitter or the [Neocons mailing
list](https://groups.google.com/forum/#!forum/clojure-neo4j)

Let us know what was unclear or what has not been covered. Maybe you
do not like the guide style or grammar or discover spelling
mistakes. Reader feedback is key to making the documentation better.
