package com.gentics.mesh.core.data.schema.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.gentics.mesh.core.data.schema.SchemaFieldChange;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.util.Tuple;

/**
 * @see SchemaFieldChange
 */
public abstract class AbstractSchemaFieldChange extends AbstractSchemaChange<FieldSchemaContainer> implements SchemaFieldChange {

	private static final String FIELDNAME_KEY = "fieldName";

	public static final String FIELD_PROPERTY_PREFIX_KEY = "fieldProperty_";

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

	@Override
	public void setFieldProperty(String key, Object value) {
		setProperty(FIELD_PROPERTY_PREFIX_KEY + key, value);
	}

	@Override
	public String getFieldProperty(String key) {
		return getProperty(FIELD_PROPERTY_PREFIX_KEY + key);
	}

	@Override
	public <T> Map<String, T> getFieldProperties() {
		return getProperties(FIELD_PROPERTY_PREFIX_KEY);
	}

	@Override
	public void fill(SchemaChangeModel restChange) {
		setFieldName(restChange.getFieldName());
		setCustomMigrationScript(restChange.getMigrationScript());
		for (Map.Entry<String, Object> entry : restChange.getProperties().entrySet()) {
			//TODO handle arrays
			setFieldProperty(entry.getKey(), String.valueOf(entry.getValue()));
		}
	}

}
