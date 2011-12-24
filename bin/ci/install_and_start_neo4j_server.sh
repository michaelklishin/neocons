#!/bin/sh

neo4_archive_location=/tmp/neo4j.tar.gz

wget http://dist.neo4j.org/neo4j-community-1.6.M02-unix.tar.gz -O $neo4_archive_location
tar zxf $neo4_archive_location
cd /tmp
gunzip $neo4_archive_location
tar xf neo4j.tar
cd neo4j-community-1.6.M02
./bin/neo4j start
sleep 3
