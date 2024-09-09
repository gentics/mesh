#!/bin/bash

# This will build the build environment image for nexus by extending the old build environment image and copying the new settings.xml file into it

docker build -f Dockerfile.nexus -t push.docker.gentics.com/docker-jenkinsbuilds/jenkinsbuilds/mesh-slave:java11-nexus .
docker push push.docker.gentics.com/docker-jenkinsbuilds/jenkinsbuilds/mesh-slave:java11-nexus
