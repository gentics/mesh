#!/bin/bash

EXTRAVOL=""

MESHDIR=$PWD/../mesh
if [ -e $MESHDIR ] ; then
  EXTRAVOL="$EXTRAVOL -v $MESHDIR:/mesh"
fi

EXAMPLEDIR=$PWD/../mesh-plugin-examples
if [ -e $EXAMPLEDIR ] ; then
  EXTRAVOL="$EXTRAVOL -v $EXAMPLEDIR:/mesh-plugin-examples"
fi

docker run -it -p 1313:1313 -v $PWD/:/site $EXTRAVOL --rm gentics/hugo-asciidoctor ./site/scripts/build.sh $@
