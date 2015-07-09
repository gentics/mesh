#!/bin/bash

ENDPOINT=$1
curl -v http://joe1:test123@localhost:8080/$ENDPOINT

CONC=$2
COUNT=$3
SID=$(curl -v -H 'Accept: application/json' --silent  http://joe1:test123@localhost:8080/$ENDPOINT 2>&1 | grep "Set-Cookie" | sed -e 's/.*mesh\.session=\(.*\);.*/\1/')
echo "SID: $SID" 
ab -n $COUNT -c $CONC -A joe1:test123  -C mesh.session=$SID http://localhost:8080/$ENDPOINT
#wrk -t 8  -d 10 -c 1000 -H "Cookie: mesh.session=$SID"  http://localhost:8080/$ENDPOINT

