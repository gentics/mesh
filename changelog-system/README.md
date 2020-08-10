# Changelog System

The changelog system is responsible to apply graph database changes which may be required during an upgrade. Gentics Mesh will automatically process new changes and apply those. Executed changes will be internally stored as executed and thus only be executed once.

## How to add changes?

Each change is implemented using a java class. These classes are located within the `com.gentics.mesh.changelog.changes` package.

* A change must extend `AbstractChange` class and implement all needed methods.

* UUIDs for new changes can be generated using the `UUIDGenerator` class.

* Changes which require a reindex operation must override the `requiresReindex` method.

* The `apply()` method contains the main code for the change which can be used to modify the graph database. All operations will only use the low level tinkerpop API.

* Finished changes must be added to the bottom of the list of existing changes within the `ChangesList` class.

## Pitfalls

* Ensure that your change also works on older systems which may not contain the graph structures which were added in later versions. It is a good practice to check whether the needed vertices exist before actually proceeding with the change.

* Some schema model changes must also be applied to the JSON data which is stored within the schema / microschemaModel versions graph elements.
