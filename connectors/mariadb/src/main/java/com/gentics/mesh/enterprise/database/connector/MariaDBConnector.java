package com.gentics.mesh.enterprise.database.connector;

import org.hibernate.dialect.MariaDBDialect;
import org.mariadb.jdbc.Driver;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;

/**
 * MariaDB 10.3+ Database connector.
 */
public class MariaDBConnector extends AbstractDatabaseConnector<Driver, MariaDBDialect> {

	public MariaDBConnector(EnterpriseMeshOptions options) {
		super(options);
	}

	@Override
	public Class<Driver> getJdbcDriverClass() {
		return Driver.class;
	}

	@Override
	public Class<MariaDBDialect> getHibernateDialectClass() {
		return MariaDBDialect.class;
	}
}
