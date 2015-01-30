#!/bin/bash

SCRIPT=$(readlink -f $0)
BASEDIR=$(dirname "$SCRIPT")

PROJROOT=$BASEDIR/../../../

DEMODIR=$PROJROOT/../demo
TARGET=$PROJROOT/target
BUILDDIR=$TARGET/docker-tmpdir

#VERSION=$1
#DOCKERHOST=$2

#docker="docker -H $DOCKERHOST"

VERSION="0.1.0-SNAPSHOT"
docker="docker"

# Prepare build dir
mkdir -p $BUILDDIR
cp -ra $BASEDIR/* $BUILDDIR
cp $TARGET/cailun-starter-${VERSION}.jar $BUILDDIR/cailun-starter.jar
cp -ra $DEMODIR $BUILDDIR

# Invoke build
cd $BUILDDIR
$docker build --rm -t registry.office/cailun:$VERSION .
#$docker push registry.office/cailun:$VERSION

