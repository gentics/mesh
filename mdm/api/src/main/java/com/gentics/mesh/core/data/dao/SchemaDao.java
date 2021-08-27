package com.gentics.mesh.core.data.dao;

import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * DAO for {@link HibSchema}.
 */
public interface SchemaDao extends DaoGlobal<HibSchema>, DaoTransformable<HibSchema, SchemaResponse> {

	/**
	 * Load the schema by uuid.
	 * 
	 * @param project
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	HibSchema loadObjectByUuid(HibProject project, InternalActionContext ac, String uuid, InternalPermission perm);

	/**
	 * Load a page of schemas.
	 * 
	 * @param ac
	 * @param project
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibSchema> findAll(InternalActionContext ac, HibProject project, PagingParameters pagingInfo);

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
	 * Find the schema by name.
	 * 
	 * @param project
	 * @param schemaName
	 * @return
	 */
	HibSchema findByName(HibProject project, String schemaName);

	/**
	 * Find the schema by uuid.
	 * 
	 * @param project
	 * @param schemaUuid
	 * @return
	 */
	HibSchema findByUuid(HibProject project, String schemaUuid);

	/**
	 * Delete the schema.
	 * 
	 * @param schema
	 * @param bac
	 */
	void delete(HibSchema schema, BulkActionContext bac);

	/**
	 * Returns an iterable of nodes which are referencing the schema container.
	 *
	 * @return
	 */
	Result<? extends HibNode> getNodes(HibSchema schema);

	/**
	 * Return an iterable with all found schema versions.
	 *
	 * @return
	 */
	Iterable<HibSchemaVersion> findAllVersions(HibSchema schema);

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
	 * Apply the given set of changes to the schema version. This will create a new version.
	 * 
	 * @param version
	 * @param ac
	 * @param model
	 * @param batch
	 * @return
	 */
	HibSchemaVersion applyChanges(HibSchemaVersion version, InternalActionContext ac, SchemaChangesListModel model, EventQueueBatch batch);

	/**
	 * Apply changes to the schema version.
	 * 
	 * @param version
	 * @param ac
	 * @param batch
	 * @return
	 */
	HibSchemaVersion applyChanges(HibSchemaVersion version, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Load the schema version via the schema and version.
	 * 
	 * @param schema
	 * @param version
	 * @return
	 */
	HibSchemaVersion findVersionByRev(HibSchema schema, String version);

	/**
	 * Check whether the schema is linked to the project.
	 * 
	 * @param schema
	 * @param project
	 * @return
	 */
	boolean isLinkedToProject(HibSchema schema, HibProject project);

	/**
	 * Remove the schema from the project.
	 * 
	 * @param schema
	 * @param project
	 * @param batch
	 */
	void removeSchema(HibSchema schema, HibProject project, EventQueueBatch batch);

	/**
	 * Diff the schema version with the request model and return a list of changes.
	 * 
	 * @param latestVersion
	 * @param ac
	 * @param requestModel
	 * @return
	 */
	SchemaChangesListModel diff(HibSchemaVersion latestVersion, InternalActionContext ac, SchemaModel requestModel);

	/**
	 * Return the schema version.
	 * 
	 * @param container
	 * @param versionUuid
	 * @return
	 */
	HibSchemaVersion findVersionByUuid(HibSchema container, String versionUuid);

	/**
	 * Load the branch schemaversion assignments for the given schema. 
	 * @param schema
	 * @return
	 */
	Map<HibBranch, HibSchemaVersion> findReferencedBranches(HibSchema schema);

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
	 * Load all schemas for the project.
	 * 
	 * @param project
	 * @return
	 */
	Result<? extends HibSchema> findAll(HibProject project);

	/**
	 * Add the schema to the db.
	 * 
	 * @param schema
	 */
	void addSchema(HibSchema schema);

	/**
	 * Load all active schema versions for the given branch.
	 * 
	 * @param branch
	 * @return
	 */
	Result<HibSchemaVersion> findActiveSchemaVersions(HibBranch branch);

	/**
	 * Load a page of schemas.
	 * 
	 * @param project
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends HibSchema> findAll(HibProject project, InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibSchema> extraFilter);

	/**
	 * Check whether the schema is linked to the project.
	 * 
	 * @param project
	 * @param schema
	 * @return
	 */
	boolean contains(HibProject project, HibSchema schema);

	/**
	 * Load the contents that use the given schema version for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @return
	 */
	Iterator<? extends HibNodeFieldContainer> findDraftFieldContainers(HibSchemaVersion version, String branchUuid);

	/**
	 * Return a stream for {@link NodeGraphFieldContainer}'s that use this schema version and are versions for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 *            branch Uuid
	 * @return
	 */
	Stream<? extends HibNodeFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid);

	/**
	 * Return a stream for {@link NodeGraphFieldContainer}'s that use this schema version and are versions for the given branch.
	 * 
	 * @param version
	 * @param branchUuid
	 * @param bucket
	 *            Bucket to limit the selection by
	 * @return
	 */
	Stream<? extends HibNodeFieldContainer> getFieldContainers(HibSchemaVersion version, String branchUuid, Bucket bucket);

	@Override
	default String getAPIPath(HibSchema element, InternalActionContext ac) {
		return element.getAPIPath(ac);
	}
}
