package com.gentics.mesh.enterprise.database.connector;

import org.postgresql.Driver;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.PostgresDuplicationAwareDialect;
import com.gentics.mesh.enterprise.server.license.LicenseService;

/**
 * PostgreSQL database connector for Gentics Mesh
 */
public class PostgreSQLConnector extends AbstractDatabaseConnector<Driver, PostgresDuplicationAwareDialect> {

	public PostgreSQLConnector(EnterpriseMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("PostgreSQL database connector requires a valid license!");
		}
	}

	@Override
	public Class<Driver> getJdbcDriverClass() {
		return Driver.class;
	}

	@Override
	public Class<PostgresDuplicationAwareDialect> getHibernateDialectClass() {
		return PostgresDuplicationAwareDialect.class;
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 2048; // 2100 is a lie
	}
}
