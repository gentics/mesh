package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.SqlServerUnicodeDialect;
import com.gentics.mesh.enterprise.server.license.LicenseService;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;

/**
 * Microsoft SQL Server database connector for Gentics Mesh 3.x.
 */
public class SQLServerConnector extends AbstractDatabaseConnector<SQLServerDriver, SqlServerUnicodeDialect> {

	public SQLServerConnector(EnterpriseMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("This database connector requires a license!");
		}
	}

	@Override
	public Class<SQLServerDriver> getJdbcDriverClass() {
		return SQLServerDriver.class;
	}

	@Override
	public Class<SqlServerUnicodeDialect> getHibernateDialectClass() {
		return SqlServerUnicodeDialect.class;
	}
}
