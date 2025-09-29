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
		return "sa";
	}

	@Override
	public Comparator<String> sortComparator() {
		return SQLServerTestUtils.SORT_COMPARATOR;
	}

	@Override
	public boolean isCaseSensitive() {
		// MSSQL is considered to be case insensitive (have a _CI_ collection), because when using a
		// case sensitive collation (_CS_), also the username sa and column names would be treated as case sensitive,
		// which will cause lot's of trouble
		return false;
	}
}
