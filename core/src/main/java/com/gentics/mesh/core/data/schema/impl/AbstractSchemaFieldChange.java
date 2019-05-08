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

	@Override
	public String getFieldName() {
		return getRestProperty(SchemaChangeModel.FIELD_NAME_KEY);
	}

	@Override
	public void setFieldName(String name) {
		setRestProperty(SchemaChangeModel.FIELD_NAME_KEY, name);
	}

	@Override
	public void updateFromRest(SchemaChangeModel model) {
		for (Map.Entry<String, Object> entry : model.getProperties().entrySet()) {
			Object value = entry.getValue();
			String key = entry.getKey();
			setRestProperty(key, value);
		}
	}

}
