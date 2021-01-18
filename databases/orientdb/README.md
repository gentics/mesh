# OrientDB Database module

This module contains OrientDB specific implementation of the database provider and transaction code.

## Update Process

1. Download and Unpack OrientDB community edition zip file

2. Copy studio zip to Mesh workspace
```
cp plugins/orientdb-studio-3.1.6.zip ~/workspaces/mesh2/mesh/databases/orientdb/src/main/resources/plugins/
```

3. Delete the old studio zip file in databases/orientdb/src/main/resources/plugins/

4. Open OrientDBClusterManager and update ORIENTDB_STUDIO_ZIP to point to the new file

5. Update OrientDB `orientdb.version` version property in bom/pom.xml

6. Add entry to changelog files

7. Run tests

## MDM

Should be integrated into mdm/orientdb-
