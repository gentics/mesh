package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link HibProject}.
 */
public interface ProjectDao extends DaoGlobal<HibProject>, DaoTransformable<HibProject, ProjectResponse> {

	/**
	 * Create the base node of the project using the user as a reference for the editor and creator fields.
	 * 
	 * @param creator
	 *            Creator of the base node
	 * @param schemaVersion
	 *            Schema version used for the basenode creation
	 * 
	 * @return Created base node
	 */
	HibNode createBaseNode(HibProject project, HibUser creator, HibSchemaVersion schemaVersion);

	/**
	 * Return the tagFamily permission root for the project. This method will create a root when no one could be found.
	 *
	 * @return
	 */
	HibBaseElement getTagFamilyPermissionRoot(HibProject project);

	/**
	 * Return the branch permission root of the project. Internally this method will create the root when it has not yet been created.
	 *
	 * @return Branch root element
	 */
	HibBaseElement getBranchPermissionRoot(HibProject project);

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
	ProjectSchemaEventModel onSchemaAssignEvent(HibProject project, HibSchema schema, Assignment assignment);

	/**
	 * Create a microschema assign event for the given input values.
	 * 
	 * @param project
	 *            Project
	 * @param microschema
	 *            Microschema
	 * @param assignment
	 *            Assignment or unassignment
	 * @return
	 */
	ProjectMicroschemaEventModel onMicroschemaAssignEvent(HibProject project, HibMicroschema microschema, Assignment assignment);

	/**
	 * Return the sub etag for the project.
	 * 
	 * @param project
	 * @param ac
	 * @return
	 */
	String getSubETag(HibProject project, InternalActionContext ac);

	/**
	 * Find all the nodes belonging to the project.
	 * 
	 * @return
	 */
	Result<? extends HibNode> findNodes(HibProject project);
}
