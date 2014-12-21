#!/bin/sh

export NEO4J_AUTH_TOKEN=$(lein run -m clojurewerkz.neocons.rest.password -p qwerty -u neo4j)

lein2 test :travis