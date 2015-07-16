package com.gentics.mesh.core.data;

import java.io.IOException;

import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

public interface SchemaContainer extends GenericVertex<SchemaResponse>, NamedNode {

	public Schema getSchema() throws IOException;

	public void setSchema(Schema schema);

	SchemaContainerImpl getImpl();

}
