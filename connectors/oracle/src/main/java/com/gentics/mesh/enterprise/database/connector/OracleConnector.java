package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.OracleLongTextDialect;
import com.gentics.mesh.enterprise.server.license.LicenseService;

import oracle.jdbc.driver.OracleDriver;

/**
 * Oracle database connector for Gentics Mesh
 */
public class OracleConnector extends AbstractDatabaseConnector<OracleDriver, OracleLongTextDialect> {

	public OracleConnector(EnterpriseMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("This database connector requires a license!");
		}
	}

	@Override
	public Class<OracleDriver> getJdbcDriverClass() {
		return OracleDriver.class;
	}

	@Override
	public Class<OracleLongTextDialect> getHibernateDialectClass() {
		return OracleLongTextDialect.class;
	}
}
