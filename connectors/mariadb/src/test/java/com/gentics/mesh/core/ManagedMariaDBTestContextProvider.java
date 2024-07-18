package com.gentics.mesh.core;

import com.gentics.mesh.database.connector.MariaDBConnector;

/**
 * Test context provider for using a managed MariaDB
 */
@ManagedBy(name = "mariadb", connector = MariaDBConnector.class)
public class ManagedMariaDBTestContextProvider extends ManagedDatabaseTestContextProvider implements MariaDBTestContextProviderBase {

}
