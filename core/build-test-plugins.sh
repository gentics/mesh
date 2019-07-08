#!/bin/bash

rm -rf target/test-plugins
mvn invoker:run@build-test-projects
