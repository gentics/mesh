package com.gentics.mesh.enterprise.database.connector;

import org.hibernate.dialect.HSQLDialect;
import org.hsqldb.jdbcDriver;

/**
 * HSQLDB Mesh database connector
 */
public class HSQLDBConnector extends AbstractDatabaseConnector<jdbcDriver, HSQLDialect> {

	static {
		MAIN_CLASS_NAME = HSQLDBConnector.class.getCanonicalName();
	}

	@Override
	public Class<jdbcDriver> getJdbcDriverClass() {
		return jdbcDriver.class;
	}

	@Override
	public Class<HSQLDialect> getHibernateDialectClass() {
		return HSQLDialect.class;
	}
}
