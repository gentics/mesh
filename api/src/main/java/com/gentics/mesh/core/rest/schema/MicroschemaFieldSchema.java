package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.node.field.MicroschemaField;

public interface MicroschemaFieldSchema extends MicroschemaField, FieldSchema {

	String[] getAllowedMicroSchemas();

	void setAllowedMicroSchemas(String[] allowedMicroSchemas);


}
