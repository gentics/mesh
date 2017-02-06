package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;

public class ProjectExamples extends AbstractExamples {

	public ProjectResponse getProjectResponse(String name) {
		ProjectResponse project = new ProjectResponse();
		project.setUuid(randomUUID());
		project.setName(name);
		project.setCreated(getTimestamp());
		project.setCreator(getUserReference());
		project.setEdited(getTimestamp());
		project.setEditor(getUserReference());
		project.setPermissions(READ, DELETE, CREATE);
		project.setRootNodeUuid(randomUUID());
		return project;
	}

	public ProjectResponse getProjectResponse2() {
		ProjectResponse project2 = new ProjectResponse();
		project2.setUuid(randomUUID());
		project2.setName("Dummy Project (Mobile)");
		project2.setCreated(getTimestamp());
		project2.setCreator(getUserReference());
		project2.setEdited(getTimestamp());
		project2.setEditor(getUserReference());
		project2.setPermissions(READ, DELETE, CREATE);
		project2.setRootNodeUuid(randomUUID());
		return project2;
	}

	public ProjectListResponse getProjectListResponse() {
		ProjectListResponse projectList = new ProjectListResponse();
		projectList.getData().add(getProjectResponse("Dummy project"));
		projectList.getData().add(getProjectResponse2());
		setPaging(projectList, 1, 10, 2, 20);
		return projectList;
	}

	public ProjectUpdateRequest getProjectUpdateRequest(String name) {
		ProjectUpdateRequest projectUpdate = new ProjectUpdateRequest();
		projectUpdate.setName(name);
		return projectUpdate;
	}

	public ProjectCreateRequest getProjectCreateRequest(String name) {
		ProjectCreateRequest projectCreate = new ProjectCreateRequest();
		projectCreate.setName(name);
		projectCreate.setSchema(new SchemaReference().setName("folder").setUuid(randomUUID()));
		return projectCreate;
	}

}
