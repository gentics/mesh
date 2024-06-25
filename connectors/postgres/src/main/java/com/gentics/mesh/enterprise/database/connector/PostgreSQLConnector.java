package com.gentics.mesh.enterprise.database.connector;

import org.postgresql.Driver;

import com.gentics.mesh.enterprise.hibernate.dialect.PostgresDuplicationAwareDialect;

/**
 * PostgreSQL database connector for Gentics Mesh
 */
public class PostgreSQLConnector extends AbstractDatabaseConnector<Driver, PostgresDuplicationAwareDialect> {

	@Override
	public Class<Driver> getJdbcDriverClass() {
		return Driver.class;
	}

	@Override
	public Class<PostgresDuplicationAwareDialect> getHibernateDialectClass() {
		return PostgresDuplicationAwareDialect.class;
	}
}
