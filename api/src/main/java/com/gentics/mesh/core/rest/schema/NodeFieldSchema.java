package com.gentics.mesh.core.rest.schema;


public interface NodeFieldSchema extends MicroschemaListableFieldSchema {

	String[] getAllowedSchemas();

	void setAllowedSchemas(String[] allowedSchemas);

	String getUuid();

	void setUuid(String uuid);

}
