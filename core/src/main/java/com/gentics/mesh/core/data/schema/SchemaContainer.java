package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends GraphFieldSchemaContainer<Schema, SchemaContainer, SchemaReference> {

	public static final String TYPE = "schemaContainer";

	/**
	 * Return a list of nodes to which the schema has been assigned.
	 * 
	 * @return
	 */
	List<? extends Node> getNodes();

}
