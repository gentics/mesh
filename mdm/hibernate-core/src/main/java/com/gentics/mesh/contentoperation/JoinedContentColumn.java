package com.gentics.mesh.contentoperation;

import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;

/**
 * A temporary column that is fetched by joining the content table with some other table
 */
public enum JoinedContentColumn implements ContentColumn {

	BINARY_FILENAME(MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "binaryfieldref", "bin_0", "filename", "binary_filename", String.class),
	BINARY_FIELDKEY(MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "binaryfieldref", "bin_0", "fieldkey", "binary_fieldkey", String.class),
	S3_FILENAME(MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "s3binaryfieldref", "s3bin_0", "filename", "s3binary_filename", String.class),
	S3_FIELDKEY(MeshTablePrefixStrategy.TABLE_NAME_PREFIX + "s3binaryfieldref", "s3bin_0", "fieldkey", "s3binary_filekey", String.class);

	private final String columnAlias;
	private String tableName;
	private String tableAlias;
	private String columnName;
	private Class<?> javaClass;

	JoinedContentColumn(String tableName, String tableAlias, String columnName, String columnAlias, Class<?> javaClass) {
		this.tableName = tableName;
		this.tableAlias = tableAlias;
		this.columnName = columnName;
		this.columnAlias = columnAlias;
		this.javaClass = javaClass;
	}

	@Override
	public String getLabel() {
		return tableAlias + "." + columnName;
	}

	public String getTableName() {
		return tableName;
	}
	public String getTableAlias() {
		return tableAlias;
	}

	public String getColumnName() {
		return columnName;
	}

	public String getColumnAlias() {
		return columnAlias;
	}

	@Override
	public Class<?> getJavaClass() {
		return javaClass;
	}
}
