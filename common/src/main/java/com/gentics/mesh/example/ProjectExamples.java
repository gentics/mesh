package com.gentics.mesh.example;

import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO2_UUID;
import static com.gentics.mesh.example.ExampleUuids.PROJECT_DEMO_UUID;
import static com.gentics.mesh.example.ExampleUuids.SCHEMA_FOLDER_UUID;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class ProjectExamples extends AbstractExamples {

	public ProjectResponse getProjectResponse(String name) {
		ProjectResponse project = new ProjectResponse();
		project.setUuid(PROJECT_DEMO2_UUID);
		project.setName(name);
		project.setCreated(createOldTimestamp());
		project.setCreator(createUserReference());
		project.setEdited(createNewTimestamp());
		project.setEditor(createUserReference());
		project.setPermissions(READ, DELETE, CREATE);
		project.setRootNode(createNodeReference());
		return project;
	}

	public ProjectResponse getProjectResponse2() {
		ProjectResponse project2 = new ProjectResponse();
		project2.setUuid(PROJECT_DEMO_UUID);
		project2.setName("Dummy Project (Mobile)");
		project2.setCreated(createOldTimestamp());
		project2.setCreator(createUserReference());
		project2.setEdited(createNewTimestamp());
		project2.setEditor(createUserReference());
		project2.setPermissions(READ, DELETE, CREATE);
		project2.setRootNode(createNodeReference());
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
		projectCreate.setHostname("getmesh.io");
		projectCreate.setSsl(true);
		projectCreate.setSchema(new SchemaReferenceImpl().setName("folder").setUuid(SCHEMA_FOLDER_UUID));
		return projectCreate;
	}

}
