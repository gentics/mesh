package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.root.ProjectRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class ProjectVerticleTest extends AbstractRestVerticleTest {

	private ProjectRoot projectRoot;

	@Autowired
	private ProjectVerticle projectVerticle;

	@Before
	public void setup() throws Exception {
		super.setupVerticleTest();
		projectRoot = boot.projectRoot();
	}

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

		info.getRole().addPermissions(data().getProject().getRootNode(), CREATE_PERM);

		Future<ProjectResponse> future = getClient().createProject(request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();

		test.assertProject(request, restProject);
		assertNotNull("The project should have been created.", projectRoot.findByName(name));

	}

	@Test
	public void testCreateDeleteProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		info.getRole().addPermissions(data().getProject().getRootNode(), CREATE_PERM);

		// Create a new project
		Future<ProjectResponse> createFuture = getClient().createProject(request);
		latchFor(createFuture);
		assertSuccess(createFuture);
		ProjectResponse restProject = createFuture.result();
		test.assertProject(request, restProject);
		assertNotNull("The project should have been created.", projectRoot.findByName(name));

		// Now delete the project
		Future<GenericMessageResponse> deleteFuture = getClient().deleteProject(restProject.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("project_deleted", deleteFuture, restProject.getName());

	}

	// Read Tests

	@Test
	public void testReadProjectList() throws Exception {

		ProjectRoot projectRoot = data().getMeshRoot().getProjectRoot();
		info.getRole().addPermissions(data().getProject(), READ_PERM);

		final int nProjects = 142;
		Project noPermProject;
		for (int i = 0; i < nProjects; i++) {
			Project extraProject = projectRoot.create("extra_project_" + i);
			extraProject.setRootNode(data().getProject().getRootNode());
			info.getRole().addPermissions(extraProject, READ_PERM);
		}
		noPermProject = projectRoot.create("no_perm_project");

		// Don't grant permissions to no perm project

		// Test default paging parameters
		Future<ProjectListResponse> future = getClient().findProjects(new PagingInfo());
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		future = getClient().findProjects(new PagingInfo(3, perPage));
		latchFor(future);
		assertSuccess(future);
		restResponse = future.result();
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
			Future<ProjectListResponse> pageFuture = getClient().findProjects(new PagingInfo(page, perPage));
			latchFor(pageFuture);
			assertSuccess(pageFuture);
			restResponse = pageFuture.result();
			allProjects.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all projects were loaded when loading all pages.", totalProjects, allProjects.size());

		// Verify that the no perm project is not part of the response
		final String noPermProjectName = noPermProject.getName();
		List<ProjectResponse> filteredProjectList = allProjects.parallelStream()
				.filter(restProject -> restProject.getName().equals(noPermProjectName)).collect(Collectors.toList());
		assertTrue("The no perm project should not be part of the list since no permissions were added.", filteredProjectList.size() == 0);

		future = getClient().findProjects(new PagingInfo(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findProjects(new PagingInfo(1, 0));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findProjects(new PagingInfo(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findProjects(new PagingInfo(4242, 25));
		latchFor(future);
		assertSuccess(future);

		String response = JsonUtil.toJson(future.result());
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":5,\"total_count\":143}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);

	}

	@Test
	public void testReadProjectByUUID() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		info.getRole().addPermissions(project, READ_PERM);

		Future<ProjectResponse> future = getClient().findProjectByUuid(project.getUuid());
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();
		test.assertProject(project, restProject);

		List<String> permissions = Arrays.asList(restProject.getPermissions());
		assertTrue(permissions.contains("create"));
		assertTrue(permissions.contains("read"));
		assertTrue(permissions.contains("update"));
		assertTrue(permissions.contains("delete"));
	}

	@Test
	public void testReadProjectByUUIDWithNoPerm() throws Exception {
		Project project = data().getProject();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());

		info.getRole().revokePermissions(project, READ_PERM);

		Future<ProjectResponse> future = getClient().findProjectByUuid(project.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", project.getUuid());

	}

	// Update Tests

	@Test
	public void testUpdateProject() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Project project = data().getProject();

		info.getRole().addPermissions(project, UPDATE_PERM);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setUuid(project.getUuid());
		request.setName("New Name");

		Future<ProjectResponse> future = getClient().updateProject(project.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();
		test.assertProject(request, restProject);

		Project reloadedProject = projectRoot.findByUUID(project.getUuid());
		assertEquals("New Name", reloadedProject.getName());
	}

	@Test
	public void testUpdateProjectWithNoPerm() throws JsonProcessingException, Exception {
		Project project = data().getProject();

		info.getRole().addPermissions(project, READ_PERM);
		info.getRole().revokePermissions(project, UPDATE_PERM);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		Future<ProjectResponse> future = getClient().updateProject(project.getUuid(), request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm");

		Project reloadedProject = projectRoot.findByUUID(project.getUuid());
		assertEquals("The name should not have been changed", project.getName(), reloadedProject.getName());
	}

	// Delete Tests

	@Test
	public void testDeleteProjectByUUID() throws Exception {
		Project project = data().getProject();
		String uuid = project.getUuid();
		assertNotNull(uuid);
		info.getRole().addPermissions(project, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("project_deleted", future, project.getName());
		assertNull("The project should have been deleted", projectRoot.findByUUID(uuid));

		// TODO check for removed routers?
	}

	@Test
	public void testDeleteProjectByUUIDWithNoPermission() throws Exception {
		Project project = data().getProject();
		String uuid = project.getUuid();
		info.getRole().revokePermissions(project, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm");
		assertNotNull("The project should not have been deleted", projectRoot.findByUUID(uuid));
	}

}
