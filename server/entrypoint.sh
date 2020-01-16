#!/bin/sh

set -o nounset
set -o errexit

UID=$(id -u)
if ! whoami &> /dev/null; then
  if [ -w /etc/passwd ]; then
    echo "Creating mesh user uid to $UID"
    echo "${USER_NAME:-mesh}:x:$UID:0:${USER_NAME:-mesh} user:/mesh:/sbin/nologin" >> /etc/passwd
  else
    echo "Could not write to /etc/passwd in order to create user for id $UID"
    exit 10
  fi
fi

exec "$@"