package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A schema container root is an aggregation vertex which is used to aggregate schema container vertices.
 */
public interface SchemaContainerRoot extends RootVertex<SchemaContainer> {

	public static final String TYPE = "schemas";


	/**
	 * Add the schema to the aggregation node and assign it to all branches of the project to which the root belongs.
	 * 
	 * @param user
	 *            User for the creator value of the jobs which will be created
	 * @param schemaContainer
	 * @param batch
	 */
	void addSchemaContainer(User user, SchemaContainer schemaContainer, EventQueueBatch batch);

	/**
	 * Remove the schema container from the aggregation node.
	 * 
	 * @param schemaContainer
	 * @param batch
	 */
	void removeSchemaContainer(SchemaContainer schemaContainer, EventQueueBatch batch);

	/**
	 * Check whether the given schema is assigned to this root node.
	 * 
	 * @param schema
	 * @return
	 */
	boolean contains(SchemaContainer schema);

	/**
	 * Returns the project to which the schema container root belongs.
	 * 
	 * @return Project or null if this is the global root container
	 */
	Project getProject();

	SchemaContainer create();

	SchemaContainerVersion createVersion();

}
