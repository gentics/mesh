package com.gentics.mesh.enterprise.database.connector;

import org.hibernate.dialect.MariaDBDialect;
import org.mariadb.jdbc.Driver;

/**
 * MariaDB 10.3+ Database connector.
 */
public class MariaDBConnector extends AbstractDatabaseConnector<Driver, MariaDBDialect> {

	@Override
	public Class<Driver> getJdbcDriverClass() {
		return Driver.class;
	}

	@Override
	public Class<MariaDBDialect> getHibernateDialectClass() {
		return MariaDBDialect.class;
	}
}
