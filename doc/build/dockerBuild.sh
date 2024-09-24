#!/bin/bash

BRANCH=$(git branch --show-current)
SCRIPT=$(realpath $0)
BASEDIR=$(dirname $SCRIPT)
ROOT=$(realpath $BASEDIR/..)
CONTENTROOT=$ROOT/src/main/hugo

echo "Generating HTML out of: " $ROOT

# Fetch examples
EXAMPLEDIR=$CONTENTROOT/content/examples
if [ -n $EXAMPLEDIR ] ; then
  git clone --depth 1 --single-branch -- https://github.com/gentics/mesh-plugin-examples $EXAMPLEDIR
fi

# Fetch Mesh Enterprise, if available
ENTERPRISEDIR=$ROOT/../../mesh-enterprise
if [ -e $ENTERPRISEDIR ] ; then
  cp -R $ENTERPRISEDIR/CHANGELOG.adoc $CONTENTROOT/content/docs/sql-changelog.asciidoc
  cp -R $ENTERPRISEDIR/ENTERPRISE-CHANGELOG.adoc $CONTENTROOT/content/docs/enterprise-changelog.asciidoc
  cp -R $ENTERPRISEDIR/changelog-2.adoc-include $CONTENTROOT/content/docs/sql-changelog-2.adoc-include
  cp -R $ENTERPRISEDIR/changelog-3.adoc-include $CONTENTROOT/content/docs/enterprise-changelog-3.adoc-include
fi

# Copy parts in place
cp -R $ROOT/src/main/docs/generated/api $CONTENTROOT/content/docs/api
cp -R $ROOT/src/main/docs/generated/search $CONTENTROOT/content/docs/search
cp -R $ROOT/src/main/docs/generated/models $CONTENTROOT/content/docs/examples/models
cp -R $ROOT/src/main/docs/generated/tables $CONTENTROOT/content/docs/examples/tables
cp -R $ROOT/../CHANGELOG.adoc $CONTENTROOT/content/docs/changelog.asciidoc
cp -R $ROOT/../LTS-CHANGELOG.adoc $CONTENTROOT/content/docs/lts-changelog.asciidoc

docker run -v $CONTENTROOT:/site -v $ROOT/target/html:/site/docs --user 1000:1000 --rm gentics/hugo-asciidoctor /site/scripts/build.sh

# Cleanup
rm -rf $CONTENTROOT/content/docs/api
rm -rf $CONTENTROOT/content/docs/search
rm -rf $CONTENTROOT/content/docs/examples/models
rm -rf $CONTENTROOT/content/docs/examples/tables
rm -rf $CONTENTROOT/content/docs/changelog.asciidoc
rm -rf $CONTENTROOT/content/docs/lts-changelog.asciidoc
rm -rf $CONTENTROOT/content/docs/sql-changelog.asciidoc
rm -rf $CONTENTROOT/content/docs/enterprise-changelog.asciidoc
rm -rf $CONTENTROOT/content/docs/sql-changelog-2.adoc-include
rm -rf $CONTENTROOT/content/docs/enterprise-changelog-3.adoc-include
rm -rf $EXAMPLEDIR
