package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.enterprise.hibernate.dialect.SqlServerUnicodeDialect;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;

/**
 * Microsoft SQL Server database connector for Gentics Mesh 3.x.
 */
public class SQLServerConnector extends AbstractDatabaseConnector<SQLServerDriver, SqlServerUnicodeDialect> {

	@Override
	public Class<SQLServerDriver> getJdbcDriverClass() {
		return SQLServerDriver.class;
	}

	@Override
	public Class<SqlServerUnicodeDialect> getHibernateDialectClass() {
		return SqlServerUnicodeDialect.class;
	}
}
