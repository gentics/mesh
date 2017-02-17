package com.gentics.mesh.core.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.expectException;
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
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

public class ProjectEndpointTest extends AbstractBasicCrudEndpointTest {

	@Test
	public void testCreateNoSchemaReference() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test1234");
		call(() -> client().createProject(request), BAD_REQUEST, "project_error_no_schema_reference");

		request.setSchema(new SchemaReference());
		call(() -> client().createProject(request), BAD_REQUEST, "project_error_no_schema_reference");
	}

	@Test
	public void testCreateBogusSchemaReference() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test1234");
		request.setSchema(new SchemaReference().setName("bogus42"));
		call(() -> client().createProject(request), BAD_REQUEST, "error_schema_reference_not_found", "bogus42", "-", "-");
	}

	@Test
	public void testCreateBogusName() {
		String name = "Tä\u1F921 üst";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchema(new SchemaReference().setName("folder"));

		ProjectResponse restProject = call(() -> client().createProject(request));
		assertEquals("The name of the project did not match.", name, restProject.getName());

		NodeResponse response = call(
				() -> client().findNodeByUuid(name, restProject.getRootNode().getUuid(), new VersioningParameters().setVersion("draft")));
		assertEquals("folder", response.getSchema().getName());

		// Test slashes
		request.setName("Bla/blub");
		call(() -> client().createProject(request));
		call(() -> client().findNodes(request.getName()));

	}

	@Test
	public void testCreateWithEndpointNames() {
		List<String> names = Arrays.asList("users", "groups", "projects");
		for (String name : names) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(name);
			request.setSchema(new SchemaReference().setName("folder"));
			call(() -> client().createProject(request), BAD_REQUEST, "project_error_name_already_reserved", name);
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchema(new SchemaReference().setName("folder"));

		ProjectResponse restProject = call(() -> client().createProject(request));

		NodeResponse response = call(
				() -> client().findNodeByUuid(name, restProject.getRootNode().getUuid(), new VersioningParameters().setVersion("draft")));
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
		request.setSchema(new SchemaReference().setName("folder"));

		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getProjectRoot(), CREATE_PERM);
		}

		String projectRootUuid = db.noTx(() -> meshRoot().getProjectRoot().getUuid());
		call(() -> client().createProject(request), FORBIDDEN, "error_missing_perm", projectRootUuid);
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
		request.setSchema(new SchemaReference().setName("folder"));

		try (NoTx noTx = db.noTx()) {
			// Create a new project
			ProjectResponse restProject = call(() -> client().createProject(request));
			assertThat(restProject).matches(request);
			assertThat(restProject.getPermissions()).hasPerm(Permission.values());

			meshRoot().getProjectRoot().reload();
			assertNotNull("The project should have been created.", meshRoot().getProjectRoot().findByName(name));

			// Read the project
			call(() -> client().findProjectByUuid(restProject.getUuid()));

			// Now delete the project
			call(() -> client().deleteProject(restProject.getUuid()));
		}
	}

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
			MeshResponse<ProjectListResponse> future = client().findProjects(new PagingParametersImpl()).invoke();
			latchFor(future);
			assertSuccess(future);
			ProjectListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 11;
			future = client().findProjects(new PagingParametersImpl(3, perPage)).invoke();
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
				restResponse = call(() -> client().findProjects(new PagingParametersImpl(currentPage, perPage)));
				allProjects.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all projects were loaded when loading all pages.", totalProjects, allProjects.size());

			// Verify that the no perm project is not part of the response
			List<ProjectResponse> filteredProjectList = allProjects.parallelStream()
					.filter(restProject -> restProject.getName().equals(noPermProjectName)).collect(Collectors.toList());
			assertTrue("The no perm project should not be part of the list since no permissions were added.", filteredProjectList.size() == 0);

			call(() -> client().findProjects(new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			call(() -> client().findProjects(new PagingParametersImpl(1, -1)), BAD_REQUEST, "error_pagesize_parameter", "-1");

			ProjectListResponse listResponse = call(() -> client().findProjects(new PagingParametersImpl(4242, 25)));

			String response = JsonUtil.toJson(listResponse);
			assertNotNull(response);

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
			request.setSchema(new SchemaReference().setName("folder"));
			call(() -> client().createProject(request));
		}

		// perPage: 0
		ProjectListResponse list = call(() -> client().findProjects(new PagingParametersImpl(1, 0)));
		assertEquals("The page count should be one.", 0, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be zero", 0, list.getData().size());

		// perPage: 1
		list = call(() -> client().findProjects(new PagingParametersImpl(1, 1)));
		assertEquals("The page count should be one.", 11, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 1, list.getData().size());

		// perPage: 2
		list = call(() -> client().findProjects(new PagingParametersImpl(1, 2)));
		assertEquals("The page count should be one.", 6, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 2, list.getData().size());

		// page: 6
		list = call(() -> client().findProjects(new PagingParametersImpl(6, 2)));
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

			ProjectResponse response = call(() ->  client().findProjectByUuid(uuid));
			assertThat(response).matches(project());
			System.out.println(response.getRootNode().getDisplayName());

			response = call(() ->  client().findProjectByUuid(uuid, new NodeParameters().setResolveLinks(LinkType.FULL)));
			assertNotNull(response.getRootNode().getPath());
			
			PermissionInfo permissions = response.getPermissions();
			assertTrue(permissions.hasPerm(CREATE));
			assertTrue(permissions.hasPerm(READ));
			assertTrue(permissions.hasPerm(UPDATE));
			assertTrue(permissions.hasPerm(DELETE));
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();

			ProjectResponse response = call(() -> client().findProjectByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())));
			assertNotNull(response.getRolePerms());
			assertThat(response.getRolePerms()).hasPerm(Permission.values());
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

			call(() -> client().findProjectByUuid(uuid), FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	// Update Tests

	@Test
	public void testUpdateWithBogusNames() {
		try (NoTx noTx = db.noTx()) {
			MeshInternal.get().boot().meshRoot().getProjectRoot().create("Test234", user(), schemaContainer("folder").getLatestVersion());

			String uuid = project().getUuid();
			ProjectUpdateRequest request = new ProjectUpdateRequest();
			request.setName("Test234");
			call(() -> client().updateProject(uuid, request), CONFLICT, "project_conflicting_name");

			// Test slashes
			request.setName("Bla/blub");
			call(() -> client().updateProject(uuid, request));
			call(() -> client().findNodes(request.getName()));
			project().reload();
			assertEquals(request.getName(), project().getName());

		}
	}

	@Test
	public void testUpdateWithEndpointName() {
		List<String> names = Arrays.asList("users", "groups", "projects");
		try (NoTx noTx = db.noTx()) {
			for (String name : names) {
				Project project = project();
				String uuid = project.getUuid();
				ProjectUpdateRequest request = new ProjectUpdateRequest();
				request.setName(name);
				call(() -> client().updateProject(uuid, request), BAD_REQUEST, "project_error_name_already_reserved", name);
			}
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
			ProjectResponse restProject = call(() -> client().updateProject(uuid, request));
			project.reload();
			assertThat(restProject).matches(project);
			// All nodes  + project + tags and tag families need to be reindex since the project name is part of the search document.
			int expectedCount = 1;
			for (Node node : project().getNodeRoot().findAll()) {
				expectedCount += node.getGraphFieldContainerCount();
			}
			expectedCount += project.getTagRoot().findAll().size();
			expectedCount += project.getTagFamilyRoot().findAll().size();

			assertThat(dummySearchProvider).hasStore(Project.composeIndexName(), Project.composeIndexType(), Project.composeDocumentId(uuid));
			assertThat(dummySearchProvider).hasEvents(expectedCount, 0, 0, 0);

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
		call(() -> client().updateProject("bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");

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

			call(() -> client().updateProject(uuid, request), FORBIDDEN, "error_missing_perm", uuid);

			Project reloadedProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertEquals("The name should not have been changed", name, reloadedProject.getName());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().grantPermissions(project(), DELETE_PERM);
		}

		try (NoTx noTx = db.noTx()) {
			Project project = project();
			String uuid = project.getUuid();

			//1. Determine a list all project indices which must be dropped
			Set<String> indices = new HashSet<>();
			for (Release release : project.getReleaseRoot().findAll()) {
				for (SchemaContainerVersion version : release.findAllSchemaVersions()) {
					String schemaContainerVersionUuid = version.getUuid();
					indices.add(NodeGraphFieldContainer.composeIndexName(uuid, release.getUuid(), schemaContainerVersionUuid, PUBLISHED));
					indices.add(NodeGraphFieldContainer.composeIndexName(uuid, release.getUuid(), schemaContainerVersionUuid, DRAFT));
				}
			}

			String name = project.getName();
			assertNotNull(uuid);
			assertNotNull(name);

			//2. Delete the project
			call(() -> client().deleteProject(uuid));

			//3. Assert that the indices have been dropped and the project has been deleted from the project index
			assertThat(dummySearchProvider).hasDelete(Project.composeIndexName(), Project.composeIndexType(), Project.composeDocumentId(uuid));
			assertThat(dummySearchProvider).hasDrop(TagFamily.composeIndexName(uuid));
			assertThat(dummySearchProvider).hasDrop(Tag.composeIndexName(uuid));
			for (String index : indices) {
				assertThat(dummySearchProvider).hasDrop(index);
			}
			assertThat(dummySearchProvider).hasEvents(0, 1, 2 + indices.size(), 0);

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
			call(() -> client().deleteProject(uuid), FORBIDDEN, "error_missing_perm", uuid);
			assertThat(dummySearchProvider).hasEvents(0, 0, 0, 0);
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
				set.add(client().updateProject(project().getUuid(), request).invoke());
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
				set.add(client().findProjectByUuid(uuid).invoke());
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
			set.add(client().deleteProject(uuid).invoke());
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
			request.setSchema(new SchemaReference().setName("folder"));
			set.add(client().createProject(request).invoke());
		}
		validateCreation(set, null);

		try (Tx tx = db.tx()) {
			long n = StreamSupport
					.stream(tx.getGraph().getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, ProjectImpl.class.getName()).spliterator(), true)
					.count();
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
				set.add(client().findProjectByUuid(project().getUuid()).invoke());
			}
			for (MeshResponse<ProjectResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

}
