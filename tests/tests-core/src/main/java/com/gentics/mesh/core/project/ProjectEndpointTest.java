package com.gentics.mesh.core.project;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.BRANCH_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.NODE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_SCHEMA_UNASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.PROJECT_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.TAG_FAMILY_DELETED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.isFailureMessage;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static java.lang.Math.ceil;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.tag.HibTag;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.branch.BranchUpdateRequest;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.event.node.NodeMeshEventModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectListResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.LinkType;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SortingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.vertx.core.eventbus.DeliveryOptions;

@MeshTestSetting(elasticsearch = TRACKING, testSize = TestSize.FULL, startServer = true)
public class ProjectEndpointTest extends AbstractMeshTest implements BasicRestTestcases {
	@Test
	public void testRenameDeleteCreateProject() throws InterruptedException, TimeoutException {
		// create project named "project"
		ProjectResponse project = createProject("project");

		// get tag families of project (this will put project into cache)
		call(() -> client().findTagFamilies("project"));
		tx(() -> assertThat(mesh().projectNameCache().get("project")).as("Cached project").isNotNull());

		// rename project to "newproject"
		project = updateProject(project.getUuid(), "newproject");
		assertThat(waitFor(() -> tx(() -> mesh().projectNameCache().get("project")) == null, 10_000)).as("Project not found in cache any more").isTrue();

		long maxWaitMs = DeliveryOptions.DEFAULT_TIMEOUT;
		long delayMs = 100;
		boolean repeat = false;

		// get tag families of newproject (this will put project into cache)
		long start = System.currentTimeMillis();
		do {
			try {
				call(() -> client().findTagFamilies("newproject"));
				repeat = false;
			} catch (Throwable t) {
				if (isFailureMessage(t, HttpResponseStatus.NOT_FOUND) && (System.currentTimeMillis() - start) < maxWaitMs) {
					Thread.sleep(delayMs);
					repeat = true;
				} else {
					throw t;
				}
			}
		} while (repeat);
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);

		// delete "newproject"
		deleteProject(project.getUuid());
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// create (again)
		project = createProject("project");
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(0);

