---
title: "Neocons, a Clojure client for Neo4J REST API: Traversing the Graph"
layout: article
---

## About this guide

 * Graph traversals
 * Operations on paths
 * Path predicates


This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a> (including images & stylesheets). The source is available [on Github](https://github.com/clojurewerkz/neocons.docs).


## What version of Neocons does this guide cover?

This guide covers Neocons 2.0.


## Overview

To traverse a graph means to extract information from it, often by starting at a node and following 0 or more relationships. There
are certain well known algorithms that can be executed on graphs: for example, finding shortest path(s) between two nodes. A lot of Neo4J
power comes from support for some of those algorithms and flexible graph traversal in general.

Note that as the [Cypher query language](http://docs.neo4j.org/chunked/milestone/cypher-query-lang.html) matures, the traversal API becomes less frequently used and may be considered to be "low level" (in comparison).
As such, please evaluate if your task at hand can be solved with a [Cypher query which Neocons supports](/articles/cypher.html).
In the future, more and more functionality in Neocons will be refactored to use Cypher internally.

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
  (let [conn   (nr/connect "http://localhost:7474/db/data/")
        john   (nn/create conn {:name "John"})
        adam   (nn/create conn {:name "Alan"})
        pete   (nn/create conn {:name "Peter"})
        _      (nrl/create conn john adam :friend)
        _      (nrl/create conn adam pete :friend)]
    (println (nn/traverse conn (:id john) :relationships [{:direction "out" :type "friend"}]
                          :return-filter {:language "builtin" :name "all_but_start_node"})))
```

Accepted options are:

 * `:order` (a string, default: `"breadth_first"`)
 * `:uniqueness` (a string, default: `"node_global"`): `"node_global"`, `"none"`, `"relationship_global"`, `"node_path"`, `"relationship_path"`
 * `:max-depth` (an integer, no default)
 * `:return-filter` (a map, default: `{:language "builtin" :name "all"}`): a map with a language the traversal is in (typically builtin) and the name of the filter (builtin filters: `"all"`, `"all_but_start_node"`). Defines a named filter for the traversal.
 * `:prune-evaluator` (a map, default: `{:language "builtin" :name "none"}`)
 * `:relationships` (a map, no default): a map with two mandatory keys, `:direction` and `:type` that specifies what relationships should be followed during the traversal. Example: `{:direction "all" :type "friend"}`. Direction can be one of: `"all"`, `"in"` or `"out"`.

For more information, see the [Neo4J REST API traversals guide](http://docs.neo4j.org/chunked/milestone/rest-api-traverse.html).

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
  (let [conn   (nr/connect "http://localhost:7474/db/data/")
        john   (nn/create conn {:name "John"})
        adam   (nn/create conn {:name "Alan"})
        pete   (nn/create conn {:name "Peter"})
        _      (nrl/create conn john adam :friend)
        _      (nrl/create conn adam pete :friend)]
    (println (nrl/traverse conn (:id john) :relationships [{:direction "out" :type "friend"}])))
```

Accepted options are:

 * `:order` (a string, default: `"breadth_first"`): specifies what tranversal order to use: [depth first](http://en.wikipedia.org/wiki/Depth-first_search) or [breadth first](http://en.wikipedia.org/wiki/Breadth-first_search)
 * `:uniqueness` (a string, default: `"node_global"`)
 * `:max-depth` (an integer, no default)
 * `:return-filter` (a map, default: `{:language "builtin" :name "all"}`)
 * `:prune-evaluator` (a map, default: `{:language "builtin" :name "none"}`)
 * `:relationships` (a map, no default): a map with two mandatory keys, `:direction` and `:type` that specifies what relationships should be followed during the traversal. Example: `{:direction "all" :type "friend"}`.

For more information, see the [Neo4J REST API traversals guide](http://docs.neo4j.org/chunked/milestone/rest-api-traverse.html).


### Working with paths

Path in a graph is a sequence of nodes and relationships. A path has a start (a node) and an end (also a node). Paths are finite and always have length associated with
them. A lot of power of graph databases comes from efficient calculation of paths (for example, shortest paths).

Paths in Neocons are represented as immutable Clojure maps (technically, `clojurewerkz.neocons.rest.records.Path` record instances) that are guaranteed
to have several attributes:

 * `:start`: a URI that points to the ending point (always a node) in this path. You can fetch the node using the `clojurewerkz.neocons.rest.nodes/fetch-from` function
 * `:end`: a URI that points to the ending point (always a node) in this path. You can fetch the node using the `clojurewerkz.neocons.rest.nodes/fetch-from` function
 * `:length`: a length of this path as an integer
 * `:nodes`: a collection of node records on this path
 * `:relationships`: a collection of relationship records on this path


### Path traversals

Path traversals walk the graph, follow certain relationships, accumulate nodes/relationships/segments and return them as paths.
To perform a path traversal, use the `clojurewerkz.neocons.rest.paths/traverse` function:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.paths         :as paths]))

(defn -main
  [& args]
  (let [conn (nr/connect "http://localhost:7474/db/data/")
        john (nn/create conn {:name "John"})
        adam (nn/create conn {:name "Alan"})
        pete (nn/create conn {:name "Peter"})
        rel1 (nrel/create conn john adam :friend)
        rel2 (nrel/create conn adam pete :friend)
        xs1  (paths/traverse conn (:id john) :relationships [{:direction "all" :type "friend"}])
        xs2  (paths/traverse conn (:id john) :relationships [{:direction "all" :type "enemy"}])]
    (println xs1)
    (println xs2)
    (let [path1 (first xs1)
          path2 (second xs1)
          path3 (last xs1)]
      ;= 0
      (println (:length path1))
      ;= 1
      (println (:length path2))
      ;= 2
      (println (:length path3))
      ;= 1
      (println (count (:nodes path1)))
      ;= same as (:location-uri john)
      (println (:start path1))
      ;= same as (:location-uri pete)
      (println (:end   path3)))))
```

### Path predicates

Several predicate functions make it easy to determine whether a particular node or relationship belong to a path:

* `clojurewerkz.neocons.rest.paths/node-in?`,
* `clojurewerkz.neocons.rest.paths/relationship-in?`
* `clojurewerkz.neocons.rest.paths/included-in?`

Finally, `clojurewerkz.neocons.rest.paths/shortest-between` and `clojurewerkz.neocons.rest.paths/all-shortest-between` calculate and return
the shortest path(s) between two nodes.

An example that demonstrates several of those functions:

``` clojure
(ns neocons.docs.examples
  (:require [clojurewerkz.neocons.rest :as nr]
            [clojurewerkz.neocons.rest.nodes :as nn]
            [clojurewerkz.neocons.rest.relationships :as nrel]
            [clojurewerkz.neocons.rest.paths         :as paths]))

(defn -main
  [& args]
  ;; a longer example that demonstrates several functions related to paths
  (let [conn  (nr/connect "http://localhost:7474/db/data/")
        john  (nn/create conn {:name "John" :age 28 :location "New York City, NY"})
        liz   (nn/create conn {:name "Liz"  :age 27 :location "Buffalo, NY"})
        beth  (nn/create conn {:name "Elizabeth" :age 30 :location "Chicago, IL"})
        bern  (nn/create conn {:name "Bernard"   :age 33 :location "Zürich"})
        gael  (nn/create conn {:name "Gaël"      :age 31 :location "Montpellier"})
        alex  (nn/create conn {:name "Alex"      :age 24 :location "Toronto, ON"})
        rel1  (nrel/create conn john liz  :knows)
        rel2  (nrel/create conn liz  beth :knows)
        rel3  (nrel/create conn liz  bern :knows)
        rel4  (nrel/create conn bern gael :knows)
        rel5  (nrel/create conn gael beth :knows)
        rel6  (nrel/create conn beth gael :knows)
        rel7  (nrel/create conn john gael :knows)
        rt    {:type "knows" :direction "out"}
        xs1   (paths/all-shortest-between conn (:id john) (:id liz)  :relationships [rt] :max-depth 1)
        path1 (first xs1)
        xs2   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 1)
        xs3   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 2)
        path3 (first xs3)
        xs4   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 3)
        path4 (first xs4)
        xs5   (paths/all-shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 7)
        path5 (first xs5)
        path6 (last  xs5)
        path7 (paths/shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 7)
        path8 (paths/shortest-between conn (:id john) (:id beth) :relationships [rt] :max-depth 1)
        path9 (paths/shortest-between conn (:id john) (:id alex) :relationships [rt] :max-depth 1)]
    ;= true
    (println (empty? xs2))
    ;= nil
    (println (= 1 (count xs1)))
    ;= true
    (println (= 2 (count xs3)))
    ;= true
    (println (= 2 (count xs4)))
    ;= true
    (println (= 2 (count xs5)))
    ;= true
    (println (= (:start path1) (:location-uri john)))
    ;= true
    (println (= (:end   path1) (:location-uri liz)))
    ;= true
    (println (= 2 (:length path3)))
    ;= true
    (println (= (:start path3) (:location-uri john)))
    ;= true
    (println (= (:end   path3) (:location-uri beth)))
    ;= true
    (println (= 2 (:length path4)))
    ;= true
    (println (= (:start path4) (:location-uri john)))
    ;= true
    (println (= (:end   path4) (:location-uri beth)))
    ;= true
    (println (= 2 (:length path5)))
    ;= true
    (println (= (:start path5) (:location-uri john)))
    ;= true
    (println (= (:end   path5) (:location-uri beth)))
    ;= true
    (println (= 2 (:length path6)))
    ;= true
    (println (= (:start path6) (:location-uri john)))
    ;= true
    (println (= (:end   path6) (:location-uri beth)))
    ;= true
    (println (= 2 (:length path7)))
    ;= true
    (println (= (:start path7) (:location-uri john)))
    ;= true
    (println (= (:end   path7) (:location-uri beth)))
    ;= true
    (println (paths/node-in? conn (:id john) path7))
    ;= false
    (println (paths/node-in? conn (:id bern) path7))
    ;= true
    (println (paths/node-in? conn john path7))
    ;= false
    (println (paths/node-in? conn bern path7))
    ;= true
    (println (paths/included-in? conn john path7))
    ;= true
    (println (paths/included-in? conn rel1 path7))
    ;= true
    (println (paths/relationship-in? conn (:id rel1) path7))
    ;= true
    (println (paths/relationship-in? conn rel1 path7))
    ;= false
    (println (paths/included-in? conn rel4 path7))
    ;= false
    (println (paths/relationship-in? conn (:id rel4) path7))
    ;= false
    (println (paths/relationship-in? conn rel4 path7))
    ;= true
    (println (paths/exists-between? conn (:id john) (:id liz) :relationships [rt] :max-depth 7))
    ;= false
    (println (paths/exists-between? conn (:id beth) (:id bern) :relationships [rt] :max-depth 7))
    ;= nil
    (println path9)))
