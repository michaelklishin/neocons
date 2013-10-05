#!/bin/sh

# travis-ci.org provides neo4j server but it will be disabled from starting on boot
# in the future
which neo4j && sudo neo4j start
sleep 3
