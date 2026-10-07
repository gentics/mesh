# Gentics Mesh — Guidelines

Generic workflow for resolving a task (bug fix, enhancement, or feature) in this repository.
Topic rules live in `.claude/rules/`: `build.md`, `testing.md`, `code-style.md`, `changelog.md`.

Changelog Folder: changelog/src/changelog/entries/<year>/<month>

## Java

Maven builds of Gentics Mesh require `JAVA_HOME` to point to a JDK of version 21 or above.
Check whether it is already set; if not, set it for the current session.

## Order of work

1. Add or update tests — see `rules/testing.md`.
2. Implement the change — see `rules/code-style.md`.
3. Verify — see `rules/build.md`.
4. Add a changelog entry for any user-facing change — see `rules/changelog.md`.
