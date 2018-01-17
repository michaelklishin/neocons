#!/bin/sh

lein run -m clojurewerkz.neocons.rest.passwords http://localhost:7474/ neo4j neo4j qwerty
