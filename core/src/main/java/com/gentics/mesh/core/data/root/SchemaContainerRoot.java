package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.error.MeshSchemaException;

public interface SchemaContainerRoot extends RootVertex<SchemaContainer> {

	public static final String TYPE = "schemas";

	/**
	 * Create new schema container.
	 * 
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @return
	 * @throws MeshSchemaException 
	 */
	SchemaContainer create(Schema schema, User creator) throws MeshSchemaException;

	/**
	 * Add the schema to the aggregation node.
	 * 
	 * @param schemaContainer
	 */
	void addSchemaContainer(SchemaContainer schemaContainer);

	/**
	 * Remove the schema container from the aggregation node.
	 * 
	 * @param schemaContainer
	 */
	void removeSchemaContainer(SchemaContainer schemaContainer);

	boolean contains(SchemaContainer schema);

}
