package com.gentics.mesh.core.data.dao;

import java.util.function.Predicate;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.PagingParameters;

/**
 * Temporary in-between interface that helps test removal of project root deps.
 */
// TODO move the contents of this to ProjectDao once migration is done
public interface ProjectDaoWrapper extends ProjectDao, DaoWrapper<HibProject>, DaoTransformable<HibProject, ProjectResponse> {

	/**
	 * Find all projects.
	 * 
	 * @return
	 */
	Result<? extends HibProject> findAll();

	/**
	 * Load a page of projects.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @return
	 */
	Page<? extends HibProject> findAll(InternalActionContext ac, PagingParameters pagingInfo);

	/**
	 * Load a page of projects.
	 * 
	 * @param ac
	 * @param pagingInfo
	 * @param extraFilter
	 * @return
	 */
	Page<? extends HibProject> findAll(InternalActionContext ac, PagingParameters pagingInfo, Predicate<HibProject> extraFilter);

	/**
	 * Find the project by name.
	 * 
	 * @param name
	 * @return
	 */
	HibProject findByName(String name);

	/**
	 * Find the project by name.
	 * 
	 * @param ac
	 * @param projectName
	 * @param perm
	 * @return
	 */
	HibProject findByName(InternalActionContext ac, String projectName, InternalPermission perm);

	/**
	 * Find the project by uuid.
	 * 
	 * @param uuid
	 * @return
	 */
	HibProject findByUuid(String uuid);

	/**
	 * Delete the project.
	 * 
	 * @param project
	 * @param bc
	 */
	void delete(HibProject project, BulkActionContext bc);

	/**
	 * Update the project.
	 * 
	 * @param element
	 * @param ac
	 * @param batch
	 * @return
	 */
	boolean update(HibProject element, InternalActionContext ac, EventQueueBatch batch);

	/**
	 * Load the project by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @return
	 */
	HibProject loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm);

	/**
	 * Load the project by uuid.
	 * 
	 * @param ac
	 * @param uuid
	 * @param perm
	 * @param errorIfNotFound
	 * @return
	 */
	HibProject loadObjectByUuid(InternalActionContext ac, String uuid, InternalPermission perm, boolean errorIfNotFound);

	/**
	 * Create the project.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	HibProject create(InternalActionContext ac, EventQueueBatch batch, String uuid);

	/**
	 * Create a project.
	 * 
	 * @param projectName
	 * @param hostname
	 * @param ssl
	 * @param pathPrefix
	 * @param creator
	 * @param schemaVersion
	 *            Schema version for the parent node
	 * @param batch
	 * @return
	 */
	default HibProject create(String projectName, String hostname, Boolean ssl, String pathPrefix, HibUser creator,
		HibSchemaVersion schemaVersion, EventQueueBatch batch) {
		return create(projectName, hostname, ssl, pathPrefix, creator, schemaVersion, null, batch);
	}

	/**
	 * Create a project.
	 * 
	 * @param name
	 * @param hostname
	 * @param ssl
	 * @param pathPrefix
	 * @param creator
	 * @param schemaVersion
	 * @param uuid
	 * @param batch
	 * @return
	 */
	HibProject create(String name, String hostname, Boolean ssl, String pathPrefix, HibUser creator, HibSchemaVersion schemaVersion,
		String uuid, EventQueueBatch batch);

	/**
	 * Return the etag for the project.
	 * 
	 * @param project
	 * @param ac
	 * @return
	 */
	String getETag(HibProject project, InternalActionContext ac);

	/**
	 * Return the API path for the given project.
	 * 
	 * @param project
	 * @param ac
	 * @return
	 */
	String getAPIPath(HibProject project, InternalActionContext ac);

	/**
	 * Create a schema assign event for the given input values.
	 * 
	 * @param project
	 *            Project
	 * @param schema
	 *            Schema
	 * @param assignment
	 *            Assignment or unassignment
	 * @return
	 */
	MeshEventModel onSchemaAssignEvent(HibProject project, HibSchema schema, Assignment assignment);

}
