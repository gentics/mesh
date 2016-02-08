package com.gentics.mesh.core.data.schema.impl;

import com.gentics.mesh.core.data.schema.FieldChange;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;

/**
 * Change entry which contains information for a field update. This includes field specific settings.
 */
public class UpdateFieldChangeImpl extends AbstractSchemaFieldChange implements FieldChange {

	public static final SchemaChangeOperation OPERATION = SchemaChangeOperation.UPDATEFIELD;

	@Override
	public Schema apply(Schema schema) {
		return schema;
	}

	@Override
	public void setFieldProperty(String key, String value) {
		setProperty(FIELD_PROPERTY_PREFIX_KEY + key, value);
	}

	@Override
	public String getFieldProperty(String key) {
		return getProperty(FIELD_PROPERTY_PREFIX_KEY + key);
	}

}
