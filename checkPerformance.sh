#!/bin/bash

ENDPOINT=$1
curl -v http://joe1:test123@localhost:8080/$ENDPOINT

CONC=$2
SID=$(curl -v --silent  http://joe1:test123@localhost:8080/$ENDPOINT 2>&1 | grep "Set-Cookie" | sed -e 's/.*cailun\.session=\(.*\);.*/\1/')

ab -n 100000 -c $CONC -A joe1:test123  -C cailun.session=$SID http://localhost:8080/$ENDPOINT
#wrk -t 8  -d 10 -c 1000 -H "Cookie: cailun.session=$SID"  http://localhost:8080/$ENDPOINT

