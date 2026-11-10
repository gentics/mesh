#!/bin/sh

set -o nounset
set -o errexit

SCRIPT=$(realpath $0)
BASEDIR=$(dirname $SCRIPT)
ROOT=$(realpath $BASEDIR/..)

#ln -s $ROOT/import/mesh/doc/src/main/docs $DOCSDIR
#ln -s $ROOT/import/examples $EXAMPLEDIR
#ln -s $ROOT/import/mesh-enterprise/CHANGELOG.adoc $DOCSDIR/sql-changelog.asciidoc

echo "Building css"
cd $ROOT/themes/gentics
yarn install && yarn build

echo "Installing API documentation"
OUT=$ROOT/static/docs/api
if [ -e $OUT/index.html ]; then
  rm -rf $OUT/index.html 
fi
mkdir -p $OUT

cp -R /site/assets/* $ROOT/static/docs/

$ROOT/scripts/updateGithub.sh

# The API documentation page is generated from the OpenAPI spec by the doc module (OpenAPIHtmlGenerator)
mv $ROOT/content/docs/api/index.html $OUT/index.html

echo "Building with hugo"
cd $ROOT

hugo

chmod -R a+rwx $ROOT/static/docs
