package com.gentics.mesh.core.data;

import java.util.List;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaResponse;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends MeshCoreVertex<SchemaResponse, SchemaContainer>, ReferenceableElement<SchemaReference> {

	public static final String TYPE = "schemaContainer";

	/**
	 * Return the schema that is stored within the container
	 * 
	 * @return
	 */
	Schema getSchema();

	/**
	 * Set the schema for the container
	 * 
	 * @param schema
	 */
	void setSchema(Schema schema);

	/**
	 * Return a list of nodes to which the schema has been assigned.
	 * 
	 * @return
	 */
	List<? extends Node> getNodes();

}
