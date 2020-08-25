package com.gentics.mesh.core.data.dao;

import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.TransformablePage;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.root.SchemaRoot;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.error.MeshSchemaException;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.madl.traversal.TraversalResult;
import com.gentics.mesh.parameter.PagingParameters;

public interface SchemaDaoWrapper extends SchemaDao, DaoWrapper<HibSchema> {

	HibSchema findByUuid(String uuid);

	HibSchema findByName(String name);

	HibSchema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

	HibSchema loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	TraversalResult<? extends HibSchema> findAll();

	TransformablePage<? extends HibSchema> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	TransformablePage<? extends HibSchema> findAll(InternalActionContext ac, HibProject project, PagingParameters pagingInfo);

	HibSchema create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Find the referenced schema container version. Throws an error, if the referenced schema container version can not be found
	 * 
	 * @param reference
	 *            reference
	 * @return Resolved container version
	 */
	HibSchemaVersion fromReference(SchemaReference reference);

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

	HibSchema findByName(HibProject project, String schemaName);

	HibSchema findByUuid(HibProject project, String schemaUuid);

	long computeCount();

	void delete(HibSchema schema, BulkActionContext bac);

	/**
	 * Returns an iterable of nodes which are referencing the schema container.
	 *
	 * @return
	 */
	TraversalResult<? extends Node> getNodes(HibSchema schema);

	/**
	 * Return a list of all schema container roots to which the schema container was added.
	 *
	 * @return
	 */
	TraversalResult<? extends SchemaRoot> getRoots(HibSchema schema);

	/**
	 * Return an iterable with all found schema versions.
	 *
	 * @return
	 */
	Iterable<? extends SchemaVersion> findAllVersions(HibSchema schema);

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

	HibSchemaVersion applyChanges(HibSchemaVersion version, InternalActionContext ac, EventQueueBatch batch);

	HibSchemaVersion findVersionByRev(HibSchema schema, String version);

	boolean isLinkedToProject(HibSchema schema, HibProject project);

	SchemaResponse transformToRestSync(HibSchema schema, InternalActionContext ac, int level);

	void removeSchema(HibSchema schema, HibProject project, EventQueueBatch batch);

	SchemaChangesListModel diff(HibSchemaVersion latestVersion, InternalActionContext ac, SchemaModel requestModel);

	HibSchemaVersion findVersionByUuid(HibSchema container, String versionUuid);

	Map<HibBranch, HibSchemaVersion> findReferencedBranches(HibSchema schema);

	Iterator<? extends NodeGraphFieldContainer> findDraftFieldContainers(HibSchemaVersion version, String branchUuid);

	TraversalResult<HibProject> findLinkedProjects(HibSchema schema);

	String getETag(HibSchema schema, InternalActionContext ac);

}
