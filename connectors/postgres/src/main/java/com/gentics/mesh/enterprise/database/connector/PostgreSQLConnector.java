package com.gentics.mesh.enterprise.database.connector;

import org.hibernate.dialect.PostgreSQL95Dialect;
import org.postgresql.Driver;

/**
 * PostgreSQL database connector for Gentics Mesh
 */
public class PostgreSQLConnector extends AbstractDatabaseConnector<Driver, PostgreSQL95Dialect> {

	@Override
	public Class<Driver> getJdbcDriverClass() {
		return Driver.class;
	}

	@Override
	public Class<PostgreSQL95Dialect> getHibernateDialectClass() {
		return PostgreSQL95Dialect.class;
	}

}
