package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

import com.gentics.mesh.core.ManagedBy;
import com.gentics.mesh.core.ManagedDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.dagger.DaggerEnterpriseHibernateMeshComponent;
import com.gentics.mesh.enterprise.database.connector.OracleConnector;

/**
 * Test context provider for using a managed Oracle
 */
@ManagedBy(name = "oracle", connector = OracleConnector.class)
public class ManagedOracleTestContextProvider extends ManagedDatabaseTestContextProvider {

	public ManagedOracleTestContextProvider() {
		super(DaggerEnterpriseHibernateMeshComponent.builder());
	}

	@Override
	protected String getConnectionUsername() {
		return dbSettings.getSchema();
	}

	@Override
	public Comparator<String> sortComparator() {
		return OracleTestUtils.SORT_COMPARATOR;
	}
}
