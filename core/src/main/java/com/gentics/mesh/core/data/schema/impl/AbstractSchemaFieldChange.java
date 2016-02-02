package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.SchemaFieldChange;

/**
 * @see SchemaFieldChange
 */
public abstract class AbstractSchemaFieldChange extends AbstractSchemaChange implements SchemaFieldChange {

	private static final String FIELDNAME_KEY = "fieldName";

	@Override
	public String getFieldName() {
		return getProperty(FIELDNAME_KEY);
	}

	@Override
	public void setFieldName(String name) {
		setProperty(FIELDNAME_KEY, name);
	}

}
