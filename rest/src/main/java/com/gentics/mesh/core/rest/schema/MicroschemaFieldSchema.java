package com.gentics.mesh.core.rest.schema;

import java.util.Map;

import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.MicroschemaField;

public interface MicroschemaFieldSchema extends MicroschemaField, FieldSchema {

	String[] getAllowedMicroSchemas();

	void setAllowedMicroSchemas(String[] allowedMicroSchemas);

	Map<String, Field> getDefaultValues();

}
