package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface SchemaDaoWrapper extends SchemaDao {

	SchemaContainer findByUuid(String uuid);

	SchemaContainer findByName(String name);

	SchemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm);

	SchemaContainer loadObjectByUuid(InternalActionContext ac, String uuid, GraphPermission perm, boolean errorIfNotFound);

	TraversalResult<? extends SchemaContainer> findAll();

	TransformablePage<? extends SchemaContainer> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	boolean update(SchemaContainer schema, InternalActionContext ac, EventQueueBatch batch);

	SchemaContainer create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Find the referenced schema container version. Throws an error, if the referenced schema container version can not be found
	 * 
	 * @param reference
	 *            reference
	 * @return Resolved container version
	 */
	SchemaContainerVersion fromReference(SchemaReference reference);

	SchemaContainerVersion fromReference(Project project, SchemaReference reference);

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
	 * @param validate
	 *
	 * @return Created schema container
	 * @throws MeshSchemaException
	 */
	SchemaContainer create(SchemaModel schema, User creator, String uuid, boolean validate) throws MeshSchemaException;

	SchemaContainer findByName(Project project, String schemaName);

	SchemaContainer findByUuid(Project project, String schemaUuid);

	long computeCount();

}
