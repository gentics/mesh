#!/bin/bash

echo "Using includes: $1"
tests=$(paste -sd "," $1)

echo "Running tests: $tests"

time mvn -fae -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -B -e -pl '!ferma,!demo,!doc,!performance-tests' test -Dtest=$tests -DfailIfNoTests=false