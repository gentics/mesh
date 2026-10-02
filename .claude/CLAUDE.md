# Gentics Mesh — Guidelines

This describes the generic workflow for resolving a task (bug fix, enhancement, or feature) in this repository.

## 1. General guidelines

Follow the general CMP guidelines at `.claude` folder of the system user.
Usage of Maven over Gentics Mesh requires `JAVA_HOME` environment variable to be set to the proper JDK of version 21 or above. Check, if it is already set, and if no, set it for the current session.

## 2. Add or update tests

Prefer following Test Driven Development methodology, e.g. writing a failing test first, then make the fix to make it passing, as per guidelines from `.claude/skills/` folder of a current user.
If following TDD is not possible according to the task investigation results, propose the user the alternative solution and wait for the explicit permission to proceed or improve.

- Changes must include tests (per `.github/CONTRIBUTING.md`).
- Be aware of test category markers used to gate CI suites (in `tests/common/src/main/java/com/gentics/mesh/test/category/`):
  - `FailingTests` — known-flaky/failing tests, excluded from the main run.
  - `ClusterTests` — run only in the dedicated cluster-test stage.
  - `PluginTests`, `NativeGraphQLFilterTests` — specialized suites.
- Run the relevant tests locally before committing, e.g.:
  ```
  mvn -pl <module> -am test
  ```
  where `<module>` is limited to the one of the following modules:
  - `hsqldb` — preferred, RAM based database context, fast and resource-saving
  - `mariadb` — has to be run either after the fix is successfully tested with `hsqldb`, or the task suggests MariaDB context explicitly.
  
  Full local verification mirrors CI's main test run. Never run it, unless explicitly requested, as it falls heavy on system resources:
  ```
  mvn -Dsurefire.excludedGroups=com.gentics.mesh.test.category.FailingTests,com.gentics.mesh.test.category.ClusterTests -Dmaven.javadoc.skip=true test
  ```
  
## 3. Implement the change

- Follow existing code conventions in the surrounding module rather than introducing new patterns.
- Use the Eclipse Java formatter config at `https://github.com/gentics/mesh/blob/dev/eclipse_formatter.xml` for code formatting.

## 4. Verify before submitting

- Build the affected modules: `mvn clean install` (add `-Dmaven.javadoc.skip=true` to skip javadoc generation locally).
- Confirm the changelog entry is present for any user-facing change.

## 5. Changelog

The changelog entries are placed into `changelog/src/changelog/entries/<year>/<month>` folder. Create the `<year>/<month>` segments for the current year and month, respectively, if those are missing.
The allowed entry types are:
 - `enhancement` — for the new features. Those usually come with ticket IDs named `GPU-*`.
 - `security` — for the updates of dependency versions, when an existing dependency is evidenced to have a security vulnerability. The ticket ID might be either `SUP-*` or, in rare cases, which has to be confirmed by a user, `GPU-*`.
 - `documentation` — for the documentation-only fixes, where no code has been changed, ticket ID is mostly `SUP-*`.
 - `bugfix` — for all the other cases, with ticket IDs marked as `SUP-*`.
- `manualchange` — the fix brings a breaking change to the user data, so the migration manual has to be provided along, independently of the ticket ID.
- `optional-manualchange` — the fix may bring a breaking change to the user data, so its usecase description and migration manual have to be provided along, independently of the ticket ID.
