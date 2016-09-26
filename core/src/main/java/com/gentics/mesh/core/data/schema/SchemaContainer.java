package com.gentics.mesh.core.data.schema;

import java.util.List;

import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;

/**
 * A schema container is a graph element which stores the JSON schema data.
 */
public interface SchemaContainer extends GraphFieldSchemaContainer<Schema, SchemaReference, SchemaContainer, SchemaContainerVersion> {

	/**
	 * Type Value: {@value #TYPE}
	 */
	public static final String TYPE = "schemaContainer";

	/**
	 * Return the list of nodes which are referencing the schema container.
	 * 
	 * @return
	 */
	List<? extends NodeImpl> getNodes();

}
