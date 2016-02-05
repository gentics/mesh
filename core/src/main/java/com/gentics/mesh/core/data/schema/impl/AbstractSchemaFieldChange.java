package com.gentics.mesh.core.data.schema.impl;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.core.data.schema.SchemaFieldChange;
import com.gentics.mesh.util.Tuple;

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

	@Override
	public List<Tuple<String, Object>> getMigrationScriptContext() {
		return Arrays.asList(Tuple.tuple("fieldname", getFieldName()));
	}
}
