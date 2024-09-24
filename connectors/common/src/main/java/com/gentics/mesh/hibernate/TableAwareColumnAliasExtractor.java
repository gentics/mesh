package com.gentics.mesh.hibernate;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hibernate.dialect.ColumnAliasExtractor;

/**
 * Consider table name in a result set as a de-duplication mechanics.
 */
public class TableAwareColumnAliasExtractor implements ColumnAliasExtractor {

	public static final ColumnAliasExtractor INSTANCE = new TableAwareColumnAliasExtractor();

	@Override
	public String extractColumnAlias(ResultSetMetaData metaData, int position) throws SQLException {
		return metaData.getTableName(position) + "_" + metaData.getColumnLabel(position) + "_" + position;
	}
}
