package com.gentics.mesh.enterprise.database.connector;

import static com.gentics.mesh.hibernate.util.HibernateUtil.renderColumn;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.SqlServerUnicodeDialect;
import com.gentics.mesh.enterprise.server.license.LicenseService;
import com.gentics.mesh.parameter.PagingParameters;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;

/**
 * Microsoft SQL Server database connector for Gentics Mesh 3.x.
 */
public class SQLServerConnector extends AbstractDatabaseConnector<SQLServerDriver, SqlServerUnicodeDialect> {

	public SQLServerConnector(EnterpriseMeshOptions options) {
		super(options);
		LicenseService licenseService = new LicenseService(options);
		if (!licenseService.isLicenseSufficient()) {
			throw new IllegalStateException("Microsoft SQL Server database connector requires a valid license!");
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

	@Override
	public String installPaging(String sqlQuery, String myAlias, PagingParameters pagingInfo) {
		sqlQuery = super.installPaging(sqlQuery, myAlias, pagingInfo);
		if (sqlQuery.contains("order by @@version")) {
			// MSSQL requires ordering for pagination, but gets mad on a special case of DISTINCT...ORDER BY @@version.
			sqlQuery = sqlQuery.replace("order by @@version", "order by " + myAlias + "." + renderColumn(CommonContentColumn.DB_UUID));
		}
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 1000;
	}
}
