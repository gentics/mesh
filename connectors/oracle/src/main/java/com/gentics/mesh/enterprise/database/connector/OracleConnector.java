package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.enterprise.hibernate.dialect.Oracle12cLongTextDialect;

import oracle.jdbc.driver.OracleDriver;

/**
 * Oracle database connector for Gentics Mesh
 */
public class OracleConnector extends AbstractDatabaseConnector<OracleDriver, Oracle12cLongTextDialect> {

	static {
		MAIN_CLASS_NAME = OracleConnector.class.getCanonicalName();
	}

	@Override
	public Class<OracleDriver> getJdbcDriverClass() {
		return OracleDriver.class;
	}

	@Override
	public Class<Oracle12cLongTextDialect> getHibernateDialectClass() {
		return Oracle12cLongTextDialect.class;
	}
}
