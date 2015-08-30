package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

public interface SchemaContainer extends GenericVertex<SchemaResponse>, NamedVertex {

	public static final String TYPE = "schemaContainer";

	Schema getSchema();

	void setSchema(Schema schema);

	SchemaContainerImpl getImpl();

	List<? extends Node> getNodes();

}
