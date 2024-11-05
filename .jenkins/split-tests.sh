#!/bin/bash

BASEDIR=$(dirname "$0")

echo "Generating batches for $1 splits"
cd $BASEDIR/..

rm includes*

echo
echo  "Collecting all tests"
mvn -Dstyle.color=never com.gentics.mesh:test-scanner-maven-plugin:scan-test-files | grep "TEST_CLASS:" | sed 's/TEST_CLASS: //g' | awk '!seen[$0]++' > alltests
tests=$(cat alltests | wc -l)

## DEBUG
tests=NodeMigrationEndpointClusteredTest

echo "Found $tests tests"
grep search alltests  > searchtests
grep -v search alltests  > nonsearchtests

echo 
echo "Generating splits"
split -a 2 -d -n l/$1 nonsearchtests includes-nonsearch-
split -a 2 -d -n l/$1 searchtests includes-search-
ls -l includes-*

if [ "$2" == "nosearch" ] ; then
  echo "Removing search tests"
  rm includes-search*
fi
TO=`expr $1 - 1`

echo "Combining splits"
for i in $(seq -w 00 $TO) ; do
  echo "Combining: $i"
  if [ -e includes-search-$i ] ; then
    cat includes-search-$i includes-nonsearch-$i > includes-$i
    rm includes-nonsearch-$i includes-search-$i
  else
    mv includes-nonsearch-$i  includes-$i
  fi
done


echo 
echo "Cleanup"
rm alltests 
rm searchtests
rm nonsearchtests