		// get tag families of project
		call(() -> client().findTagFamilies("project"));
		assertThat(mesh().projectNameCache().size()).as("Project name cache size").isEqualTo(1);
	}

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

		expect(PROJECT_CREATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(name).uuidNotNull();
		});

		// Base node of the project
		expect(NODE_CREATED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event).uuidNotNull();
			assertNull("No name should be set for the base node.", event.getName());
		});

		ProjectResponse restProject = call(() -> client().createProject(request));

		awaitEvents();

		// Verify that the new routes have been created
		NodeResponse response = call(() -> client().findNodeByUuid(name, restProject.getRootNode().getUuid(), new VersioningParametersImpl()
			.setVersion("draft")));
		assertEquals("folder", response.getSchema().getName());

		assertThat(restProject).matches(request);
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			assertNotNull("The project should have been created.", tx.projectDao().findByName(name));
			HibProject project = tx.projectDao().findByUuid(restProject.getUuid());
			assertNotNull(project);
			assertTrue(userDao.hasPermission(user(), project, CREATE_PERM));
			assertTrue(userDao.hasPermission(user(), project.getBaseNode(), CREATE_PERM));
			assertTrue(userDao.hasPermission(user(), project.getTagFamilyPermissionRoot(), CREATE_PERM));
			assertTrue(userDao.hasPermission(user(), project.getNodePermissionRoot(), CREATE_PERM));

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
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), tx.data().permissionRoots().project(), CREATE_PERM);
			tx.success();
		}

		String projectRootUuid = db().tx(() -> Tx.get().data().permissionRoots().project().getUuid());
		call(() -> client().createProject(request), FORBIDDEN, "error_missing_perm", projectRootUuid, CREATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		try (Tx tx = tx()) {
			String uuid = UUIDUtil.randomUUID();

			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("New Name");
			request.setSchema(new SchemaReferenceImpl().setName("folder"));

			assertThat(trackingSearchProvider()).hasNoStoreEvents();
			ProjectResponse restProject = call(() -> client().createProject(uuid, request));

			assertThat(restProject).hasUuid(uuid);

			HibProject reloadedProject = tx.projectDao().findByUuid(uuid);
			assertEquals("New Name", reloadedProject.getName());
		}
	}

	@Test
	@Override
	@Ignore("Not valid over dup UUIDs being allowed globally")
	public void testCreateWithDuplicateUuid() throws Exception {
		try (Tx noTx = tx()) {
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
			RoleDao roleDao = tx.roleDao();
			roleDao.grantPermissions(role(), project().getBaseNode(), CREATE_PERM);
			roleDao.grantPermissions(role(), project().getBaseNode(), CREATE_PERM);
			roleDao.grantPermissions(role(), project().getBaseNode(), CREATE_PERM);
			roleDao.revokePermissions(role(), tx.data().permissionRoots().mesh(), CREATE_PERM, DELETE_PERM, UPDATE_PERM, READ_PERM);
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
			assertThat(restProject.getPermissions()).hasPerm(Permission.basicPermissions());

			assertNotNull("The project should have been created.", tx.projectDao().findByName(name));

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
			RoleDao roleDao = tx.roleDao();
			roleDao.grantPermissions(role(), project(), READ_PERM);
			tx.success();
		}
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			for (int i = 0; i < nProjects; i++) {
				HibProject extraProject = createProject("extra_project_" + i, "folder");
				extraProject.setBaseNode(project().getBaseNode());
				roleDao.grantPermissions(role(), extraProject, READ_PERM);
			}
			createProject(noPermProjectName, "folder");

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

		verifySorting(param -> call(() -> client().findProjects(param)), ProjectResponse::getName, "name", "List of project names");
	}

	@Test
	@Override
	public void testReadPermittedSorted() throws Exception {
		for (int i = 0; i < 10; i++) {
			final String name = "test12345_" + i;
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(name);
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			ProjectResponse response = call(() -> client().createProject(request));
			if ((i % 2) == 0) {
				tx(tx -> {
					tx.roleDao().revokePermissions(role(), tx.projectDao().findByUuid(response.getUuid()), READ_PERM);
				});
			}
		}
		ProjectListResponse list = call(() -> client().findProjects(new SortingParametersImpl("name", SortOrder.DESCENDING)));
		assertEquals("Total data size should be 6", 6, list.getData().size());
		assertThat(list.getData()).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
				fa != null ? fa.getName() : null,
				fb != null ? fb.getName() : null));
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
			RoleDao roleDao = tx.roleDao();
			HibProject project = project();
			String uuid = project.getUuid();
			assertNotNull("The UUID of the project must not be null.", project.getUuid());
			roleDao.grantPermissions(role(), project, READ_PERM, UPDATE_PERM);

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
			HibProject project = project();
			String uuid = project.getUuid();

			ProjectResponse response = call(() -> client().findProjectByUuid(uuid, new RolePermissionParametersImpl().setRoleUuid(role().getUuid())));
			assertNotNull(response.getRolePerms());
			assertThat(response.getRolePerms()).hasPerm(Permission.basicPermissions());
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), project(), READ_PERM);
			tx.success();
		}
		call(() -> client().findProjectByUuid(projectUuid()), FORBIDDEN, "error_missing_perm", projectUuid(), READ_PERM.getRestPerm().getName());
	}

	// Update Tests

	@Test
	public void testUpdateWithBogusNames() throws TimeoutException {
		String uuid = projectUuid();

		tx(() -> createProject("Test234", "folder"));
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("Test234");
		call(() -> client().updateProject(uuid, request), CONFLICT, "project_conflicting_name");

		// Test slashes
		request.setName("Bla/blub");
		call(() -> client().updateProject(uuid, request));
		assertThat(waitForSuccess(() -> call(() -> client().findNodes(request.getName())), 10_000))
			.as("Fetching nodes for project %s succeeded".formatted(request.getName())).isTrue();

		try (Tx tx = tx()) {
			assertEquals(request.getName(), Tx.get().projectDao().findByUuid(projectUuid()).getName());
		}

	}

	@Test
	public void testUpdateByAppendingToName() throws TimeoutException {
		String uuid = projectUuid();

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("abc");
		call(() -> client().updateProject(uuid, request));
		assertThat(waitForSuccess(() -> call(() -> client().findNodes(request.getName())), 10_000))
				.as("Fetching nodes for project %s succeeded".formatted(request.getName())).isTrue();

		request.setName("abcd");
		call(() -> client().updateProject(uuid, request));
		assertThat(waitForSuccess(() -> call(() -> client().findNodes(request.getName())), 10_000))
			.as("Fetching nodes for project %s succeeded".formatted(request.getName())).isTrue();
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
			RoleDao roleDao = tx.roleDao();
			HibProject project = project();
			roleDao.grantPermissions(role(), project, UPDATE_PERM);
			tx.success();
		}

		String newName = "New Name";
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName(newName);
		assertThat(trackingSearchProvider()).hasNoStoreEvents();

		expect(PROJECT_UPDATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(newName).uuidNotNull();
		});

		ProjectResponse restProject = call(() -> client().updateProject(uuid, request));

		awaitEvents();
		waitForSearchIdleEvent();

		// Assert that the routerstorage was updates
		assertTrue("The new project router should have been added", mesh().routerStorageRegistry().hasProject(newName));
		call(() -> client().findNodes(newName));

		try (Tx tx = tx()) {
			HibProject project = tx.projectDao().findByUuid(projectUuid());
			assertThat(restProject).matches(project);
			// All nodes + project + tags and tag families need to be reindex
			// since the project name is part of the search document.
			int expectedCount = 1;
			for (HibNode node : tx.nodeDao().findAll(project())) {
				expectedCount += tx.contentDao().getFieldContainerCount(node);
			}
			expectedCount += tx.tagDao().count();
			expectedCount += tx.tagFamilyDao().count(project);

			assertThat(trackingSearchProvider()).hasStore(HibProject.composeIndexName(), HibProject.composeDocumentId(uuid));
			assertThat(trackingSearchProvider()).hasEvents(expectedCount, 0, 0, 0, 0);

			HibProject reloadedProject = tx.projectDao().findByUuid(uuid);
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
			RoleDao roleDao = tx.roleDao();
			HibProject project = project();
			roleDao.grantPermissions(role(), project, READ_PERM);
			roleDao.revokePermissions(role(), project, UPDATE_PERM);
			tx.success();
		}

		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		call(() -> client().updateProject(uuid, request), FORBIDDEN, "error_missing_perm", uuid, UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			HibProject reloadedProject = tx.projectDao().findByUuid(uuid);
			assertEquals("The name should not have been changed", PROJECT_NAME, reloadedProject.getName());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String uuid = projectUuid();
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.grantPermissions(role(), project(), DELETE_PERM);
			tx.success();
		}

		Set<String> droppedIndices = new HashSet<>();
		// Set<Tuple<String, String>> documentDeletes = new HashSet<>();
		try (Tx tx = tx()) {
			HibProject project = project();
			// for (Node node : project.getNodeRoot().findAll()) {
			// for (NodeGraphFieldContainer ngfc : node.getGraphFieldContainersIt(initialBranchUuid(), PUBLISHED)) {
			// String idx = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
			// ngfc.getSchemaContainerVersion().getUuid(), PUBLISHED);
			// String did = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), ngfc.getLanguageTag());
			// documentDeletes.add(Tuple.tuple(idx, did));
			// }
			// for (NodeGraphFieldContainer ngfc : node.getGraphFieldContainersIt(initialBranchUuid(), DRAFT)) {
			// String idx = NodeGraphFieldContainer.composeIndexName(projectUuid(), initialBranchUuid(),
			// ngfc.getSchemaContainerVersion().getUuid(), DRAFT);
			// String did = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), ngfc.getLanguageTag());
			// documentDeletes.add(Tuple.tuple(idx, did));
			// }
			// }

			droppedIndices.add(HibTagFamily.composeIndexName(projectUuid()));
			droppedIndices.add(HibTag.composeIndexName(projectUuid()));

			// 1. Determine a list all project indices which must be dropped
			droppedIndices.add(ContentDao.composeIndexPattern(uuid));
			// for (Branch branch : project.getBranchRoot().findAll()) {
			// for (SchemaContainerVersion version : branch.findActiveSchemaVersions()) {
			// String schemaContainerVersionUuid = version.getUuid();
			// }
			// }
		}

		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		expect(PROJECT_DELETED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(PROJECT_NAME).hasUuid(projectUuid());
		}).one();

		expect(NODE_DELETED).match(1, NodeMeshEventModel.class, event -> {
			assertThat(event).hasUuid(baseNodeUuid);
		});

		// The default branch should be deleted
		expect(BRANCH_DELETED).one();
		// The schemas: folder, content should be unassigned from the project
		expect(PROJECT_SCHEMA_UNASSIGNED).total(3);
		// Colors and basic should be deleted
		expect(TAG_FAMILY_DELETED).total(2);

		// 2. Delete the project
		call(() -> client().deleteProject(uuid));

		awaitEvents();
		waitForSearchIdleEvent();

		// 3. Assert that the indices have been dropped and the project has been
		// deleted from the project index
		// for (Tuple<String, String> entry : documentDeletes) {
		// assertThat(trackingSearchProvider()).hasDelete(entry.v1(), entry.v2());
		// }

		assertThat(trackingSearchProvider()).hasDelete(HibProject.composeIndexName(), HibProject.composeDocumentId(uuid));
		assertThat(trackingSearchProvider()).hasDrop(HibTagFamily.composeIndexName(uuid));
		assertThat(trackingSearchProvider()).hasDrop(HibTag.composeIndexName(uuid));
		for (String index : droppedIndices) {
			assertThat(trackingSearchProvider()).hasDrop(index);
		}
		// 1 project
		long deleted = 1;
		assertThat(trackingSearchProvider()).hasEvents(0, 0, deleted, droppedIndices.size(), 0);

		try (Tx tx = tx()) {
			assertElement(tx.projectDao(), uuid, false);
		}
		// TODO check for removed routers?

	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		String uuid = projectUuid();
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), project(), DELETE_PERM);
			tx.success();
		}

		call(() -> client().deleteProject(uuid), FORBIDDEN, "error_missing_perm", uuid, DELETE_PERM.getRestPerm().getName(),
			DELETE_PERM.getRestPerm().getName());
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 0, 0, 0);

		try (Tx tx = tx()) {
			HibProject project = tx.projectDao().findByUuid(uuid);
			assertNotNull("The project should not have been deleted", project);
		}

	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws Exception {
		int nJobs = 5;
		ProjectUpdateRequest request = new ProjectUpdateRequest();
		request.setName("New Name");

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().updateProject(projectUuid(), request).toCompletable())
			.blockingAwait();
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().findProjectByUuid(projectUuid()).toCompletable())
			.blockingAwait();
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		String uuid = project().getUuid();
		validateDeletion(i -> client().deleteProject(uuid), nJobs);
	}

	@Test
	@Ignore("Disabled since test is unstable - CL-246")
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 100;
		long nProjectsBefore = tx(tx -> { return tx.projectDao().count(); });

		validateCreation(nJobs, i -> {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("test12345_" + i);
			request.setSchema(new SchemaReferenceImpl().setName("folder"));
			return client().createProject(request);
		});

		try (Tx tx = tx()) {
			CommonTx ctx = tx.unwrap();
			long n = ctx.count(ctx.projectDao().getPersistenceClass());
			long nProjectsAfter = ctx.projectDao().count();
			assertEquals(nProjectsBefore + nJobs, nProjectsAfter);
			assertEquals(nProjectsBefore + nJobs, n);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> client().findProjectByUuid(projectUuid()).toCompletable())
			.blockingAwait();
	}

	@Test
	@Override
	public void testPermissionResponse() {
		ProjectResponse project = client().findProjects().blockingGet().getData().get(0);
		assertThat(project.getPermissions()).hasNoPublishPermsSet();
	}

	@Test
	public void testDeleteWithBranches() throws Exception {
		String uuid = projectUuid();
		String branchName = "Branch_V1";

		BranchCreateRequest request = new BranchCreateRequest();
		request.setName(branchName);

		waitForJob(() -> {
			BranchResponse response = call(() -> client().createBranch(PROJECT_NAME, request));
			assertThat(response).as("Branch Response").isNotNull().hasName(branchName).isActive().isNotMigrated();
		});

		// update a node in all branches
		String nodeUuid = tx(() -> folder("2015").getUuid());
		for (BranchResponse branch : call(() -> client().findBranches(PROJECT_NAME)).getData()) {
			NodeUpdateRequest update = new NodeUpdateRequest();
			update.setLanguage("en");
			update.setVersion("0.1");
			update.getFields().put("name", FieldUtil.createStringField("2015 in " + branch.getName()));
			call(() -> client().updateNode(PROJECT_NAME, nodeUuid, update, new VersioningParametersImpl().setBranch(branch.getName())));
		}

		consistency.check();

		call(() -> client().deleteProject(uuid));
	}

	@Test
	public void createProjectAfterDeletedRole() {
		RoleResponse role = createRole("test");
		client().updateRolePermissions(role.getUuid(), "/projects", RolePermissionRequest.withPermissions(CREATE)).blockingAwait();
		deleteRole(role.getUuid());
		createProject("testProject");
	}

	/**
	 * Test that the endpoints for /api/v[x]/projects is unaffected from deleting a project named "project"
	 */
	@Test
	public void testDeleteProjectNamedProject() {
		// create project named "project"
		ProjectResponse project = createProject("project");

		// get all projects
		ProjectListResponse list = call(() -> client().findProjects());
		assertThat(list.getData().stream().map(ProjectResponse::getName)).as("List of projects").containsOnly("dummy", "project");

		// delete project
		deleteProject(project.getUuid());

		// get the list of projects
		list = call(() -> client().findProjects());
		assertThat(list.getData().stream().map(ProjectResponse::getName)).as("List of projects").containsOnly("dummy");
	}
}
