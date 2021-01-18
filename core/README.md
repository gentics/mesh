# Gentics Mesh - Core

The core contains the actual main business logic implementation.

## MDM

* Module must be made clean from OrientDB, MADL, Ferma Code
* OrientDB specific Code (like e.g. com.gentics.mesh.core.data.impl.ProjectImpl or consistency checks) must be moved to mdm/orientdb-*
* Business code, which currently uses classes like com.gentics.mesh.core.data.impl.ProjectImpl must be refactored to use com.gentics.mesh.core.data.project.HibProject instead
* Move REST API endpoint specific code (e.g. UserEndpoint) to `/verticles/rest` (once they are clean)

## Tests

Plugin tests may require pre-build test plugins in order to be executed locally. The build for those plugins can be invoked using the `build-test-plugins.sh` script.
