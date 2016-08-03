package com.gentics.mesh.rest.method;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.parameter.ParameterProvider;

import io.vertx.core.Future;

public interface ProjectClientMethods {

	/**
	 * Load the given project.
	 * 
	 * @param uuid
	 * @param parameters
	 * @return
	 */
	Future<ProjectResponse> findProjectByUuid(String uuid, ParameterProvider... parameters);

	/**
	 * Find the project using the specified name.
	 * 
	 * @param name
	 * @param parameters
	 * @return
	 */
	Future<ProjectResponse> findProjectByName(String name, ParameterProvider... parameters);

	/**
	 * Load multiple projects.
	 * 
	 * @param parameters
	 * @return
	 */
	Future<ProjectListResponse> findProjects(ParameterProvider... parameters);

	// TODO use language tag instead?
	/**
	 * Assign language to the project.
	 * 
	 * @param projectUuid
	 * @param languageUuid
	 * @return
	 */
	Future<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid);

	/**
	 * Unassign the given language from the project.
	 * 
	 * @param projectUuid
	 * @param languageUuid
	 * @return
	 */
	Future<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid);

	/**
	 * Create a new project.
	 * 
	 * @param request
	 * @return
	 */
	Future<ProjectResponse> createProject(ProjectCreateRequest request);

	/**
	 * Update the project.
	 * 
	 * @param uuid
	 * @param request
	 * @return
	 */
	Future<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest request);

	/**
	 * Delete the project.
	 * 
	 * @param uuid
	 * @return
	 */
	Future<GenericMessageResponse> deleteProject(String uuid);
}
