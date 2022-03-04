package com.gentics.mesh.core.data.schema;

import java.util.Map;

import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;

/**
 * Common field change class which may be used for changes that target a specific field.
 */
public interface HibSchemaFieldChange extends HibSchemaChange<FieldSchemaContainer> {

	/**
	 * Return the field name which should be handled.
	 * 
	 * @return field name
	 */
	default String getFieldName() {
		return getRestProperty(SchemaChangeModel.FIELD_NAME_KEY);
	}

	/**
	 * Set the name of the field which should be handled.
	 * 
	 * @param name
	 *            field name
	 */
	default void setFieldName(String name) {
		setRestProperty(SchemaChangeModel.FIELD_NAME_KEY, name);
	}

	@Override
	default void updateFromRest(SchemaChangeModel model) {
		for (Map.Entry<String, Object> entry : model.getProperties().entrySet()) {
			Object value = entry.getValue();
			String key = entry.getKey();
			setRestProperty(key, value);
		}
	}
}