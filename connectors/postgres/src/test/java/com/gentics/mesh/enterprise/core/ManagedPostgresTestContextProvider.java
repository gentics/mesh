package com.gentics.mesh.enterprise.core;

import com.gentics.mesh.core.ManagedBy;
import com.gentics.mesh.core.ManagedDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.database.connector.PostgreSQLConnector;

/**
 * Test context provider for using a managed Postgres
 */
@ManagedBy(name = "postgres", connector = PostgreSQLConnector.class)
public class ManagedPostgresTestContextProvider extends ManagedDatabaseTestContextProvider {

	@Override
	protected String getConnectionUrlExtraParams() {
		return "?autosave=always&currentSchema=" + dbSettings.getSchema();
	}
}
