# Gentics Mesh - MDM OrientDB Wrapper

The `mdm/orientdb-wrapper` module contains the implementation of the `mdm/orientdb-api` and the DAO implementations.

Tasks:

* Merge orientdb-api and orientdb-wrapper contents into `mdm/orientdb` module.
* Replace `****DaoWrapper` interfaces with `****Dao` interfaces. The methods must be moved to the `***Dao` interface within the `mdm/api` module once all graph dependencies within the `Hib***` interfaces have been removed.
