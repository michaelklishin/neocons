## Changes between Neocons 3.2.0 and 3.3.0 (unreleased)

### Clojure 1.8

Neocons now depends on `org.clojure/clojure` version `1.9.0`. It is
still compatible with Clojure 1.8 and if your `project.clj` depends on
a different version, it will be used, but 1.9 is the default now.

### Neo4J Java Driver Upgrade

Neo4J Java driver dependency has been updated to `1.5.0`.

### clj-http Upgrade

clj-http dependency has been updated to `3.7.0`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.8.0`.




## Changes between Neocons 3.1.0 and 3.2.0 (Jan 18th, 2018)

### Neo4J Bolt Protocol Support

GitHub issue: [#89](https://github.com/michaelklishin/neocons/pull/89)

### Neo4J 3.2 Support

Neocons now supports Neo4J Server 3.2.



## Changes between Neocons 3.0.0 and 3.1.0

### Neo4J 2.2: Ability to Change Password

`clojurewerkz.neocons.rest.password/change-password` is a new function that
can be used to update user credentials:

``` clojure
(require 'clojurewerkz.neocons.rest.password :as pwd)

(pwd/change-password uri "joe" "old-pwd" "new-pwd")
```

Contributed by Rohit Aggarwal.


### Urly Dependency Dropped

Neocons no longer depends on Urly, a deprecated ClojureWerkz library.

Contributed by Ricardo J. Mendez.

### Clojure 1.7

Neocons now depends on `org.clojure/clojure` version `1.7.0`. It is
still compatible with Clojure 1.5 and if your `project.clj` depends on
a different version, it will be used, but 1.7 is the default now.

### clj-http Upgrade

clj-http dependency has been updated to `2.0.0`.

### Cheshire Upgrade

Cheshire dependency has been updated to `5.5.0`.

### HTTP Authentication via URI

It is now possible to specify credentials in the URI.

Contributed by Øystein Jakobsen.

### ClojureWerkz Support Upgrade

Neocons now uses ClojureWerkz Support 1.1.0.


## Changes between Neocons 2.0.0 and 3.0.0

Neocons no longer uses a dynamic var to hold the state of the connection. This leads to significant changes to the API as the connection has to be passed to functions. The position of the connection argument is always the first argument for the sake of consistency:

``` clojure
(require '[clojurewerkz.neocons.rest :as nr])
(require '[clojurewerkz.neocons.rest.nodes :as nn])

;; with Neocons 2.0

(nr/connect! "http://localhost:7476/db")
(nn/create {:url "http://clojurewerkz.org/"})

;; with Neocons 3.0
(let [conn (nr/connect "http://localhost:7476/db")]
  (nn/create conn {:url "http://clojurewerkz.org/"}))
```

Additionally `connect!` function in `clojurewerkz.neocons.rest` no longer exists. This has been replaced by function `connect` in `clojurewerkz.neocons.rest`. The `connect` function has the same arguments as the `connect!` function only it returns a `Connection` record.

The `Connection` record has a key called `:options` which can be used to pass additional parameters to be used by [clj-http](https://github.com/dakrone/clj-http) like `debug`.

### Clojure 1.6

Neocons now depends on `org.clojure/clojure` version `1.6.0`. It is still compatible with Clojure 1.4 and if your `project.clj` depends on a different version, it will be used, but 1.6 is the default now.

### Cheshire 5.3

Neocons now uses [Cheshire](https://github.com/dakrone/cheshire) 5.3.

### clj-http upgraded to 0.9.1

Neocons now uses clj-http 0.9.1.

### Neo4J 2.0 Index Creation Fix

Neocons will now use a key name accepted by Neo4J 2.0.0-rc1
when creating indexes.

Contributed by Rohit Aggarwal.


## Changes between Neocons 2.0.0-rc1 and 2.0.0

### Clojure 1.6 Compatibility Fixes

Neocons is again compatible with recent
releases of Clojure 1.6 (master).


## Changes between Neocons 2.0.0-beta3 and 2.0.0-rc1

### Renamed Function

Renamed the `clojurewerkz.neocons.rest.constraints/drop` to
`clojurewerkz.neocons.rest.constraints/drop-unique` for future
portability.

## Changes between Neocons 2.0.0-beta2 and 2.0.0-beta3

### Constraints Support (Neo4J 2.0 Only)

`clojurewerkz.neocons.rest.constraints` is a new namespace that
implements [Neo4J 2.0 constraints](http://docs.neo4j.org/chunked/milestone/rest-api-schema-constraints.html).

``` clojure
(require '[clojurewerkz.neocons.rest.constraints :as cts])

;; create a uniqueness constraint
(cts/create-unique "Person" :name)

;; get constraint info
(cts/get-unique "Person" :name)

;; drop a constraint
(cts/drop "Person" :name)
```



### Labels Support (Neo4J 2.0 Only)

`clojurewerkz.neocons.rest.labels` is a new namespace that provides
support for [labels in Neo4J 2.0](http://docs.neo4j.org/chunked/milestone/rest-api-node-labels.html).

It is possible to add, replace, remove and retrieve labels to/from a node.

To add labels to a node, use `clojurewerkz.neocons.rest.labels/add`:

``` clojure
(require '[clojurewerkz.neocons.rest.labels :as nl])

(nl/add node ["neo4j" "clojure"])
```

To add replaces all labels on a node, use `clojurewerkz.neocons.rest.labels/replace`:

``` clojure
(require '[clojurewerkz.neocons.rest.labels :as nl])

(nl/replace node ["graph" "database"])
```

Deleting a label from a node is possible with `clojurewerkz.neocons.rest.labels/remove`:

``` clojure
(require '[clojurewerkz.neocons.rest.labels :as nl])

(nl/remove node "database")
```

`clojurewerkz.neocons.rest.labels/get-all-labels` is the function that lists
either all labels in the database (w/o arguments) or on a specific node
(1-arity):

``` clojure
(require '[clojurewerkz.neocons.rest.labels :as nl])

(nl/get-all-labels node)
;= [all labels]
(nl/get-all-labels node)
;= [labels on node]
```




## Changes between Neocons 1.1.0 and 2.0.0-beta2

### Clojure 1.3 Support Dropped

Neocons no longer supports Clojure 1.3.

### Transaction Support (Neo4J Server 2.0)

Neocons 2.0 gains support for [transactions](http://docs.neo4j.org/chunked/milestone/rest-api-transactional.html).


#### Higher Level API

A group of Cypher statements can be executed in a transaction
that will be committed automatically upon success. Any error
during the execution will trigger a rollback.

``` clojure
(require '[clojurewerkz.neocons.rest.transaction :as tx])

(tx/in-transaction
  (tx/statement "CREATE (n {props}) RETURN n" {:props {:name "Node 1"}})
  (tx/statement "CREATE (n {props}) RETURN n" {:props {:name "Node 2"}}))
```

#### Lower Level API

Transactions are instantiated from a group of Cypher statements
that are passed as maps to `clojurewerkz.neocons.rest.transaction/begin`:

``` clojure
(let [t (tx/begin-tx [{:statement "CREATE (n {props}) RETURN n" {:props {:name "My node"}}}])]
  (tx/commit t))

(let [t (tx/begin-tx)]
  (tx/rollback t))
```

`clojurewerkz.neocons.rest.transaction/commit` and
`clojurewerkz.neocons.rest.transaction/rollback` commit
and roll a transaction back, respectively.

#### Macro for working with a transaction

If you want a more fine grained control of working in a transaction without manually
committing or checking for exceptions, you can use the
`clojurewerkz.neocons.rest.transaction/with-transaction` macro.

``` clojure
(require '[clojurewerkz.neocons.rest.transaction :as tx])

(let [transaction (tx/begin-tx)]
  (tx/with-transaction
    transaction
    true
    (let [[_ result] (tx/execute transaction [(tx/statement "CREATE (n) RETURN ID(n)")])]
    (println result))))
```

If there any errors while processing, the transaction is rolled back.

The first argument is the variable which holds the transaction information. The second argument to the macro is `commit-on-success`, which commits the transaction there are no errors.

## Changes between Neocons 1.1.0-beta4 and 1.1.0

### ClojureWerkz Support Upgrade

Neocons now uses ClojureWerkz Support 0.15.0.

### Clojure 1.5 By Default

Neocons now depends on `org.clojure/clojure` version `1.5.0`. It is
still compatible with Clojure 1.3+ and if your `project.clj` depends on
a different version, it will be used, but 1.5 is the default now.

We encourage all users to upgrade to 1.5, it is a drop-in replacement
for the majority of projects out there.

### Cheshire 5.x

Neocons now uses [Cheshire](https://github.com/dakrone/cheshire) 5.x.


## Changes between Neocons 1.1.0-beta3 and 1.1.0-beta4

### Improved URI Path Encoding

Keys with colons are now handled correctly (as of Urly `2.0.0-alpha5`).



## Changes between Neocons 1.1.0-beta2 and 1.1.0-beta3

### Correct URI Path Encoding

Neocons now correctly encodes all parts of URIs, which means
index keys and values can contain whitespace and Unicode
characters, for example.

GH issue: #20

### clj-http upgraded to 0.6.4

Neocons now uses clj-http 0.6.4.

### Support upgraded to 0.12.0

Neocons now uses ClojureWerkz Support 0.12.0.



## Changes between Neocons 1.1.0-beta1 and 1.1.0-beta2

### Support upgraded to 0.10.0

Neocons now uses ClojureWerkz Support 0.10.0.


### clj-http upgraded to 0.6.3

Neocons now uses clj-http 0.6.3.


### clojurewerkz.neocons.rest.relationship/maybe-create Now Fully Supports Ids

`clojurewerkz.neocons.rest.relationship/maybe-create` now correctly works with node ids
as well as `Node` records.

GH issue: #19.

### More Informative Exceptions

HTTP exceptions bubbling up now will carry more information (namely the response `:body`).

Contributed by Adrian Gruntkowski.


### clojurewerkz.neocons.rest.relationships/get-many

`clojurewerkz.neocons.rest.relationships/get-many` is a new function that fetches multiple relationships
by id in a single request:

```clojure
(require '[clojurewerkz.neocons.rest.relationships :as rel])

(rel/get-many [id1 id2 id3])
```

Contributed by Adrian Gruntkowski.


## Changes between Neocons 1.0.0 and 1.1.0-beta1

### Initial Spatial Plugin Support

Neocons now has initial support for the Neo4J Spatial plugin in the `clojurewerkz.neocons.spatial`
namespace.

Contributed by Kyle Goodwin.


### Cheshire For JSON Serliazation

Neocons now uses (and depends on) [Cheshire](https://github.com/dakrone/cheshire) for JSON serialization.
[clojure.data.json](https://github.com/clojure/data.json) is no longer a dependency.

### Clojure 1.4 By Default

Neocons now depends on `org.clojure/clojure` version `1.4.0`. It is still compatible with Clojure 1.3 and if your `project.clj` depends
on 1.3, it will be used, but 1.4 is the default now.

We encourage all users to upgrade to 1.4, it is a drop-in replacement for the majority of projects out there.


### Pass Configuration When Creating Node Indexes

`clojurewerkz.neocons.rest.nodes/create-index` now correctly passes index configuration
to Neo4J Server. Reported in #6.


### clj-http upgraded to 0.5.5

Neocons now uses clj-http 0.5.5.



## Changes between Neocons 1.0.0-rc3 and 1.0.0

### Better support for unique graph entities

`clojurewerkz.neocons.rest.nodes/create-unique-in-index` and `clojurewerkz.neocons.rest.relationships/create-unique-in-index` are
two functions that create a node (relationship) and add it to an index while ensuring entry uniqueness atomically.


Contributed by Zhemin Lin.


## Changes between Neocons 1.0.0-rc2 and 1.0.0-rc3

### Generic batch operation support

`clojurewerkz.neocons.rest.batch/perform` allows for executing any sequence of operations in batch using [Neo4J REST API for batch operations](http://docs.neo4j.org/chunked/milestone/rest-api-batch-ops.html):

``` clojure
(ns clojurewerkz.neocons.examples
  (:require [clojurewerkz.neocons.rest               :as neorest]
            [clojurewerkz.neocons.rest.batch         :as b]))

(neorest/connect! "http://localhost:7474/db/data/")

(let [ops [{:method "POST"
                     :to     "/node"
                     :body   {}
                     :id     0}
                    {:method "POST"
                     :to     "/node"
                     :body   {}
                     :id     1}
                    {:method "POST",
                     :to     "{0}/relationships",
                     :body   {:to   "{1}"
                              :data {}
                              :type "knows"}
                     :id     2}]
               res (doall (b/perform ops))]
           (println res))
```

This is a relatively low level function. It is reasonable to expect an easier to use way of executing batch operations
in future versions of Neocons.


### Batch creation of nodes

A new function, `clojurewerkz.neocons.rest.nodes/create-batch`, can be used to efficiently insert a large number of nodes
at the same time (up to hundreds of thousands or millions).

It returns a lazy sequence of results, which both makes it more memory efficient and may require forcing the evaluation
with `clojure.core/doall` in some cases.


### Unique indexes and graph entities

`clojurewerkz.neocons.rest.nodes/create-index` now accepts a new configuration option: `:unique`, which makes
the index unique (that allows/guarantees only one entry per key).

`clojurewerkz.neocons.rest.relationships/create-index` works the same way.

`clojurewerkz.neocons.rest.nodes/add-to-index` and
`clojurewerkz.neocons.rest.relationships/add-to-index` now take an additional (optional) argument that, when set to true,
will add the entity to the index [as unique](http://docs.neo4j.org/chunked/milestone/rest-api-unique-indexes.html)


## Changes between Neocons 1.0.0-rc1 and 1.0.0-rc2

### Support for indexes over relationships

This include `clojurewerkz.neocons.rest.relationships/create-index`, `clojurewerkz.neocons.rest.relationships/add-to-index`
and other functions that are `clojurewerkz.neocons.rest.nodes` counterparts but for relationships.

Thanks to Neo4J Server's consistent REST API, indexes support for relationships is almost identical to that on nodes.



## Changes between Neocons 1.0.0-beta4 and 1.0.0-rc1

### Documentation guides

We started working on documentation guides for Neocons at [clojureneo4j.info](http://clojureneo4j.info)


## Changes between Neocons 1.0.0-beta3 and 1.0.0-beta4

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
