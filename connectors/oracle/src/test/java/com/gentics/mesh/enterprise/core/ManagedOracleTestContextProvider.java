package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.core.ManagedBy;
import com.gentics.mesh.core.ManagedDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.database.connector.OracleConnector;

/**
 * Test context provider for using a managed Oracle
 */
@ManagedBy(name = "oracle", connector = OracleConnector.class)
public class ManagedOracleTestContextProvider extends ManagedDatabaseTestContextProvider {

	@Override
	protected String getConnectionUrl() {
		return "jdbc:oracle:thin:@" + dbSettings.getHost() + ":" + dbSettings.getPort() + "/";
	}

	@Override
	protected String getConnectionUsername() {
		return dbSettings.getSchema();
	}
}
