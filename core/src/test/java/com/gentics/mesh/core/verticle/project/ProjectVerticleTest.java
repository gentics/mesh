package com.gentics.mesh.core.verticle.project;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.Project;
import com.gentics.mesh.core.data.service.ProjectService;
import com.gentics.mesh.core.rest.project.request.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.request.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.project.response.ProjectListResponse;
import com.gentics.mesh.core.rest.project.response.ProjectResponse;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.JsonUtils;

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

		roleService.addPermission(info.getRole(), data().getProject().getRootNode(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		ProjectResponse restProject = JsonUtils.readValue(response, ProjectResponse.class);

		test.assertProject(request, restProject);
		assertNotNull("The project should have been created.", projectService.findByName(name));

	}

	@Test
	public void testCreateDeleteProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		roleService.addPermission(info.getRole(), data().getProject().getRootNode(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/projects/", 200, "OK", requestJson);
		ProjectResponse restProject = JsonUtils.readValue(response, ProjectResponse.class);
		test.assertProject(request, restProject);

		assertNotNull("The project should have been created.", projectService.findByName(name));

		response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + restProject.getUuid(), 200, "OK");
		expectMessageResponse("project_deleted", response, restProject.getName());

	}

	// Read Tests

	@Test
	public void testReadProjectList() throws Exception {

		roleService.addPermission(info.getRole(), data().getProject(), PermissionType.READ);

		final int nProjects = 142;
		Project noPermProject;
		try (Transaction tx = graphDb.beginTx()) {
			for (int i = 0; i < nProjects; i++) {
				Project extraProject = projectService.create("extra_project_" + i);
				extraProject.setRootNode(data().getProject().getRootNode());
				extraProject = projectService.save(extraProject);
				roleService.addPermission(info.getRole(), extraProject, PermissionType.READ);
			}
			noPermProject = projectService.create("no_perm_project");
			noPermProject = projectService.save(noPermProject);
			tx.success();
		}

		// Don't grant permissions to no perm project

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/projects/", 200, "OK");
		ProjectListResponse restResponse = JsonUtils.readValue(response, ProjectListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, ProjectListResponse.class);
		assertEquals(perPage, restResponse.getData().size());

		// Extra projects + dummy project
		int totalProjects = nProjects + 1;
		int totalPages = (int) Math.ceil(totalProjects / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		assertEquals(totalProjects, restResponse.getMetainfo().getTotalCount());

		List<ProjectResponse> allProjects = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, ProjectListResponse.class);
			allProjects.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all projects were loaded when loading all pages.", totalProjects, allProjects.size());

		// Verify that the no perm project is not part of the response
		final String noPermProjectName = noPermProject.getName();
		List<ProjectResponse> filteredProjectList = allProjects.parallelStream()
				.filter(restProject -> restProject.getName().equals(noPermProjectName)).collect(Collectors.toList());
		assertTrue("The no perm project should not be part of the list since no permissions were added.", filteredProjectList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=0&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/projects/?per_page=-1&page=" + 1, 400, "Bad Request");
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
		System.out.println(response);
		ProjectResponse restProject = JsonUtils.readValue(response, ProjectResponse.class);
		test.assertProject(project, restProject);

		List<String> permissions = Arrays.asList(restProject.getPerms());
		assertTrue(permissions.contains("create"));
		assertTrue(permissions.contains("read"));
		assertTrue(permissions.contains("update"));
		assertTrue(permissions.contains("delete"));
	}

	@Test
	public void testReadProjectByUUIDWithNoPerm() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), project, PermissionType.READ);
			tx.success();
		}

		String response = request(info, HttpMethod.GET, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, project.getUuid());
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
		ProjectResponse restProject = JsonUtils.readValue(response, ProjectResponse.class);
		test.assertProject(request, restProject);

		Project reloadedProject = projectService.findByUUID(project.getUuid());
		assertEquals("New Name", reloadedProject.getName());
	}

	@Test
	public void testUpdateProjectWithNoPerm() throws JsonProcessingException, Exception {
		Project project = data().getProject();

		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(info.getRole(), project, PermissionType.READ);
			roleService.revokePermission(info.getRole(), project, PermissionType.UPDATE);
			tx.success();
		}

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

		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), project, PermissionType.DELETE);
			tx.success();
		}

		String response = request(info, HttpMethod.DELETE, "/api/v1/projects/" + project.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, project.getUuid());
		assertNotNull("The project should not have been deleted", projectService.findByUUID(project.getUuid()));
	}

}
