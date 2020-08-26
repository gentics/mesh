package com.gentics.mesh.core.data.schema;

import com.gentics.mesh.core.data.HibElement;
import com.gentics.mesh.core.rest.schema.FieldSchemaContainer;

public interface HibFieldSchemaVersionElement<RM extends FieldSchemaContainer> extends HibElement {

	String getName();

	String getJson();

	void setJson(String json);

	String getVersion();

	RM getSchema();

	void setSchema(RM schema);

	SchemaChange<?> getNextChange();

	void setNextChange(SchemaChange<?> change);

	void deleteElement();

}
