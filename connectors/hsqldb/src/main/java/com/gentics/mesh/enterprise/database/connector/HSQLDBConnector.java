package com.gentics.mesh.enterprise.database.connector;

import org.hibernate.dialect.HSQLDialect;
import org.hsqldb.jdbcDriver;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.server.license.LicenseService;

/**
 * HSQLDB Mesh database connector
 */
public class HSQLDBConnector extends AbstractDatabaseConnector<jdbcDriver, HSQLDialect> {

	public HSQLDBConnector(EnterpriseMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("This database connector requires a license!");
		}
	}

	@Override
	public Class<jdbcDriver> getJdbcDriverClass() {
		return jdbcDriver.class;
	}

	@Override
	public Class<HSQLDialect> getHibernateDialectClass() {
		return HSQLDialect.class;
	}
}
