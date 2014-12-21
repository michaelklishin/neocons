#!/bin/sh

export NEO4J_AUTH_TOKEN=$(lein run -m clojurewerkz.neocons.rest.password -n qwerty)

lein2 test :travis