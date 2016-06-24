#!/bin/bash

SCRIPT="`readlink -f $0`" 
BASEDIR="`dirname "$SCRIPT"`"

cd $BASEDIR

# define some colors to use for output
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
# kill and remove any running containers
cleanup () {
  docker-compose kill
  docker-compose rm -f
}
# catch unexpected failures, do cleanup and output an error message
trap 'cleanup ; printf "${RED}Tests Failed For Unexpected Reasons${NC}\n"' HUP INT QUIT PIPE TERM
# build and run the composed services
docker-compose build && docker-compose up -d
if [ $? -ne 0 ] ; then
  printf "${RED}Docker Compose Failed${NC}\n"
  exit -1
fi
# wait for the test service to complete and grab the exit code
TEST_EXIT_CODE=`docker wait integrationtests_tester_1`
# output the logs for the test (for clarity)
docker logs --tail=all integrationtests_tester_1
# inspect the output of the test and display respective message
if [ -z ${TEST_EXIT_CODE+x} ] || [ "$TEST_EXIT_CODE" -ne 0 ] ; then
  printf "${RED}Tests Failed${NC} - Exit Code: $TEST_EXIT_CODE\n"
else
  printf "${GREEN}Tests Passed${NC}\n"
fi
# call the cleanup function
cleanup
# exit the script with the same code as the test service code
exit $TEST_EXIT_CODE
