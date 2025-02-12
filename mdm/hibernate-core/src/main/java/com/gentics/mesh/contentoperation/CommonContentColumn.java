package com.gentics.mesh.contentoperation;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * A collection of columns that are present in all content tables.
 */
public enum CommonContentColumn implements ContentColumn {

	DB_UUID(ContentColumnNames.DB_UUID, UUID.class),
	DB_VERSION(ContentColumnNames.DB_VERSION, Long.class),
	EDITOR_DB_UUID(ContentColumnNames.EDITOR_DB_UUID, UUID.class),
	EDITED(ContentColumnNames.EDITED, Timestamp.class),
	BUCKET_ID(ContentColumnNames.BUCKET_ID, Integer.class),
	SCHEMA_DB_UUID(ContentColumnNames.SCHEMA_DB_UUID, UUID.class),
	SCHEMA_VERSION_DB_UUID(ContentColumnNames.SCHEMA_VERSION_DB_UUID, UUID.class),
	LANGUAGE_TAG(ContentColumnNames.LANGUAGE_TAG, String.class),
	NODE(ContentColumnNames.NODE, UUID.class),
	CURRENT_VERSION_NUMBER(ContentColumnNames.CURRENT_VERSION_NUMBER, String.class); // stores the current version as major.minor string

	private final String label;
	private final Class<?> clazz;

	CommonContentColumn(String label, Class<?> clazz) {
		this.label = label;
		this.clazz = clazz;
	}

	@Override
	public String getLabel() {
		return label;
	}

	@Override
	public Class<?> getJavaClass() {
		return clazz;
	}
}
