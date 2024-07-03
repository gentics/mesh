package com.gentics.mesh.enterprise.database.connector;

import java.util.Map;

import com.gentics.mesh.contentoperation.CommonContentColumn;
import com.gentics.mesh.enterprise.etc.config.EnterpriseMeshOptions;
import com.gentics.mesh.enterprise.hibernate.dialect.SqlServerUnicodeDialect;
import com.gentics.mesh.hibernate.util.HibernateUtil;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Microsoft SQL Server database connector for Gentics Mesh 3.x.
 */
public class SQLServerConnector extends AbstractEnterpriseDatabaseConnector {

	public SQLServerConnector(EnterpriseMeshOptions options) {
		super(options);
	}

	@Override
	public String installPaging(String sqlQuery, String myAlias, PagingParameters pagingInfo) {
		sqlQuery = super.installPaging(sqlQuery, myAlias, pagingInfo);
		if (sqlQuery.contains("order by @@version")) {
			// MSSQL requires ordering for pagination, but gets mad on a special case of DISTINCT...ORDER BY @@version.
			sqlQuery = sqlQuery.replace("order by @@version", "order by " + myAlias + "." + renderColumn(CommonContentColumn.DB_UUID));
		}
		return sqlQuery;
	}

	@Override
	public int getQueryParametersCountLimit() {
		return 2048; //2100 is a lie
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
		return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
	}

	@Override
	protected String getDefaultDialectClassName() {
		return SqlServerUnicodeDialect.class.getCanonicalName();
	}
}
