package com.gentics.mesh.enterprise.database.connector;

import com.gentics.mesh.enterprise.hibernate.dialect.OracleLongTextDialect;

import oracle.jdbc.driver.OracleDriver;

/**
 * Oracle database connector for Gentics Mesh
 */
public class OracleConnector extends AbstractDatabaseConnector<OracleDriver, OracleLongTextDialect> {

	@Override
	public Class<OracleDriver> getJdbcDriverClass() {
		return OracleDriver.class;
	}

	@Override
	public Class<OracleLongTextDialect> getHibernateDialectClass() {
		return OracleLongTextDialect.class;
	}
}
