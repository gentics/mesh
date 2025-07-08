package com.gentics.mesh.enterprise.core;

import java.util.Comparator;

import com.gentics.mesh.core.ManagedBy;
import com.gentics.mesh.core.ManagedDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.dagger.DaggerEnterpriseHibernateMeshComponent;
import com.gentics.mesh.enterprise.database.connector.SQLServerConnector;

/**
 * Test context provider for using a managed MS SQL
 */
@ManagedBy(name = "mssql", connector = SQLServerConnector.class)
public class ManagedSQLServerTestContextProvider extends ManagedDatabaseTestContextProvider {

	public ManagedSQLServerTestContextProvider() {
		super(DaggerEnterpriseHibernateMeshComponent.builder());
	}

	@Override
	protected String getConnectionUrlExtraParams() {
		return "encrypt=false";
	}

	@Override
	protected String getConnectionUsername() {
		return "SA";
	}

	@Override
	public Comparator<String> sortComparator() {
		return SQLServerTestUtils.SORT_COMPARATOR;
	}
}
