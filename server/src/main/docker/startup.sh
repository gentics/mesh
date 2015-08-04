#!/bin/bash

function startServices() {
  echo "Starting services"
  service ssh start
  service apache2 start
  cd /opt/mesh
  java -jar /opt/mesh/mesh-starter.jar
}

function dontExit() {
  # Don't terminate
  while true ; do
    sleep 1
  done
}

startServices
dontExit
