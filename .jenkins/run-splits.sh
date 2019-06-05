#!/bin/bash

echo "Using includes: $1"
tests=$(paste -sd "," $1 | sed 's/\.java//g' | sed 's/\//./g')

echo "Running tests: $tests"


time mvn -X -fae -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -B -e -pl '!ferma,!demo,!doc,!performance-tests' test -Dtest=$tests -DfailIfNoTests=false