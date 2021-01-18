# Gentics Mesh - MDM API

The `mdm/api` module contains the MDM API which can be used to create DAO implementations and Domain classes.

Tasks:

* Move `***DaoWrapper` code into `***Dao` interfaces and remove `***DaoWrapper` interfaces.
* Combine `***Dao` and `***DaoActions` interfaces (DAOActions contain common DAO methods like loading entities, ...)
* Rename domain interfaces: `HibUser` -> `MDMUser`
