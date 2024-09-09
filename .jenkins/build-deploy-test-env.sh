#!/bin/bash

if [ -z $1 ] ; then
  TAG=latest
else
  TAG=$1
fi

docker build -t push.docker.gentics.com/docker-jenkinsbuilds/jenkinsbuilds/mesh-slave:$TAG .
docker push push.docker.gentics.com/docker-jenkinsbuilds/jenkinsbuilds/mesh-slave:$TAG
