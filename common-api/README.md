# Gentics Mesh - Common API

This module contains internal API code which is not graph db specific.

Code from `/core` and `/common` was moved here since it does not require the `mdm` API.

## MDM 

* This module was created during the refactoring for MDM and therefore is already "clean"
* During further refactoring, additional "common API" code may be moved here
* It may be worth to keep the code in a dedicated module. If this is not wanted it could also be moved to `/mdm/common` or `/common`.
