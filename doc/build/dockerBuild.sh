#!/bin/bash

BRANCH=$(git branch --show-current)
SCRIPT=$(realpath $0)
BASEDIR=$(dirname $SCRIPT)
ROOT=$(realpath $BASEDIR/..)
SRCCONTENTROOT=$ROOT/src/main/hugo
CONTENTROOT=$ROOT/target/hugo
HTMLROOT=$ROOT/target/html

echo "Generating HTML out of: " $ROOT

mkdir -p $CONTENTROOT
mkdir -p $HTMLROOT
cp -Rf $SRCCONTENTROOT/* $CONTENTROOT

# Fetch examples
EXAMPLEDIR=$CONTENTROOT/content/examples
if [ -n $EXAMPLEDIR ] ; then
  git clone --depth 1 --single-branch -- https://github.com/gentics/mesh-plugin-examples $EXAMPLEDIR
fi

# Fetch Mesh Enterprise, if available
ENTERPRISEDIR=$ROOT/../../mesh-enterprise
if [ -e $ENTERPRISEDIR ] ; then
  cp -Rf $ENTERPRISEDIR/CHANGELOG.adoc $CONTENTROOT/content/docs/sql-changelog.asciidoc
  cp -Rf $ENTERPRISEDIR/ENTERPRISE-CHANGELOG.adoc $CONTENTROOT/content/docs/enterprise-changelog.asciidoc
  cp -Rf $ENTERPRISEDIR/changelog-2.adoc-include $CONTENTROOT/content/docs/sql-changelog-2.adoc-include
  cp -Rf $ENTERPRISEDIR/changelog-3.adoc-include $CONTENTROOT/content/docs/enterprise-changelog-3.adoc-include
fi

# Copy parts in place
cp -Rf $ROOT/src/main/docs/generated/api $CONTENTROOT/content/docs/api
cp -Rf $ROOT/src/main/docs/generated/search $CONTENTROOT/content/docs/search
cp -Rf $ROOT/src/main/docs/generated/models $CONTENTROOT/content/docs/examples/models
cp -Rf $ROOT/src/main/docs/generated/tables $CONTENTROOT/content/docs/examples/tables
cp -Rf $ROOT/../CHANGELOG.adoc $CONTENTROOT/content/docs/changelog.asciidoc
cp -Rf $ROOT/../LTS-CHANGELOG.adoc $CONTENTROOT/content/docs/lts-changelog.asciidoc

docker run -v $CONTENTROOT:/site -v $HTMLROOT:/site/docs --user 1000:1000 --rm gentics/hugo-asciidoctor /site/scripts/build.sh
