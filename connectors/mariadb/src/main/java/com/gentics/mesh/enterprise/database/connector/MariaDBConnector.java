package com.gentics.mesh.enterprise.database.connector;

import org.mariadb.jdbc.Driver;

import com.gentics.mesh.enterprise.dialect.MariaDB103UuidAwareDialect;

/**
 * MariaDB 10.3+ Database connector.
 */
public class MariaDBConnector extends AbstractDatabaseConnector<Driver, MariaDB103UuidAwareDialect> {

	@Override
	public Class<Driver> getJdbcDriverClass() {
		return Driver.class;
	}

	@Override
	public Class<MariaDB103UuidAwareDialect> getHibernateDialectClass() {
		return MariaDB103UuidAwareDialect.class;
	}
}
