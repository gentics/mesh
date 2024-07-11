package com.gentics.mesh.core.data.dao;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.BaseElement;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.project.Project;
import com.gentics.mesh.core.data.schema.Microschema;
import com.gentics.mesh.core.data.schema.Schema;
import com.gentics.mesh.core.data.schema.SchemaVersion;
import com.gentics.mesh.core.data.user.User;
import com.gentics.mesh.core.rest.event.project.ProjectMicroschemaEventModel;
import com.gentics.mesh.core.rest.event.project.ProjectSchemaEventModel;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.result.Result;
import com.gentics.mesh.event.Assignment;
import com.gentics.mesh.event.EventQueueBatch;

/**
 * DAO for {@link Project}.
 */
public interface ProjectDao extends DaoGlobal<Project>, DaoTransformable<Project, ProjectResponse> {

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
	Node createBaseNode(Project project, User creator, SchemaVersion schemaVersion);

	/**
	 * Return the tagFamily permission root for the project. This method will create a root when no one could be found.
	 *
	 * @return
	 */
	BaseElement getTagFamilyPermissionRoot(Project project);

	/**
	 * Return the branch permission root of the project. Internally this method will create the root when it has not yet been created.
	 *
	 * @return Branch root element
	 */
	BaseElement getBranchPermissionRoot(Project project);

	/**
	 * Find the project by name.
	 * 
	 * @param ac
	 * @param projectName
	 * @param perm
	 * @return
	 */
	Project findByName(InternalActionContext ac, String projectName, InternalPermission perm);

	/**
	 * Create the project.
	 * 
	 * @param ac
	 * @param batch
	 * @param uuid
	 * @return
	 */
	Project create(InternalActionContext ac, EventQueueBatch batch, String uuid);

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
	default Project create(String projectName, String hostname, Boolean ssl, String pathPrefix, User creator,
		SchemaVersion schemaVersion, EventQueueBatch batch) {
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
	Project create(String name, String hostname, Boolean ssl, String pathPrefix, User creator, SchemaVersion schemaVersion,
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
	ProjectSchemaEventModel onSchemaAssignEvent(Project project, Schema schema, Assignment assignment);

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
	ProjectMicroschemaEventModel onMicroschemaAssignEvent(Project project, Microschema microschema, Assignment assignment);

	/**
	 * Return the sub etag for the project.
	 * 
	 * @param project
	 * @param ac
	 * @return
	 */
	String getSubETag(Project project, InternalActionContext ac);

	/**
	 * Find all the nodes belonging to the project.
	 * 
	 * @return
	 */
	Result<? extends Node> findNodes(Project project);

	/**
	 * Find all the languages assigned to the project.
	 * 
	 * @return
	 */
	Result<? extends Language> findLanguages(Project project);
}
