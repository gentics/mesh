#!/bin/bash

SID=$(curl -v --silent  http://joe1:test123@localhost:8080/api/v1/nav/get 2>&1 | grep "Set-Cookie" | sed -e 's/.*cailun\.session=\(.*\);.*/\1/')

ab -n 10000 -c 10 -A joe1:test123  -C cailun.session=$SID http://localhost:8080/api/v1/nav
