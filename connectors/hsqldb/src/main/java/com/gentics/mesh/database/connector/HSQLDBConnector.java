package com.gentics.mesh.database.connector;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.dialect.HSQLDialect;
import org.hsqldb.jdbcDriver;

import com.gentics.mesh.etc.config.HibernateMeshOptions;

/**
 * HSQLDB Mesh database connector
 */
public class HSQLDBConnector extends AbstractDatabaseConnector {

	public HSQLDBConnector(HibernateMeshOptions options) {
		super(options);
	}

	@Override
	public String getConnectionUrl() {
		return (StringUtils.isNotBlank(options.getStorageOptions().getDatabaseAddress()) 
				? ("jdbc:hsqldb:hsql://" + options.getStorageOptions().getDatabaseAddress() + "/" + options.getStorageOptions().getDatabaseName())
				: "jdbc:hsqldb:mem:tsg"
			);
	}

	@Override
	public int getQueryParametersCountLimit() {
		return Short.MAX_VALUE;
	}

	@Override
	public String renderNonContentColumn(String column) {
		return (column != null && column.contains("-")) ? renderColumnUnsafe(column, false) : column;
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
