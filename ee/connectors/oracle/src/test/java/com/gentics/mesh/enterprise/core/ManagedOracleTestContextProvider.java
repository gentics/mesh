package com.gentics.mesh.enterprise.core;

import java.util.Comparator;
import java.util.Locale;

import com.gentics.mesh.core.ManagedBy;
import com.gentics.mesh.core.ManagedDatabaseTestContextProvider;
import com.gentics.mesh.enterprise.dagger.DaggerEnterpriseHibernateMeshComponent;
import com.gentics.mesh.enterprise.database.connector.OracleConnector;

/**
 * Test context provider for using a managed Oracle
 */
@ManagedBy(name = "oracle", connector = OracleConnector.class)
public class ManagedOracleTestContextProvider extends ManagedDatabaseTestContextProvider {
	static {
		// we set the default locale to english, because the jdbc driver depends on this when setting the sort collation
		Locale.setDefault(Locale.ENGLISH);
	}

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

	@Override
	public Double getMinAllowedDouble() {
		return OracleTestUtils.getMinAllowedDouble();
	}

	@Override
	public Double getMaxAllowedDouble() {
		return OracleTestUtils.getMaxAllowedDouble();
	}
}
