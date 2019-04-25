package com.gentics.mesh.rest.client.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.impl.EmptyResponse;

public interface ProjectClientMethods {

	/**
	 * Load the given project.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Find the project using the specified name.
	 * 
	 * @param name
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters);

	/**
	 * Load multiple projects.
	 * 
	 * @param parameters
	 * @return
	 */
	MeshRequest<ProjectListResponse> findProjects(ParameterProvider... parameters);

	// TODO use language tag instead?
	/**
	 * Assign language to the project.
	 * 
	 * @param projectUuid
	 * @param languageUuid
	 * @return
	 */
	MeshRequest<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid);

	/**
	 * Unassign the given language from the project.
	 * 
	 * @param projectUuid
	 * @param languageUuid
	 * @return
	 */
	MeshRequest<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid);

	/**
	 * Create a new project.
	 * 
	 * @param request
	 * @return
	 */

	MeshRequest<ProjectResponse> createProject(ProjectCreateRequest request);

	/**
	 * Create a new project using the provided uuid.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	MeshRequest<ProjectResponse> createProject(String uuid, ProjectCreateRequest request);

	/**
	 * Update the project.
	 * 
	 * @param uuid
	 *            Uuid of the project
	 * @param request
	 * @return
	 */
	MeshRequest<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request);

	/**
	 * Delete the project.
	 *
	 * @param uuid Uuid of the project
	 * @return
	 */
	MeshRequest<EmptyResponse> deleteProject(String uuid);

	/**
	 * Invoke a version purge on the project to get rid of all versions across all nodes and branches of this project.
	 * @param uuid
	 * @return
	 */
	MeshRequest<GenericMessageResponse> purgeProject(String uuid);
}
