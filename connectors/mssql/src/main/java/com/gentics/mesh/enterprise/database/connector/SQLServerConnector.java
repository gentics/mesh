package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.enterprise.hibernate.dialect.SqlServer2012UnicodeDialect;
import com.microsoft.sqlserver.jdbc.SQLServerDriver;

/**
 * Microsoft SQL Server database connector for Gentics Mesh 3.x.
 */
public class SQLServerConnector extends AbstractDatabaseConnector<SQLServerDriver, SqlServer2012UnicodeDialect> {

	@Override
	public Class<SQLServerDriver> getJdbcDriverClass() {
		return SQLServerDriver.class;
	}

	@Override
	public Class<SqlServer2012UnicodeDialect> getHibernateDialectClass() {
		return SqlServer2012UnicodeDialect.class;
	}
}
