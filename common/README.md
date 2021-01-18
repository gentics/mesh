# Gentics Mesh - Common Implementation Module

This module contains common (internally used) classes which are required by various other modules.

## MDM

* This module still contains OrientDB specific code. The refactored code should be moved to `mdm/common` or `mdm/orientdb-*`.
* After everything is refactored, this module should completely be removed.

Example:
* class com.gentics.mesh.core.db.AbstractEdgeFrame must be moved to mdm/orientdb-*
