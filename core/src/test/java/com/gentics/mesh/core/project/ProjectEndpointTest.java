package com.gentics.mesh.core.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.core.data.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.admin.migration.MigrationStatus.COMPLETED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.ClientHelper.validateSet;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.MeshTestHelper.prepareBarrier;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static com.gentics.mesh.test.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static java.lang.Math.ceil;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.router.RouterStorage;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;
import com.syncleus.ferma.tx.Tx;
import com.syncleus.ferma.typeresolvers.PolymorphicTypeResolver;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = true)
public class ProjectEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	public void testCreateNoSchemaReference() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test1234");
		call(() -> client().createProject(request), BAD_REQUEST, "project_error_no_schema_reference");

		request.setSchema(new SchemaReferenceImpl());
		call(() -> client().createProject(request), BAD_REQUEST, "project_error_no_schema_reference");
	}

	@Test
	public void testCreateBogusSchemaReference() {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test1234");
		request.setSchema(new SchemaReferenceImpl().setName("bogus42"));
		call(() -> client().createProject(request), BAD_REQUEST, "error_schema_reference_not_found", "bogus42", "-", "-");
	}

	@Test
	public void testCreateBogusName() {
		String name = "Tä\u1F921 üst";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchema(new SchemaReferenceImpl().setName("folder"));

		ProjectResponse restProject = call(() -> client().createProject(request));
		assertEquals("The name of the project did not match.", name, restProject.getName());

		NodeResponse response = call(() -> client().findNodeByUuid(name, restProject.getRootNode().getUuid(), new VersioningParametersImpl()
			.setVersion("draft")));
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
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(request), BAD_REQUEST, "project_error_name_already_reserved", name);
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchema(new SchemaReferenceImpl().setName("folder"));

		CompletableFuture<JsonObject> fut = new CompletableFuture<>();
		vertx().eventBus().consumer(Project.TYPE_INFO.getOnCreatedAddress(), (Message<JsonObject> rh) -> {
			fut.complete(rh.body());
		});

		ProjectResponse restProject = call(() -> client().createProject(request));

		// Verify that the new routes have been created
		NodeResponse response = call(() -> client().findNodeByUuid(name, restProject.getRootNode().getUuid(), new VersioningParametersImpl()
			.setVersion("draft")));
		assertEquals("folder", response.getSchema().getName());

		JsonObject eventInfo = fut.get(10, TimeUnit.SECONDS);
		assertEquals(name, eventInfo.getString("name"));
		assertEquals(restProject.getUuid(), eventInfo.getString("uuid"));

		assertThat(restProject).matches(request);
		try (Tx tx = tx()) {
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
		request.setSchema(new SchemaReferenceImpl().setName("folder"));

		try (Tx tx = tx()) {
			role().revokePermissions(meshRoot().getProjectRoot(), CREATE_PERM);
			tx.success();
		}

		String projectRootUuid = db().tx(() -> meshRoot().getProjectRoot().getUuid());
		call(() -> client().createProject(request), FORBIDDEN, "error_missing_perm", projectRootUuid, CREATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		try (Tx noTx = db().tx()) {
			String uuid = UUIDUtil.randomUUID();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("New Name");
			request.setSchema(new SchemaReferenceImpl().setName("folder"));

			assertThat(trackingSearchProvider()).hasNoStoreEvents();
			ProjectResponse restProject = call(() -> client().createProject(uuid, request));

			assertThat(restProject).hasUuid(uuid);

			Project reloadedProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertEquals("New Name", reloadedProject.getName());
		}
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		try (Tx noTx = db().tx()) {
			String uuid = user().getUuid();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("New Name");
			request.setSchemaRef("folder");

			assertThat(trackingSearchProvider()).hasNoStoreEvents();
			call(() -> client().createProject(uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
		}
	}

	@Test
	public void testCreateWithHostname() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setHostname("dummy.host");
		request.setSsl(true);
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		call(() -> client().createProject(request));

		BranchResponse response1 = call(() -> client().findBranches(name)).getData().get(0);
		assertThat(response1).hasHostname("dummy.host").hasSSL(true);

		BranchUpdateRequest updateRequest = new BranchUpdateRequest();
		updateRequest.setHostname("different.host");
		updateRequest.setSsl(null);
		BranchResponse response2 = call(() -> client().updateBranch(name, response1.getUuid(), updateRequest));
		assertThat(response2).hasHostname("different.host").hasSSL(true);
	}

	@Test
	public void testCreateWithPathPrefix() throws Exception {
		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setHostname("dummy.host");
		request.setPathPrefix("my/prefix");
		request.setSchema(new SchemaReferenceImpl().setName("folder"));
		call(() -> client().createProject(request));

		BranchResponse branch = call(() -> client().findBranches(name)).getData().get(0);
		assertThat(branch).hasPathPrefix("my/prefix");

	}

	@Test
	public void testCreateDeleteMultiple() throws InterruptedException {
		final String NAME = "dummy123";
		for (int i = 0; i < 500; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(NAME);
			request.setHostname("dummy.host");
			request.setSsl(true);
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			ProjectResponse response = call(() -> client().createProject(request));
			String baseUuid = response.getRootNode().getUuid();

			call(() -> client().deleteProject(response.getUuid()));

			call(() -> client().findNodeByUuid(NAME, baseUuid), NOT_FOUND, "project_not_found", NAME);
		}
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		try (Tx tx = tx()) {
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			role().grantPermissions(project().getBaseNode(), CREATE_PERM);
			role().revokePermissions(meshRoot(), CREATE_PERM, DELETE_PERM, UPDATE_PERM, READ_PERM);
			tx.success();
		}

		final String name = "test12345";
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(name);
		request.setSchema(new SchemaReferenceImpl().setName("folder"));

		try (Tx tx = tx()) {
			// Create a new project
			ProjectResponse restProject = call(() -> client().createProject(request));
			assertThat(restProject).matches(request);
			assertThat(restProject.getPermissions()).hasPerm(Permission.values());

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
		final int nProjects = 142;
		final String noPermProjectName = "no_perm_project";
		try (Tx tx = tx()) {
			role().grantPermissions(project(), READ_PERM);
			tx.success();
		}
		try (Tx tx = tx()) {
			for (int i = 0; i < nProjects; i++) {
				Project extraProject = meshRoot().getProjectRoot().create("extra_project_" + i, null, null, null, user(), schemaContainer("folder")
					.getLatestVersion());
				extraProject.setBaseNode(project().getBaseNode());
				role().grantPermissions(extraProject, READ_PERM);
			}
			meshRoot().getProjectRoot().create(noPermProjectName, null, null, null, user(), schemaContainer("folder").getLatestVersion());

			// Don't grant permissions to no perm project
			tx.success();
		}
		// Test default paging parameters
		ProjectListResponse restResponse = client().findProjects(new PagingParametersImpl()).blockingGet();
		assertNull(restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(nProjects + 1, restResponse.getData().size());

		long perPage = 11;
		restResponse = client().findProjects(new PagingParametersImpl(3, perPage)).blockingGet();
		assertEquals(perPage, restResponse.getData().size());

		// Extra projects + dummy project
		int totalProjects = nProjects + 1;
		int totalPages = (int) ceil(totalProjects / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage().longValue());
		assertEquals(totalProjects, restResponse.getMetainfo().getTotalCount());

		List<ProjectResponse> allProjects = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			final int currentPage = page;
			restResponse = call(() -> client().findProjects(new PagingParametersImpl(currentPage, perPage)));
			allProjects.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all projects were loaded when loading all pages.", totalProjects, allProjects.size());

		// Verify that the no perm project is not part of the response
		List<ProjectResponse> filteredProjectList = allProjects.parallelStream().filter(restProject -> restProject.getName().equals(
			noPermProjectName)).collect(toList());
		assertTrue("The no perm project should not be part of the list since no permissions were added.", filteredProjectList.size() == 0);

		call(() -> client().findProjects(new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

		call(() -> client().findProjects(new PagingParametersImpl(1, -1L)), BAD_REQUEST, "error_pagesize_parameter", "-1");

		ProjectListResponse listResponse = call(() -> client().findProjects(new PagingParametersImpl(4242, 25L)));

		assertNotNull(listResponse.toJson());

		assertEquals(4242, listResponse.getMetainfo().getCurrentPage());
		assertEquals(25, listResponse.getMetainfo().getPerPage().longValue());
		assertEquals(143, listResponse.getMetainfo().getTotalCount());
		assertEquals(6, listResponse.getMetainfo().getPageCount());
		assertEquals(0, listResponse.getData().size());
	}

	@Test
	public void testReadProjects() {

		for (int i = 0; i < 10; i++) {
			final String name = "test12345_" + i;
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(name);
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			call(() -> client().createProject(request));
		}

		// perPage: 0
		ProjectListResponse list = call(() -> client().findProjects(new PagingParametersImpl(1, 0L)));
		assertEquals("The page count should be one.", 0, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be zero", 0, list.getData().size());

		// perPage: 1
		list = call(() -> client().findProjects(new PagingParametersImpl(1, 1L)));
		assertEquals("The page count should be one.", 11, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 1, list.getData().size());

		// perPage: 2
		list = call(() -> client().findProjects(new PagingParametersImpl(1, 2L)));
		assertEquals("The page count should be one.", 6, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 2, list.getData().size());

		// page: 6
		list = call(() -> client().findProjects(new PagingParametersImpl(6, 2L)));
		assertEquals("The page count should be one.", 6, list.getMetainfo().getPageCount());
		assertEquals("Total count should be one.", 11, list.getMetainfo().getTotalCount());
		assertEquals("Total data size should be one.", 1, list.getData().size());
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (Tx tx = tx()) {
			Project project = project();
			String uuid = project.getUuid();
			assertNotNull("The UUID of the project must not be null.", project.getUuid());
			role().grantPermissions(project, READ_PERM, UPDATE_PERM);

			ProjectResponse response = call(() -> client().findProjectByUuid(uuid));
			assertThat(response).matches(project());
			System.out.println(response.getRootNode().getDisplayName());

			response = call(() -> client().findProjectByUuid(uuid, new NodeParametersImpl().setResolveLinks(LinkType.FULL)));
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
		try (Tx tx = tx()) {
			Project project = project();
			String uuid = project.getUuid();

			ProjectResponse response = call(() -> client().findProjectByUuid(uuid, new RolePermissionParametersImpl().setRoleUuid(role().getUuid())));
			assertNotNull(response.getRolePerms());
			assertThat(response.getRolePerms()).hasPerm(Permission.values());
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			role().revokePermissions(project(), READ_PERM);
			tx.success();
		}
		call(() -> client().findProjectByUuid(projectUuid()), FORBIDDEN, "error_missing_perm", projectUuid(), READ_PERM.getRestPerm().getName());
	}

	// Update Tests

	@Test
	public void testUpdateWithBogusNames() {
		String uuid = projectUuid();

		try (Tx tx = tx()) {
			MeshInternal.get().boot().meshRoot().getProjectRoot().create("Test234", null, null, null, user(), schemaContainer("folder").getLatestVersion());
			tx.success();
		}
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("Test234");
		call(() -> client().updateProject(uuid, request), CONFLICT, "project_conflicting_name");

		// Test slashes
		request.setName("Bla/blub");
		call(() -> client().updateProject(uuid, request));
		call(() -> client().findNodes(request.getName()));

		try (Tx tx = tx()) {
			assertEquals(request.getName(), project().getName());
		}

	}

	@Test
	public void testUpdateByAppendingToName() {
		String uuid = projectUuid();

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("abc");
		call(() -> client().updateProject(uuid, request));
		call(() -> client().findNodes(request.getName()));

		request.setName("abcd");
		call(() -> client().updateProject(uuid, request));
		call(() -> client().findNodes(request.getName()));
	}

	@Test
	public void testUpdateWithEndpointName() {
		List<String> names = Arrays.asList("users", "groups", "projects");
		for (String name : names) {
			String uuid = projectUuid();
			ProjectUpdateRequest request = new ProjectUpdateRequest();
			request.setName(name);
			call(() -> client().updateProject(uuid, request), BAD_REQUEST, "project_error_name_already_reserved", name);
		}
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		String uuid = projectUuid();
		try (Tx tx = tx()) {
			Project project = project();
			role().grantPermissions(project, UPDATE_PERM);
			tx.success();
		}

		String newName = "New Name";
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName(newName);
		assertThat(trackingSearchProvider()).hasNoStoreEvents();
		ProjectResponse restProject = call(() -> client().updateProject(uuid, request));

		// Assert that the routerstorage was updates
		assertTrue("The new project router should have been added", RouterStorage.hasProject(newName));
		call(() -> client().findNodes(newName));

		try (Tx tx = tx()) {
			Project project = project();
			assertThat(restProject).matches(project);
			// All nodes + project + tags and tag families need to be reindex
			// since the project name is part of the search document.
			int expectedCount = 1;
			for (Node node : project().getNodeRoot().findAll()) {
				expectedCount += node.getGraphFieldContainerCount();
			}
			expectedCount += meshRoot().getTagRoot().computeCount();
			expectedCount += project.getTagFamilyRoot().computeCount();

			assertThat(trackingSearchProvider()).hasStore(Project.composeIndexName(), Project.composeDocumentId(uuid));
			assertThat(trackingSearchProvider()).hasEvents(expectedCount, 0, 0, 0);

			Project reloadedProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertEquals(newName, reloadedProject.getName());
		}

	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("new Name");
		call(() -> client().updateProject("bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws JsonProcessingException, Exception {
		String uuid = projectUuid();
		try (Tx tx = tx()) {
			Project project = project();
			role().grantPermissions(project, READ_PERM);
			role().revokePermissions(project, UPDATE_PERM);
			tx.success();
		}

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		call(() -> client().updateProject(uuid, request), FORBIDDEN, "error_missing_perm", uuid, UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			Project reloadedProject = meshRoot().getProjectRoot().findByUuid(uuid);
			assertEquals("The name should not have been changed", PROJECT_NAME, reloadedProject.getName());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String uuid = projectUuid();
		try (Tx tx = tx()) {
			role().grantPermissions(project(), DELETE_PERM);
			tx.success();
		}

		Set<String> indices = new HashSet<>();
		Set<Tuple<String, String>> documentDeletes = new HashSet<>();
		try (Tx tx = tx()) {
			Project project = project();
			for (Node node : project.getNodeRoot().findAll()) {
				for (NodeGraphFieldContainer ngfc : node.getGraphFieldContainersIt(initialBranchUuid(), PUBLISHED)) {
					String idx = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
						ngfc.getSchemaContainerVersion().getUuid(), PUBLISHED);
					String did = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), ngfc.getLanguage().getLanguageTag());
					documentDeletes.add(Tuple.tuple(idx, did));
				}
				for (NodeGraphFieldContainer ngfc : node.getGraphFieldContainersIt(initialBranchUuid(), DRAFT)) {
					String idx = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
						ngfc.getSchemaContainerVersion().getUuid(), DRAFT);
					String did = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), ngfc.getLanguage().getLanguageTag());
					documentDeletes.add(Tuple.tuple(idx, did));
				}
			}

			// 1. Determine a list all project indices which must be dropped
			for (Branch branch : project.getBranchRoot().findAll()) {
				for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
					String schemaContainerVersionUuid = version.getUuid();
					indices.add(NodeGraphFieldContainer.composeIndexName(uuid, branch.getUuid(), schemaContainerVersionUuid, PUBLISHED));
					indices.add(NodeGraphFieldContainer.composeIndexName(uuid, branch.getUuid(), schemaContainerVersionUuid, DRAFT));
				}
			}
		}

		// 2. Delete the project
		call(() -> client().deleteProject(uuid));

		// 3. Assert that the indices have been dropped and the project has been
		// deleted from the project index
		for (Tuple<String, String> entry : documentDeletes) {
			assertThat(trackingSearchProvider()).hasDelete(entry.v1(), entry.v2());
		}
		assertThat(trackingSearchProvider()).hasDelete(Project.composeIndexName(), Project.composeDocumentId(uuid));
		assertThat(trackingSearchProvider()).hasDrop(TagFamily.composeIndexName(uuid));
		assertThat(trackingSearchProvider()).hasDrop(Tag.composeIndexName(uuid));
		for (String index : indices) {
			assertThat(trackingSearchProvider()).hasDrop(index);
		}
		assertThat(trackingSearchProvider()).hasEvents(0, 1 + 8, 2 + indices.size(), 0);

		try (Tx tx = tx()) {
			assertElement(meshRoot().getProjectRoot(), uuid, false);
		}
		// TODO check for removed routers?

	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		String uuid = projectUuid();
		try (Tx tx = tx()) {
			role().revokePermissions(project(), DELETE_PERM);
			tx.success();
		}

		call(() -> client().deleteProject(uuid), FORBIDDEN, "error_missing_perm", uuid, DELETE_PERM.getRestPerm().getName(), DELETE_PERM.getRestPerm().getName());
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0);

		try (Tx tx = tx()) {
			Project project = meshRoot().getProjectRoot().findByUuid(uuid);
			assertNotNull("The project should not have been deleted", project);
		}

	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws Exception {
		try (Tx tx = tx()) {
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
		try (Tx tx = tx()) {
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
		long nProjectsBefore = meshRoot().getProjectRoot().computeCount();

		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("test12345_" + i);
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			set.add(client().createProject(request).invoke());
		}
		validateCreation(set, null);

		try (Tx tx = tx()) {
			long n = StreamSupport.stream(tx.getGraph().getVertices(PolymorphicTypeResolver.TYPE_RESOLUTION_KEY, ProjectImpl.class.getName())
				.spliterator(), true).count();
			long nProjectsAfter = meshRoot().getProjectRoot().computeCount();
			assertEquals(nProjectsBefore + nJobs, nProjectsAfter);
			assertEquals(nProjectsBefore + nJobs, n);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		try (Tx tx = tx()) {
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

	@Test
	public void testDeleteWithBranches() throws Exception {
		String uuid = projectUuid();
		String branchName = "Branch_V1";

		tx(() -> group().addRole(roles().get("admin")));

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);

		waitForJobs(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Branch Response").isNotNull().hasName(branchName).isActive().isNotMigrated();
		}, COMPLETED, 1);

		// update a node in all branches
		String nodeUuid = tx(() -> folder("2015").getUuid());
		for (BranchResponse branch : call(() -> client().findBranches(PROJECT_NAME)).getData()) {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("name", FieldUtil.createStringField("2015 in " + branch.getName()));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParametersImpl().setBranch(branch.getName())));
		}

		checkConsistency();

		call(() -> client().deleteProject(uuid));
	}
}
