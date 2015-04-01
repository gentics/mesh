package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Project;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ProjectService;
import com.gentics.cailun.core.rest.project.request.ProjectCreateRequest;
import com.gentics.cailun.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.cailun.core.rest.project.response.ProjectListResponse;
import com.gentics.cailun.core.rest.project.response.ProjectResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class ProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Autowired
	private ProjectService projectService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return projectVerticle;
	}

	// Create Tests

	@Test
	public void testCreateProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		roleService.addPermission(info.getRole(), data().getCaiLunRoot(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull("The project should have been created.", projectService.findByName(name));

	}

	@Test
	public void testCreateDeleteProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		roleService.addPermission(info.getRole(), data().getCaiLunRoot(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull("The project should have been created.", projectService.findByName(name));

		ProjectResponse restProject = JsonUtils.readValue(response, ProjectResponse.class);
		response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + restProject.getUUID(), 200, "OK");
		expectMessageResponse("project_deleted", response, restProject.getName());

	}

	// Read Tests

	@Test
	public void testReadProjectList() throws Exception {

		roleService.addPermission(info.getRole(), data().getProject(), PermissionType.READ);

		final int nProjects = 142;
		for (int i = 0; i < nProjects; i++) {
			Project extraProject = new Project("extra_project_" + i);
			extraProject = projectService.save(extraProject);
			roleService.addPermission(info.getRole(), extraProject, PermissionType.READ);
		}
		Project noPermProject = new Project("no_perm_project");
		noPermProject = projectService.save(noPermProject);

		// Don't grant permissions to no perm project

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/projects/", 200, "OK");
		ProjectListResponse restResponse = JsonUtils.readValue(response, ProjectListResponse.class);
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, ProjectListResponse.class);
		Assert.assertEquals(perPage, restResponse.getData().size());

		// Extra projects + aloha project
		int totalProjects = nProjects + 1;
		int totalPages = (int) Math.ceil(totalProjects / (double) perPage);
		Assert.assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		Assert.assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(totalProjects, restResponse.getMetainfo().getTotalCount());

		List<ProjectResponse> allProjects = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, ProjectListResponse.class);
			allProjects.addAll(restResponse.getData());
		}
		Assert.assertEquals("Somehow not all projects were loaded when loading all pages.", totalProjects, allProjects.size());

		// Verify that the no perm project is not part of the response
		final String noPermProjectName = noPermProject.getName();
		List<ProjectResponse> filteredProjectList = allProjects.parallelStream()
				.filter(restProject -> restProject.getName().equals(noPermProjectName)).collect(Collectors.toList());
		assertTrue("The no perm project should not be part of the list since no permissions were added.", filteredProjectList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":6,\"total_count\":143}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);

	}

	@Test
	public void testReadProjectByUUID() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		roleService.addPermission(info.getRole(), project, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadProjectByUUIDWithNoPerm() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + project.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Update Tests

	@Test
	public void testUpdateProject() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Project project = data().getProject();

		roleService.addPermission(info.getRole(), project, PermissionType.UPDATE);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setUuid(project.getUuid());
		request.setName("New Name");

		String response = request(info, HttpMethod.PUT, "/api/v1/projects/" + project.getUuid(), 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"New Name\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Project reloadedProject = projectService.findByUUID(project.getUuid());

		Assert.assertEquals("New Name", reloadedProject.getName());
	}

	@Test
	public void testUpdateProjectWithNoPerm() throws JsonProcessingException, Exception {
		Project project = data().getProject();

		roleService.addPermission(info.getRole(), project, PermissionType.READ);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		String response = request(info, HttpMethod.PUT, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden", JsonUtils.toJson(request));
		expectMessageResponse("error_missing_perm", response, project.getUuid());

		Project reloadedProject = projectService.reload(project);
		Assert.assertEquals("The name should not have been changed", project.getName(), reloadedProject.getName());
	}

	// Delete Tests

	@Test
	public void testDeleteProjectByUUID() throws Exception {
		Project project = data().getProject();
		assertNotNull(project.getUuid());

		roleService.addPermission(info.getRole(), project, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getUuid(), 200, "OK");
		expectMessageResponse("project_deleted", response, project.getName());
		assertNull("The project should have been deleted", projectService.findByUUID(project.getUuid()));

		// TODO check for removed routers?
	}

	@Test
	public void testDeleteProjectByUUIDWithNoPermission() throws Exception {
		Project project = data().getProject();

		String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, project.getUuid());
		assertNotNull("The project should not have been deleted", projectService.findByUUID(project.getUuid()));
	}

}
