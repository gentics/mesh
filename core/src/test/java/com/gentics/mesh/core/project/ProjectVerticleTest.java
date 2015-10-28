package com.gentics.mesh.core.project;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.verticle.project.ProjectVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.query.impl.RolePermissionParameter;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

public class ProjectVerticleTest extends AbstractBasicCrudVerticleTest {

	@Autowired
	private ProjectVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	// Create Tests

	@Test
	@Override
	public void testCreate() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);

		Future<ProjectResponse> future = getClient().createProject(request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();

		test.assertProject(request, restProject);
		assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));

		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<Project> reference = new AtomicReference<>();
		meshRoot().getProjectRoot().findByUuid(restProject.getUuid(), rh -> {
			reference.set(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
		Project project = reference.get();
		assertNotNull(project);
		assertTrue(user().hasPermission(ac, project, CREATE_PERM));
		assertTrue(user().hasPermission(ac, project.getBaseNode(), CREATE_PERM));
		assertTrue(user().hasPermission(ac, project.getTagFamilyRoot(), CREATE_PERM));
		assertTrue(user().hasPermission(ac, project.getTagRoot(), CREATE_PERM));
		assertTrue(user().hasPermission(ac, project.getNodeRoot(), CREATE_PERM));
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		role().grantPermissions(project().getBaseNode(), CREATE_PERM);
		role().grantPermissions(project().getBaseNode(), CREATE_PERM);
		role().grantPermissions(project().getBaseNode(), CREATE_PERM);

		// Create a new project
		Future<ProjectResponse> createFuture = getClient().createProject(request);
		latchFor(createFuture);
		assertSuccess(createFuture);
		ProjectResponse restProject = createFuture.result();
		test.assertProject(request, restProject);
		meshRoot().getProjectRoot().reload();
		assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));

		// Read the project
		Future<ProjectResponse> readFuture = getClient().findProjectByUuid(restProject.getUuid());
		latchFor(readFuture);
		assertSuccess(readFuture);

		// Now delete the project
		Future<GenericMessageResponse> deleteFuture = getClient().deleteProject(restProject.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("project_deleted", deleteFuture, restProject.getUuid() + "/" + restProject.getName());

	}

	// Read Tests

	@Test
	@Override
	public void testReadMultiple() throws Exception {

		role().grantPermissions(project(), READ_PERM);

		final int nProjects = 142;
		String noPermProjectName;
		for (int i = 0; i < nProjects; i++) {
			Project extraProject = meshRoot().getProjectRoot().create("extra_project_" + i, user());
			extraProject.setBaseNode(project().getBaseNode());
			role().grantPermissions(extraProject, READ_PERM);
		}
		Project noPermProject = meshRoot().getProjectRoot().create("no_perm_project", user());
		noPermProjectName = noPermProject.getName();

		// Don't grant permissions to no perm project

		// Test default paging parameters
		Future<ProjectListResponse> future = getClient().findProjects(new PagingParameter());
		latchFor(future);
		assertSuccess(future);
		ProjectListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		future = getClient().findProjects(new PagingParameter(3, perPage));
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
			Future<ProjectListResponse> pageFuture = getClient().findProjects(new PagingParameter(page, perPage));
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

		future = getClient().findProjects(new PagingParameter(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findProjects(new PagingParameter(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findProjects(new PagingParameter(4242, 25));
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
	public void testReadProjectCountInfoOnly() {
		Future<ProjectListResponse> future = getClient().findProjects(new PagingParameter(1, 0));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		Project project = project();
		String uuid = project.getUuid();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());
		role().grantPermissions(project, READ_PERM, UPDATE_PERM);

		Future<ProjectResponse> future = getClient().findProjectByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();
		test.assertProject(project(), restProject);

		List<String> permissions = Arrays.asList(restProject.getPermissions());
		assertTrue(permissions.contains("create"));
		assertTrue(permissions.contains("read"));
		assertTrue(permissions.contains("update"));
		assertTrue(permissions.contains("delete"));
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		Project project = project();
		String uuid = project.getUuid();

		Future<ProjectResponse> future = getClient().findProjectByUuid(uuid, new RolePermissionParameter().setRoleUuid(role().getUuid()));
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result().getRolePerms());
		assertEquals(4, future.result().getRolePerms().length);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		Project project = project();
		String uuid = project.getUuid();
		assertNotNull("The UUID of the project must not be null.", project.getUuid());
		role().revokePermissions(project, READ_PERM);

		Future<ProjectResponse> future = getClient().findProjectByUuid(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	// Update Tests

	@Test
	@Override
	public void testUpdate() throws Exception {
		Project project = project();
		String uuid = project.getUuid();
		role().grantPermissions(project, UPDATE_PERM);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		assertEquals(0, searchProvider.getStoreEvents().size());
		Future<ProjectResponse> future = getClient().updateProject(uuid, request);
		latchFor(future);
		assertSuccess(future);
		ProjectResponse restProject = future.result();
		test.assertProject(request, restProject);
		assertTrue(searchProvider.getStoreEvents().size() != 0);

		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			Project reloadedProject = rh.result();
			reloadedProject.reload();
			assertEquals("New Name", reloadedProject.getName());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("new Name");

		Future<ProjectResponse> future = getClient().updateProject("bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws JsonProcessingException, Exception {
		Project project = project();
		String uuid = project.getUuid();
		String name = project.getName();
		role().grantPermissions(project, READ_PERM);
		role().revokePermissions(project, UPDATE_PERM);

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		Future<ProjectResponse> future = getClient().updateProject(uuid, request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			Project reloadedProject = rh.result();
			assertEquals("The name should not have been changed", name, reloadedProject.getName());
			latch.countDown();
		});
		failingLatch(latch);
	}

	// Delete Tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		Project project = project();
		String uuid = project.getUuid();
		String name = project.getName();
		assertNotNull(uuid);
		assertNotNull(name);
		role().grantPermissions(project, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("project_deleted", future, uuid + "/" + name);
		assertElement(meshRoot().getProjectRoot(), uuid, false);
		// TODO check for removed routers?
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		Project project = project();
		String uuid = project.getUuid();
		role().revokePermissions(project, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getProjectRoot().findByUuid(uuid, rh -> {
			assertNotNull("The project should not have been deleted", rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws Exception {
		int nJobs = 5;
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().updateProject(project().getUuid(), request));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override

	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		String uuid = project().getUuid();
		// CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findProjectByUuid(uuid));
		}
		validateSet(set, null);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		String uuid = project().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<GenericMessageResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().deleteProject(uuid));
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 100;
		int nProjectsBefore = meshRoot().getProjectRoot().findAll().size();

		//CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("test12345_" + i);
			set.add(getClient().createProject(request));
		}
		validateCreation(set, null);

		try (Trx tx = db.trx()) {
			int n = 0;
			for (Vertex vertex : tx.getGraph().getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, ProjectImpl.class.getName())) {
				n++;
			}
			int nProjectsAfter = meshRoot().getProjectRoot().findAll().size();
			assertEquals(nProjectsBefore + nJobs, nProjectsAfter);
			assertEquals(nProjectsBefore + nJobs, n);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		Set<Future<ProjectResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findProjectByUuid(project().getUuid()));
		}
		for (Future<ProjectResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

}
