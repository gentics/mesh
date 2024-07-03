package com.gentics.mesh.enterprise.database.connector;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.HSQLDialect;
import org.hsqldb.jdbcDriver;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;

/**
 * HSQLDB Mesh database connector
 */
public class HSQLDBConnector extends AbstractEnterpriseDatabaseConnector {

	public HSQLDBConnector(EnterpriseMeshOptions options) {
		super(options);
	}

	@Override
	public int getQueryParametersCountLimit() {
		return Short.MAX_VALUE;
	}

	@Override
	public String renderNonContentColumn(String column) {
		return column;
	}

	@Override
	public String getDummyComparison(Map<String, Object> params, boolean mustPass) {
		return StringUtils.EMPTY;
	}

	@Override
	protected String getDefaultDriverClassName() {
		return jdbcDriver.class.getCanonicalName();
	}

	@Override
	protected String getDefaultDialectClassName() {
		// TODO Auto-generated method stub
		return HSQLDialect.class.getCanonicalName();
	}
}
