package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link HibSchema}.
 */
public interface SchemaDao extends ContainerDao<SchemaResponse, SchemaVersionModel, SchemaReference, HibSchema, HibSchemaVersion, SchemaModel>, RootDao<HibProject, HibSchema> {

	/**
	 * Create the schema.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibSchema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Find the referenced schema container version. Throws an error, if the referenced schema container version can not be found
	 * 
	 * @param reference
	 *            reference
	 * @return Resolved container version
	 */
	HibSchemaVersion fromReference(SchemaReference reference);

	/**
	 * Load the schema versions via the given reference.
	 * 
	 * @param project
	 * @param reference
	 * @return
	 */
	HibSchemaVersion fromReference(HibProject project, SchemaReference reference);

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
	HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid) throws MeshSchemaException;

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
	default HibSchema create(SchemaVersionModel schema, HibUser creator) throws MeshSchemaException {
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
	 * @param validate
	 *
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	HibSchema create(SchemaVersionModel schema, HibUser creator, String uuid, boolean validate) throws MeshSchemaException;

	/**
	 * Returns an iterable of nodes which are referencing the schema container.
	 *
	 * @return
	 */
	Result<? extends HibNode> getNodes(HibSchema schema);

	/**
	 * Assign the schema to the project.
	 * 
	 * @param schemaContainer
	 * @param project
	 * @param user
	 * @param batch
	 */
	void addSchema(HibSchema schemaContainer, HibProject project, HibUser user, EventQueueBatch batch);

	/**
	 * Remove the schema from the project.
	 * 
	 * @param schema
	 * @param project
	 * @param batch
	 */
	void removeSchema(HibSchema schema, HibProject project, EventQueueBatch batch);

	/**
	 * Find all projects which reference the schema.
	 * 
	 * @param schema
	 * @return
	 */
	Result<HibProject> findLinkedProjects(HibSchema schema);

	/**
	 * Load all nodes.
	 * 
	 * @param version
	 * @param uuid
	 * @param user
	 * @param type
	 * @return
	 */
	Result<? extends HibNode> findNodes(HibSchemaVersion version, String uuid, HibUser user, ContainerType type);

	/**
	 * Add the schema to the db.
	 * 
	 * @param schema
	 */
	void addSchema(HibSchema schema);
}
