#!/bin/sh

VERSION="3.3.1"
TARBALL="neo4j-server-$VERSION.tar.gz"

cd /tmp || exit
wget -O $TARBALL "http://dist.neo4j.org/neo4j-community-$VERSION-unix.tar.gz?edition=community&version=$VERSION&distribution=tarball&dlid=2803678"
tar zxf $TARBALL

cd "neo4j-community-$VERSION" || exit

./bin/neo4j start
sleep 5
