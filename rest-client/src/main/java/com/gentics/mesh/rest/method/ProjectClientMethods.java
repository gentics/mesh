package com.gentics.mesh.rest.method;

import io.vertx.core.Future;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.QueryParameterProvider;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;

public interface ProjectClientMethods {

	Future<ProjectResponse> findProjectByUuid(String uuid);

	Future<ProjectListResponse> findProjects(QueryParameterProvider... parameters);

	// TODO use language tag instead?
	Future<ProjectResponse> assignLanguageToProject(String projectUuid, String languageUuid);

	Future<ProjectResponse> unassignLanguageFromProject(String projectUuid, String languageUuid);

	Future<ProjectResponse> createProject(ProjectCreateRequest projectCreateRequest);

	Future<ProjectResponse> updateProject(String uuid, ProjectUpdateRequest projectUpdateRequest);

	Future<GenericMessageResponse> deleteProject(String uuid);
}