```

In cases when there are multiple paths of equal length, `clojurewerkz.neocons.rest.paths/shortest-between` will return just one and `clojurewerkz.neocons.rest.paths/all-shortest-between` will return all of them.


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
  (let [conn (nr/connect "http://localhost:7474/db/data/")
        john (nn/create conn {:name "John"})
        beth (nn/create conn {:name "Elizabeth"})
        gael (nn/create conn {:name "Gaël"})
        _    (nrl/create conn john beth :knows)
        _    (nrl/create conn beth gael :knows)
        rt   {:type "knows" :direction "out"}]
    (println (np/exists-between? conn (:id john) (:id gael) :relationships [rt] :max-depth 3))))
```

Relationship types that can be used (followed) during traversal are given via the `:relationships` option.

TBD: more path operations


## What to read next

The documentation is organized as a number of guides, covering all kinds of topics.

We recommend that you read the following guides first, if possible, in this order:

 * [The Cypher query language](/articles/cypher.html)



## Tell Us What You Think!

Please take a moment to tell us what you think about this guide on
Twitter or the [Neocons mailing
list](https://groups.google.com/forum/#!forum/clojure-neo4j)

Let us know what was unclear or what has not been covered. Maybe you
do not like the guide style or grammar or discover spelling
mistakes. Reader feedback is key to making the documentation better.
