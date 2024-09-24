#!/bin/sh

set -o nounset
set -o errexit

SCRIPT=$(realpath $0)
BASEDIR=$(dirname $SCRIPT)
ROOT=$BASEDIR/..

downloadGithub() {
    local url=$1
    local filename=$2
    TMP=/tmp/$filename.tmp
    rm $TMP || true
    wget $url -O $TMP && mv -f $TMP $ROOT/data/$filename || true
}

echo "Updating github information files"
downloadGithub https://api.github.com/repos/gentics/mesh github.json
downloadGithub https://api.github.com/repos/gentics/mesh/releases releases.json

LATEST=$(cat $ROOT/data/releases.json  | grep \"name\" |  sort -r | sed 's/.*:.\"\(.*\)\".*/\1/' | grep -v LTS  |  sort  -t "." -k1,1 -k2,2 -k3,3 -n -r | head -n 1)

echo "Latest version: $LATEST"
echo "{ \"version\": \"$LATEST\" }" > $ROOT/data/mesh.json
echo "{ \"latest\": \"$LATEST\" }" > $ROOT/static/api/updatecheck
