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

echo "Building raml"
OUT=$ROOT/static/docs/api
if [ -e $OUT/index.html ]; then
  rm -rf $OUT/index.html 
fi
mkdir -p $OUT

cp -R /site/assets/* $ROOT/static/docs/

$ROOT/scripts/updateGithub.sh

cd $ROOT/themes/gentics
npx raml2html -i $ROOT/content/docs/api/api-docs.raml -o $OUT/index.html -t $ROOT/themes/gentics/raml/template.nunjucks

echo "Building with hugo"
cd $ROOT

hugo

chmod -R a+rwx $ROOT/static/docs
