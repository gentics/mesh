# Changelog

Entries are placed into `changelog/src/changelog/entries/<year>/<month>`. Create the
`<year>/<month>` segments for the current year and month, respectively, if those are missing.

The allowed entry types are:

- `enhancement` — for the new features. Those usually come with ticket IDs named `GPU-*`.
- `security` — for the updates of dependency versions, when an existing dependency is evidenced to
  have a security vulnerability. The ticket ID might be either `SUP-*` or, in rare cases, which has
  to be confirmed by a user, `GPU-*`.
- `documentation` — for the documentation-only fixes, where no code has been changed, ticket ID is
  mostly `SUP-*`.
- `bugfix` — for all the other cases, with ticket IDs marked as `SUP-*`.
- `manualchange` — the fix brings a breaking change to the user data, so the migration manual has
  to be provided along, independently of the ticket ID.
- `optional-manualchange` — the fix may bring a breaking change to the user data, so its usecase
  description and migration manual have to be provided along, independently of the ticket ID.
