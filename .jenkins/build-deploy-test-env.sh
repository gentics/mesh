#!/bin/bash

if [ -z $1 ] ; then
  TAG=latest
else
  TAG=$1
fi

docker build -t gtx-docker-jenkinsbuilds.docker.apa-it.at/gentics/jenkinsbuilds/mesh-slave:$TAG .
docker push gtx-docker-jenkinsbuilds.docker.apa-it.at/gentics/jenkinsbuilds/mesh-slave:$TAG
