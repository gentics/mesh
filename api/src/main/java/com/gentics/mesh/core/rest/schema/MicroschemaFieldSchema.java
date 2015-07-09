package com.gentics.mesh.core.rest.schema;

import java.util.List;

import com.gentics.mesh.core.rest.node.field.MicroschemaListableField;

public interface MicroschemaFieldSchema extends FieldSchema {

	String[] getAllowedMicroSchemas();

	void setAllowedMicroSchemas(String[] allowedMicroSchemas);

	List<? extends MicroschemaListableField> getFields();

}
