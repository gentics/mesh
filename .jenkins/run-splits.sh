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
time mvn -fae -Dsurefire.excludedGroups=com.gentics.mesh.test.category.FailingTests,com.gentics.mesh.test.category.ClusterTests -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -Dmesh.container.image.prefix=docker.apa-it.at/ -B -e -pl '!demo/default,!doc,!performance-tests' test -Dtest=$tests -DfailIfNoTests=false -Djacoco.skip=$jacoco | ts "$3 [%Y-%m-%d %H:%M:%S]"
