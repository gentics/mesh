package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public interface HibFieldSchemaVersionElement extends HibElement {

	String getName();

	String getJson();

	void setJson(String json);

	String getVersion();

	FieldSchemaContainer getSchema();

}
