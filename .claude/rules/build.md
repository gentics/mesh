# Build

Build only the affected modules and what they depend on, without running tests (tests are run
separately, see `testing.md`):

```
mvn -pl <module> -am -DskipTests -Dmaven.javadoc.skip=true clean install
```

A root-level `mvn clean install` runs the unit tests of the whole reactor — do not run it unless
explicitly requested.
