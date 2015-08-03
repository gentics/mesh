package com.gentics.mesh.core.data;

import java.io.IOException;

import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

public interface SchemaContainer extends GenericVertex<SchemaResponse>, NamedNode {

	public static final String TYPE = "schemaContainer";

	Schema getSchema() throws IOException;

	void setSchema(Schema schema);

	SchemaContainerImpl getImpl();

}
