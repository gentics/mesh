#!/bin/bash

function startServices() {
  echo "Starting services"
  service ssh start
  service apache2 start
  cd /opt/cailun
 #nohup java -jar /opt/cailun/cailun-starter.jar > /opt/cailun/cailun.log 2> /opt/cailun/cailun.err < /dev/null &
  java -jar /opt/cailun/cailun-starter.jar
}

function dontExit() {
  # Don't terminate
  while true ; do
    sleep 1
  done
}

startServices
dontExit
~                                                                                                                                                                      
~                                                                                                                                                                      
~                                                                                                                                                                      
~                                                                                                                                                                      
~                                                                                                                                                                      
~                                         
