#!/bin/sh

# GrapheneDB account needs a refresh
# if test "$TRAVIS_SECURE_ENV_VARS" = 'true'; then
#   NEO4J_LOGIN=neo4j NEO4J_PASSWORD=qwerty lein test :travis
# else
#   NEO4J_LOGIN=neo4j NEO4J_PASSWORD=qwerty lein test :default
# fi

NEO4J_LOGIN=neo4j NEO4J_PASSWORD=qwerty lein test :default
