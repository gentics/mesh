#!/bin/bash

rm -rf target/test-plugins
mvn invoker:run@build-test-projects -Dskip.test-plugins=false
