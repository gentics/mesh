# Testing

- Changes must include tests (per `.github/CONTRIBUTING.md`).
- Prefer Test Driven Development: write a failing test first, then make the fix that turns it green.

## Test categories

Markers in `tests/common/src/main/java/com/gentics/mesh/test/category/` gate the CI suites:

- `FailingTests` — known-flaky/failing tests, excluded from the main run.
- `ClusterTests` — run only in the dedicated cluster-test stage.
- `PluginTests`, `NativeGraphQLFilterTests` — specialized suites.

## Where tests live and how they run

Most tests are in `src/main/java` of the `tests/tests-*` modules. They have no database of their
own: the connector modules run them via surefire `dependenciesToScan`
(`connectors/hsqldb/pom.xml`, `connectors/mariadb/pom.xml`). Running a connector without `-Dtest`
therefore runs the whole core suite — always narrow it.

Both connectors skip their tests by default (`skip.hsqlmemory.tests` / `skip.mariadb.tests` are
`true` in the root `pom.xml`), so the skip flag must be switched off explicitly.

`tests-mesh-core` builds the test plugins under `src/test/plugins` with `maven-invoker-plugin`
unless `-Dskip.test-plugins=true` is passed. Those builds need `mesh-plugin-bom` installed in the
local repository and fail otherwise. Pass `-Dskip.test-plugins=true` unless the test under change
is a `PluginTests` test; in that case install the plugin modules first (as CI does) and leave the
flag off.

### HSQLDB — preferred

RAM-based database context, fast and resource-saving:

```
mvn -pl :mesh-database-connector-hsqldb -am -Dskip.hsqlmemory.tests=false -Dskip.test-plugins=true \
    -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false test
```

### MariaDB

Run it only after the change passes on HSQLDB, or when the task explicitly asks for the MariaDB
context. It needs Docker; start and stop the database containers around the run, as CI does
(`Jenkinsfile.split`):

```
mvn -pl :mesh-database-connector-mariadb docker:start -Dskip.mariadb.tests=false
mvn -pl :mesh-database-connector-mariadb -am -Dskip.mariadb.tests=false -Dskip.test-plugins=true \
    -Dtest=<TestClass> -Dsurefire.failIfNoSpecifiedTests=false test
mvn -pl :mesh-database-connector-mariadb docker:stop -Dskip.mariadb.tests=false
```

## Full suite

Full local verification mirrors CI's main test run. Never run it unless explicitly requested — it
falls heavy on system resources:

```
mvn -Dsurefire.excludedGroups=com.gentics.mesh.test.category.FailingTests,com.gentics.mesh.test.category.ClusterTests -Dmaven.javadoc.skip=true test
```
