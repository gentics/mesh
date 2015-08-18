package com.gentics.mesh.core.verticle.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.verticle.ProjectVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class ProjectVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectVerticle projectVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return projectVerticle;
	}

	// Create Tests

	@Test
	public void testCreateProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		Future<ProjectResponse> future = getClient().createProject(request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();

		test.assertProject(request, restProject);
		try (Trx tx = new Trx(db)) {
			assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));
		}

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			AtomicReference<Project> reference = new AtomicReference<>();
			meshRoot().getProjectRoot().findByUuid(restProject.getUuid(), rh -> {
				reference.set(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
			Project project = reference.get();
			assertNotNull(project);
			assertTrue(user().hasPermission(project, CREATE_PERM));
			assertTrue(user().hasPermission(project.getBaseNode(), CREATE_PERM));
			assertTrue(user().hasPermission(project.getTagFamilyRoot(), CREATE_PERM));
			assertTrue(user().hasPermission(project.getTagRoot(), CREATE_PERM));
			assertTrue(user().hasPermission(project.getNodeRoot(), CREATE_PERM));
		}
	}

	@Test
	public void testCreateDeleteProject() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		try (Trx tx = new Trx(db)) {
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			tx.success();
		}

		// Create a new project
		Future<ProjectResponse> createFuture = getClient().createProject(request);
		latchFor(createFuture);
		assertSuccess(createFuture);
		ProjectResponse restProject = createFuture.result();
		test.assertProject(request, restProject);
		try (Trx tx = new Trx(db)) {
			assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));
		}

		// Now delete the project
		Future<GenericMessageResponse> deleteFuture = getClient().deleteProject(restProject.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("project_deleted", deleteFuture, restProject.getUuid() + "/" + restProject.getName());

	}

	// Read Tests

	@Test
	public void testReadProjectList() throws Exception {

		try (Trx tx = new Trx(db)) {
			role().grantPermissions(project(), READ_PERM);
			tx.success();
		}

		final int nProjects = 142;
		String noPermProjectName;
		try (Trx tx = new Trx(db)) {
			for (int i = 0; i < nProjects; i++) {
				Project extraProject = meshRoot().getProjectRoot().create("extra_project_" + i, user());
				extraProject.setBaseNode(project().getBaseNode());
				role().grantPermissions(extraProject, READ_PERM);
			}
			Project noPermProject = meshRoot().getProjectRoot().create("no_perm_project", user());
			noPermProjectName = noPermProject.getName();
			tx.success();
		}

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
		assertNotNull(response);

		ProjectListResponse listResponse = future.result();
		assertEquals(4242, listResponse.getMetainfo().getCurrentPage());
		assertEquals(25, listResponse.getMetainfo().getPerPage());
		assertEquals(143, listResponse.getMetainfo().getTotalCount());
		assertEquals(6, listResponse.getMetainfo().getPageCount());
		assertEquals(0, listResponse.getData().size());
	}

	@Test
	public void testReadProjectByUUID() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Project project = project();
			uuid = project.getUuid();
			assertNotNull("The UUID of the project must not be null.", project.getUuid());
			role().grantPermissions(project, READ_PERM);
			tx.success();
		}

		Future<ProjectResponse> future = getClient().findProjectByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();
		try (Trx tx = new Trx(db)) {
			test.assertProject(project(), restProject);
		}

		List<String> permissions = Arrays.asList(restProject.getPermissions());
		assertTrue(permissions.contains("create"));
		assertTrue(permissions.contains("read"));
		assertTrue(permissions.contains("update"));
		assertTrue(permissions.contains("delete"));
	}

	@Test
	public void testReadProjectByUUIDWithNoPerm() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Project project = project();
			uuid = project.getUuid();
			assertNotNull("The UUID of the project must not be null.", project.getUuid());
			role().revokePermissions(project, READ_PERM);
			tx.success();
		}

		Future<ProjectResponse> future = getClient().findProjectByUuid(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	// Update Tests

	@Test
	public void testUpdateProject() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Project project = project();
			uuid = project.getUuid();
			role().grantPermissions(project, UPDATE_PERM);
			tx.success();
		}

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		Future<ProjectResponse> future = getClient().updateProject(uuid, request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();
		test.assertProject(request, restProject);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
				Project reloadedProject = rh.result();
				assertEquals("New Name", reloadedProject.getName());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	public void testUpdateProjectWithNoPerm() throws JsonProcessingException, Exception {
		String uuid;
		String name;
		try (Trx tx = new Trx(db)) {
			Project project = project();
			uuid = project.getUuid();
			name = project.getName();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);
			tx.success();
		}

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		Future<ProjectResponse> future = getClient().updateProject(uuid, request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
				Project reloadedProject = rh.result();
				assertEquals("The name should not have been changed", name, reloadedProject.getName());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	// Delete Tests

	@Test
	public void testDeleteProjectByUUID() throws Exception {
		String uuid;
		String name;
		try (Trx tx = new Trx(db)) {
			Project project = project();
			uuid = project.getUuid();
			name = project.getName();
			assertNotNull(uuid);
			assertNotNull(name);
			role().grantPermissions(project, DELETE_PERM);
			tx.success();
		}

		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("project_deleted", future, uuid + "/" + name);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
				assertNull("The project should have been deleted", rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}

		// TODO check for removed routers?
	}

	@Test
	public void testDeleteProjectByUUIDWithNoPermission() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Project project = project();
			uuid = project.getUuid();
			role().revokePermissions(project, DELETE_PERM);
			tx.success();
		}

		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
				assertNotNull("The project should not have been deleted", rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

}
