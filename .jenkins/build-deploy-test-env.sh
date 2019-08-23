#!/bin/bash


docker build -t gtx-docker-jenkinsbuilds.docker.apa-it.at/gentics/jenkinsbuilds/mesh-slave:latest .
docker push gtx-docker-jenkinsbuilds.docker.apa-it.at/gentics/jenkinsbuilds/mesh-slave:latest
