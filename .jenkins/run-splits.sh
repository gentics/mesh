#!/bin/bash

BASEDIR=$(dirname "$0")
cd $BASEDIR/..
echo "Using includes: $1"
tests=$(paste -sd "," $1 | sed 's/\.java//g' | sed 's/\//./g')

if [ -z "$tests" ] ; then
  echo "Did not collect valid test set. Set was empty."
  exit 10
fi

echo "Running tests: $tests"
jacoco=$2
echo "Using jacoco: $jacoco"
nomariadb=true
if [ "$3" = "mariadb" ]; then
   nomariadb=false
fi
nohsqldb=true
if [ "$3" = "hsqlm" ]; then
   nohsqldb=false
fi
echo "Skipping databases: MariaDB=$nomariadb"
time mvn -fae -U -Dsurefire.excludedGroups=com.gentics.mesh.test.category.FailingTests,com.gentics.mesh.test.category.ClusterTests -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -Dmesh.container.image.prefix=docker.gentics.com/ -B -e -pl '!doc,!performance-tests' test -Dtest=$tests -DfailIfNoTests=false -Djacoco.skip=$jacoco -Dskip.mariadb.tests=$nomariadb -Dskip.hsqlmemory.tests=$nohsqldb -Dmesh.testdb.manager.host=localhost -Dmesh.testdb.manager.port=8080 | ts "$4 [%Y-%m-%d %H:%M:%S]"
