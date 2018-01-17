#!/bin/sh

lein run -m clojurewerkz.neocons.rest.passwords http://localhost:7474/ neo4j neo4j qwerty

if test "$TRAVIS_SECURE_ENV_VARS" = 'true'; then
  NEO4J_LOGIN=neo4j NEO4J_PASSWORD=qwerty lein test :travis
else
  NEO4J_LOGIN=neo4j NEO4J_PASSWORD=qwerty lein test :default
fi
