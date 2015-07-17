package com.gentics.mesh.core.rest.schema;

import java.util.List;

public interface MicroschemaFieldSchema extends FieldSchema {

	String[] getAllowedMicroSchemas();

	void setAllowedMicroSchemas(String[] allowedMicroSchemas);

	List<MicroschemaListableFieldSchema> getFields();

}
