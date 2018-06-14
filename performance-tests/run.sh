#!/bin/bash

export DOCKER_HOST=tcp://hyperion.cluster.gentics.com:2375
docker-compose build --no-cache
docker-compose up
