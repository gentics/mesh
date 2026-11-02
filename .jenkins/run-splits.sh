#!/bin/bash

BASEDIR=$(dirname "$0")
cd $BASEDIR/..
echo "Using includes: $1"
tests=$(paste -sd "," $1 | sed 's/\.java//g' | sed 's/\//./g')

if [ -z "$tests" ] ; then
  echo "Did not collect valid test set. Set was empty."
  exit 10
fi

echo "Running tests: $tests"
jacoco=$2
echo "Using jacoco: $jacoco"
pl=""
testContextProviderClass=""
nomssql=true
if [ "$3" = "mssql" ]; then
   nomssql=false
   pl=":mesh-enterprise-database-connector-mssql"
   testContextProviderClass=" -DtestContextProviderClass=com.gentics.mesh.enterprise.core.ManagedSQLServerTestContextProvider"
fi
nooracle=true
if [ "$3" = "oracle" ]; then
   nooracle=false
   pl=":mesh-enterprise-database-connector-oracle"
   testContextProviderClass=" -DtestContextProviderClass=com.gentics.mesh.enterprise.core.ManagedOracleTestContextProvider"
fi
nopostgres=true
if [ "$3" = "postgres" ]; then
   nopostgres=false
   pl=":mesh-enterprise-database-connector-postgres"
   testContextProviderClass=" -DtestContextProviderClass=com.gentics.mesh.enterprise.core.ManagedPostgresTestContextProvider"
fi
nomariadb=true
if [ "$3" = "mariadb" ]; then
   nomariadb=false
   testContextProviderClass=" -DtestContextProviderClass=com.gentics.mesh.core.ManagedMariaDBTestContextProvider"
   pl=":mesh-database-connector-mariadb"
fi
nohsqlm=true
if [ "$3" = "hsqlm" ]; then
   nohsqlm=false
   testContextProviderClass=" -DtestContextProviderClass=com.gentics.mesh.core.HSQLMemoryTestContextProvider"
   pl=":mesh-database-connector-hsqldb"
fi
echo "Skipping databases: MSSQL=$nomssql, Oracle=$nooracle, PostgreSQL=$nopostgres, MariaDB=$nomariadb, HSQL InMemory=$nohsqlm"
# the MCP server is not a dependency of any connector, add it explicitly so its tests are part of the splits
time mvn -pl $pl,:mesh-mcp-server -am -fae -U -Dsurefire.excludedGroups=com.gentics.mesh.test.category.FailingTests,com.gentics.mesh.test.category.ClusterTests -Dmaven.javadoc.skip=true -Dskip.cluster.tests=true -Dmaven.test.failure.ignore=true -Dmesh.container.image.prefix=docker.gentics.com/ -B -e test -Dtest=$tests -DfailIfNoTests=false -Djacoco.skip=$jacoco -Dskip.mssql.tests=$nomssql -Dskip.oracle.tests=$nooracle -Dskip.postgres.tests=$nopostgres -Dskip.mariadb.tests=$nomariadb -Dskip.hsqlmemory.tests=$nohsqlm -Dmesh.testdb.manager.host=localhost -Dmesh.testdb.manager.port=8080 $testContextProviderClass | ts "$4 [%Y-%m-%d %H:%M:%S]"
