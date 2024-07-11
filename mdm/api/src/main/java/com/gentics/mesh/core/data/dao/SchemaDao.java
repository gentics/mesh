package com.gentics.mesh.core.data.dao;

import java.util.stream.Stream;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link Schema}.
 */
public interface SchemaDao extends ContainerDao<SchemaResponse, SchemaVersionModel, SchemaReference, Schema, SchemaVersion, SchemaModel>, RootDao<Project, Schema> {

	/**
	 * Create the schema.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	Schema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Find the referenced schema container version. Throws an error, if the referenced schema container version can not be found
	 * 
	 * @param reference
	 *            reference
	 * @return Resolved container version
	 */
	SchemaVersion fromReference(SchemaReference reference);

	/**
	 * Load the schema versions via the given reference.
	 * 
	 * @param project
	 * @param reference
	 * @return
	 */
	SchemaVersion fromReference(Project project, SchemaReference reference);

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
	Schema create(SchemaVersionModel schema, User creator, String uuid) throws MeshSchemaException;

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
	default Schema create(SchemaVersionModel schema, User creator) throws MeshSchemaException {
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
	Schema create(SchemaVersionModel schema, User creator, String uuid, boolean validate) throws MeshSchemaException;

	/**
	 * Returns an iterable of nodes which are referencing the schema container.
	 *
	 * @return
	 */
	Result<? extends Node> getNodes(Schema schema);

	/**
	 * Find all projects which reference the schema.
	 * 
	 * @param schema
	 * @return
	 */
	Result<Project> findLinkedProjects(Schema schema);

	/**
	 * Load all nodes, accessible in the given branch with Read Published permission.
	 * 
	 * @param version schema version
	 * @param branchUuid branch uuid
	 * @param user user
	 * @param type content type
	 * @return
	 */
	Result<? extends Node> findNodes(SchemaVersion version, String branchUuid, User user, ContainerType type);

	/**
	 * Return a stream for {@link NodeFieldContainer}'s that use this schema version and are versions for the given branch.
	 * 
	 * @param versiSchemaDAOActions schemaActions();

	on
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Stream<? extends NodeFieldContainer> getFieldContainers(SchemaVersion version, String branchUuid);

	/**
	 * Return a stream for {@link NodeFieldContainer}'s that use this schema version and are versions for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @param bucket
	 *            Bucket to limit the selection by
	 * @return
	 */
	Stream<? extends NodeFieldContainer> getFieldContainers(SchemaVersion version, String branchUuid, Bucket bucket);

	/**
	 * Load the limited portion of contents that use the given schema version for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @param limit limits the fetched entry number. if less than 1, limits are disabled
	 * @return
	 */
	Result<? extends NodeFieldContainer> findDraftFieldContainers(SchemaVersion version, String branchUuid, long limit);

	/**
	 * Load the contents that use the given schema version for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	default Result<? extends NodeFieldContainer> findDraftFieldContainers(SchemaVersion version, String branchUuid) {
		return findDraftFieldContainers(version, branchUuid, -1);
	}
}
