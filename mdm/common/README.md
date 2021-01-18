# Gentics Mesh - MDM Common

The `mdm/common` module contains already cleaned code which does not reference OrientDB code.
The goal of this module is to be a placeholder container for `/common` and `/core` code which has no graph dependency. The contents of this module should at the end be move to or replace `/common`.
The `/common` module does currently still contain GraphDB specific code which needs to be refactored and moved to either `mdm/common`, `mdm/api` or `mdm/orientdb-*`.

## Tasks

* Attempt to move code to `/common`