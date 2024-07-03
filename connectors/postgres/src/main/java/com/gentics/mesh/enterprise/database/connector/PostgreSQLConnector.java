package com.gentics.mesh.enterprise.database.connector;

import java.util.Map;

import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.PostgresDuplicationAwareDialect;
import com.gentics.mesh.hibernate.util.HibernateUtil;

/**
 * PostgreSQL database connector for Gentics Mesh
 */
public class PostgreSQLConnector extends AbstractEnterpriseDatabaseConnector {

	public PostgreSQLConnector(EnterpriseMeshOptions options) {
		super(options);
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 32767;
	}

	@Override
	public String getDummyComparison(Map<String, Object> params, boolean mustPass) {
		Boolean value = Boolean.valueOf(mustPass);
		String trueParam = HibernateUtil.makeParamName(value);
		params.put(trueParam, value);
		return " AND :" + trueParam + " ";
	}

	@Override
	protected String getDefaultDriverClassName() {
		return "org.postgresql.Driver";
	}

	@Override
	protected String getDefaultDialectClassName() {
		return PostgresDuplicationAwareDialect.class.getCanonicalName();
	}
}
