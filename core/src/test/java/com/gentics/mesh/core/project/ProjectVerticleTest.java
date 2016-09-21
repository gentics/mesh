package com.gentics.mesh.core.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
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
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicIsolatedCrudVerticleTest;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.AbstractVerticle;

public class ProjectVerticleTest extends AbstractBasicIsolatedCrudVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.projectVerticle());
		list.add(meshDagger.nodeVerticle());
		return list;
	}

	// Create Tests

	@Test
	public void testCreateNoSchemaReference() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test1234");
		call(() -> getClient().createProject(request), BAD_REQUEST, "project_error_no_schema_reference");

		request.setSchemaReference(new SchemaReference());
		call(() -> getClient().createProject(request), BAD_REQUEST, "project_error_no_schema_reference");
	}

	@Test
	public void testCreateBogusSchemaReference() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test1234");
		request.setSchemaReference(new SchemaReference().setName("bogus42"));
		call(() -> getClient().createProject(request), BAD_REQUEST, "error_schema_reference_not_found", "bogus42", "-", "-");
	}

	@Test
	public void testCreateBogusName() {
		String name = "Tä\u1F921 üst";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchemaReference(new SchemaReference().setName("folder"));

		ProjectResponse restProject = call(() -> getClient().createProject(request));
		assertEquals("The name of the project did not match.", name, restProject.getName());

		NodeResponse response = call(
				() -> getClient().findNodeByUuid(name, restProject.getRootNodeUuid(), new VersioningParameters().setVersion("draft")));
		assertEquals("folder", response.getSchema().getName());

	}

	@Test
	@Override
	public void testCreate() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchemaReference(new SchemaReference().setName("folder"));

		ProjectResponse restProject = call(() -> getClient().createProject(request));

		NodeResponse response = call(
				() -> getClient().findNodeByUuid(name, restProject.getRootNodeUuid(), new VersioningParameters().setVersion("draft")));
		assertEquals("folder", response.getSchema().getName());

		assertThat(restProject).matches(request);
		try (NoTx noTx = db.noTx()) {
			assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));
			Project project = meshRoot().getProjectRoot().findByUuid(restProject.getUuid());
			assertNotNull(project);
			assertTrue(user().hasPermission(project, CREATE_PERM));
			assertTrue(user().hasPermission(project.getBaseNode(), CREATE_PERM));
			assertTrue(user().hasPermission(project.getTagFamilyRoot(), CREATE_PERM));
			assertTrue(user().hasPermission(project.getNodeRoot(), CREATE_PERM));

			assertEquals("folder", project.getBaseNode().getSchemaContainer().getLatestVersion().getName());
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchemaReference(new SchemaReference().setName("folder"));

		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getProjectRoot(), CREATE_PERM);
		}

		String projectRootUuid = db.noTx(() -> meshRoot().getProjectRoot().getUuid());
		call(() -> getClient().createProject(request), FORBIDDEN, "error_missing_perm", projectRootUuid);
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			role().revokePermissions(meshRoot(), CREATE_PERM, DELETE_PERM, UPDATE_PERM, READ_PERM);
		}

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchemaReference(new SchemaReference().setName("folder"));

		try (NoTx noTx = db.noTx()) {
			// Create a new project
			ProjectResponse restProject = call(() -> getClient().createProject(request));
			assertThat(restProject).matches(request);
			assertEquals(6, restProject.getPermissions().length);

			meshRoot().getProjectRoot().reload();
			assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));

			// Read the project
			MeshResponse<ProjectResponse> readFuture = getClient().findProjectByUuid(restProject.getUuid()).invoke();
			latchFor(readFuture);
			assertSuccess(readFuture);

			// Now delete the project
			call(() -> getClient().deleteProject(restProject.getUuid()));
		}
	}

	// Read Tests

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().grantPermissions(project(), READ_PERM);
		}
		try (NoTx noTx = db.noTx()) {
			final int nProjects = 142;
			String noPermProjectName;
			for (int i = 0; i < nProjects; i++) {
				Project extraProject = meshRoot().getProjectRoot().create("extra_project_" + i, user(), schemaContainer("folder").getLatestVersion());
				extraProject.setBaseNode(project().getBaseNode());
				role().grantPermissions(extraProject, READ_PERM);
			}
			Project noPermProject = meshRoot().getProjectRoot().create("no_perm_project", user(), schemaContainer("folder").getLatestVersion());
			noPermProjectName = noPermProject.getName();

			// Don't grant permissions to no perm project

			// Test default paging parameters
			MeshResponse<ProjectListResponse> future = getClient().findProjects(new PagingParameters()).invoke();
			latchFor(future);
			assertSuccess(future);
			ProjectListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 11;
			future = getClient().findProjects(new PagingParameters(3, perPage)).invoke();
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
				final int currentPage = page;
				restResponse = call(() -> getClient().findProjects(new PagingParameters(currentPage, perPage)));
				allProjects.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all projects were loaded when loading all pages.", totalProjects, allProjects.size());

			// Verify that the no perm project is not part of the response
			List<ProjectResponse> filteredProjectList = allProjects.parallelStream()
					.filter(restProject -> restProject.getName().equals(noPermProjectName)).collect(Collectors.toList());
			assertTrue("The no perm project should not be part of the list since no permissions were added.", filteredProjectList.size() == 0);

			future = getClient().findProjects(new PagingParameters(-1, perPage)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			future = getClient().findProjects(new PagingParameters(1, -1)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = getClient().findProjects(new PagingParameters(4242, 25)).invoke();
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
	}

	@Test
	public void testReadProjects() {

		for (int i = 0; i < 10; i++) {
			final String name = "test12345_" + i;
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(name);
			request.setSchemaReference(new SchemaReference().setName("folder"));
			call(() -> getClient().createProject(request));
		}

		// perPage: 0
		ProjectListResponse list = call(() -> getClient().findProjects(new PagingParameters(1, 0)));
		assertEquals("The page count should be one.", 0, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be zero", 0, list.getData().size());

		// perPage: 1
		list = call(() -> getClient().findProjects(new PagingParameters(1, 1)));
		assertEquals("The page count should be one.", 11, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 1, list.getData().size());

		// perPage: 2
		list = call(() -> getClient().findProjects(new PagingParameters(1, 2)));
		assertEquals("The page count should be one.", 6, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 2, list.getData().size());

		// page: 6
		list = call(() -> getClient().findProjects(new PagingParameters(6, 2)));
		assertEquals("The page count should be one.", 6, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 1, list.getData().size());

	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			assertNotNull("The UUID of the project must not be null.", project.getUuid());
			role().grantPermissions(project, READ_PERM, UPDATE_PERM);

			MeshResponse<ProjectResponse> future = getClient().findProjectByUuid(uuid).invoke();
			latchFor(future);
			assertSuccess(future);
			ProjectResponse restProject = future.result();
			assertThat(restProject).matches(project());

			List<String> permissions = Arrays.asList(restProject.getPermissions());
			assertTrue(permissions.contains("create"));
			assertTrue(permissions.contains("read"));
			assertTrue(permissions.contains("update"));
			assertTrue(permissions.contains("delete"));
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();

			MeshResponse<ProjectResponse> future = getClient().findProjectByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid()))
					.invoke();
			latchFor(future);
			assertSuccess(future);
			assertNotNull(future.result().getRolePerms());
			assertEquals(6, future.result().getRolePerms().length);
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			assertNotNull("The UUID of the project must not be null.", project.getUuid());
			role().revokePermissions(project, READ_PERM);

			MeshResponse<ProjectResponse> future = getClient().findProjectByUuid(uuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	// Update Tests

	@Test
	public void testUpdateWithConflicitingName() {
		try (NoTx noTx = db.noTx()) {
			MeshRoot.getInstance().getProjectRoot().create("Test234", user(), schemaContainer("folder").getLatestVersion());

			Project project = project();
			String uuid = project.getUuid();
			role().grantPermissions(project, UPDATE_PERM);
			ProjectUpdateRequest request = new ProjectUpdateRequest();
			request.setName("Test234");
			MeshResponse<ProjectResponse> future = getClient().updateProject(uuid, request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "project_conflicting_name");
		}

	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			role().grantPermissions(project, UPDATE_PERM);

			ProjectUpdateRequest request = new ProjectUpdateRequest();
			request.setName("New Name");

			assertThat(dummySearchProvider).hasNoStoreEvents();
			ProjectResponse restProject = call(() -> getClient().updateProject(uuid, request));
			project.reload();
			assertThat(restProject).matches(project);
			// All nodes need to be reindex since the project name is part of the search document.
			assertThat(dummySearchProvider).recordedStoreEvents(57);

			Project reloadedProject = meshRoot().getProjectRoot().findByUuid(uuid);
			reloadedProject.reload();
			assertEquals("New Name", reloadedProject.getName());
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("new Name");

		MeshResponse<ProjectResponse> future = getClient().updateProject("bogus", request).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws JsonProcessingException, Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			String name = project.getName();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);

			ProjectUpdateRequest request = new ProjectUpdateRequest();
			request.setName("New Name");

			MeshResponse<ProjectResponse> future = getClient().updateProject(uuid, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);

			Project reloadedProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertEquals("The name should not have been changed", name, reloadedProject.getName());
		}
	}

	// Delete Tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().grantPermissions(project(), DELETE_PERM);
		}

		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			String name = project.getName();
			assertNotNull(uuid);
			assertNotNull(name);
			MeshResponse<Void> future = getClient().deleteProject(uuid).invoke();
			latchFor(future);
			assertSuccess(future);
			assertElement(meshRoot().getProjectRoot(), uuid, false);
			// TODO check for removed routers?
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();
			role().revokePermissions(project, DELETE_PERM);

			MeshResponse<Void> future = getClient().deleteProject(uuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);

			project = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNotNull("The project should not have been deleted", project);
		}
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws Exception {
		try (NoTx noTx = db.noTx()) {
			int nJobs = 5;
			ProjectUpdateRequest request = new ProjectUpdateRequest();
			request.setName("New Name");

			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().updateProject(project().getUuid(), request).invoke());
			}
			validateSet(set, barrier);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (NoTx noTx = db.noTx()) {
			String uuid = project().getUuid();
			// CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findProjectByUuid(uuid).invoke());
			}
			validateSet(set, null);
		}
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		String uuid = project().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<Void>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().deleteProject(uuid).invoke());
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Ignore("Disabled since test is unstable - CL-246")
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 100;
		int nProjectsBefore = meshRoot().getProjectRoot().findAll().size();

		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("test12345_" + i);
			request.setSchemaReference(new SchemaReference().setName("folder"));
			set.add(getClient().createProject(request).invoke());
		}
		validateCreation(set, null);

		try (Tx tx = db.tx()) {
			long n = StreamSupport.stream(tx.getGraph().getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, ProjectImpl.class.getName()).spliterator(),true).count();
			int nProjectsAfter = meshRoot().getProjectRoot().findAll().size();
			assertEquals(nProjectsBefore + nJobs, nProjectsAfter);
			assertEquals(nProjectsBefore + nJobs, n);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		try (NoTx noTx = db.noTx()) {
			int nJobs = 200;
			Set<MeshResponse<ProjectResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findProjectByUuid(project().getUuid()).invoke());
			}
			for (MeshResponse<ProjectResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

}
