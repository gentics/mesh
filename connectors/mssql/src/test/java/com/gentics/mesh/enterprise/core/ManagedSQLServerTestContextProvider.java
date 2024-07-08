package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.core.ManagedBy;
import com.gentics.mesh.core.ManagedDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.database.connector.SQLServerConnector;

/**
 * Test context provider for using a managed MS SQL
 */
@ManagedBy(name = "mssql", connector = SQLServerConnector.class)
public class ManagedSQLServerTestContextProvider extends ManagedDatabaseTestContextProvider {

	@Override
	protected String getConnectionUrl() {
		return "jdbc:sqlserver://" + dbSettings.getHost() + ":" + dbSettings.getPort() + ";databaseName=";
	}

	@Override
	protected String getConnectionUrlExtraParams() {
		return ";encrypt=false";
	}

	@Override
	protected String getConnectionUsername() {
		return "SA";
	}
}
