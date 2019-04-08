package com.gentics.mesh.core.data.root;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * A schema container root is an aggregation vertex which is used to aggregate schema container vertices.
 */
public interface SchemaContainerRoot extends RootVertex<SchemaContainer> {

	public static final String TYPE = "schemas";

	/**
	 * Create new schema container.
	 * 
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	default SchemaContainer create(SchemaModel schema, User creator) throws MeshSchemaException {
		return create(schema, creator, null);
	}

	/**
	 * Create new schema container.
	 * 
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @param uuid
	 *            Optional uuid
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	SchemaContainer create(SchemaModel schema, User creator, String uuid) throws MeshSchemaException;

	/**
	 * Create new schema container.
	 *
	 * @param schema
	 *            Schema that should be stored in the container
	 * @param creator
	 *            User that is used to set editor and creator references
	 * @param uuid
	 *            Optional uuid
	 * @param validate
	 *
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	SchemaContainer create(SchemaModel schema, User creator, String uuid, boolean validate) throws MeshSchemaException;

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
	 * Find the referenced schema container version. Throws an error, if the referenced schema container version can not be found
	 * 
	 * @param reference
	 *            reference
	 * @return Resolved container version
	 */
	SchemaContainerVersion fromReference(SchemaReference reference);

	/**
	 * Returns the project to which the schema container root belongs.
	 * 
	 * @return Project or null if this is the global root container
	 */
	Project getProject();

}
