package com.gentics.mesh.test;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.cli.Mesh;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.SchemaContainer;
import com.gentics.mesh.core.data.service.I18NService;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagFieldContainer;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.rest.MeshRestClientHttpException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractRestVerticleTest extends AbstractDBTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractRestVerticleTest.class);

	@Autowired
	private I18NService i18n;

	protected Vertx vertx;

	private int port;

	private MeshRestClient client;

	@Autowired
	private RouterStorage routerStorage;

	@Autowired
	protected DummySearchProvider searchProvider;

	@Before
	public void setupVerticleTest() throws Exception {
		setupData();
		port = com.gentics.mesh.test.TestUtil.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(DemoDataProvider.PROJECT_NAME);

		List<AbstractWebVerticle> vertices = getVertices();

		JsonObject config = new JsonObject();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", config, Thread.currentThread().getContextClassLoader());

		for (AbstractWebVerticle verticle : vertices) {
			// Inject spring config
			verticle.setSpringConfig(springConfig);
			verticle.init(vertx, context);
			verticle.start();
			verticle.registerEndPoints();
		}
		client = new MeshRestClient("localhost", getPort());
		client.setLogin(user().getUsername(), data().getUserInfo().getPassword());
		resetClientSchemaStorage();
	}

	@After
	public void cleanup() {
		searchProvider.reset();
		BootstrapInitializer.clearReferences();
		// databaseService.getDatabase().clear();
		databaseService.getDatabase().reset();
	}

	protected void resetClientSchemaStorage() throws IOException {
		getClient().getClientSchemaStorage().clear();
		for (SchemaContainer container : data().getSchemaContainers().values()) {
			getClient().getClientSchemaStorage().addSchema(container.getSchema());
		}
	}

	public abstract List<AbstractWebVerticle> getVertices();

	@After
	public void tearDown() throws Exception {
		if (client != null) {
			client.close();
		}
	}

	public int getPort() {
		return port;
	}

	public MeshRestClient getClient() {
		return client;
	}

	// User
	protected UserResponse createUser(String username) {
		String userName = "testuser42a";
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(userName);
		request.setPassword("test1234");
		request.setGroupUuid(group().getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected UserResponse readUser(String uuid) {
		Future<UserResponse> future = getClient().findUserByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected UserResponse updateUser(String uuid, String newUserName) {
		UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
		userUpdateRequest.setUsername(newUserName);
		Future<UserResponse> future = getClient().updateUser(uuid, userUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteUser(String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteUser(uuid);
		latchFor(future);
		assertSuccess(future);
	}

	// Group

	protected GroupResponse createGroup(String groupName) {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(groupName);
		Future<GroupResponse> future = getClient().createGroup(request);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected GroupResponse readGroup(String uuid) {
		Future<GroupResponse> future = getClient().findGroupByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected GroupResponse updateGroup(String uuid, String newGroupName) {
		GroupUpdateRequest groupUpdateRequest = new GroupUpdateRequest();
		groupUpdateRequest.setName(newGroupName);
		Future<GroupResponse> future = getClient().updateGroup(uuid, groupUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteGroup(String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteGroup(uuid);
		latchFor(future);
		assertSuccess(future);
	}

	// Role

	protected RoleResponse createRole(String roleName, String groupUuid) {
		RoleCreateRequest roleCreateRequest = new RoleCreateRequest();
		roleCreateRequest.setName(roleName);
		roleCreateRequest.setGroupUuid(groupUuid);
		Future<RoleResponse> future = getClient().createRole(roleCreateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected RoleResponse readRole(String uuid) {
		Future<RoleResponse> future = getClient().findRoleByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteRole(String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteRole(uuid);
		latchFor(future);
		assertSuccess(future);
	}

	protected RoleResponse updateRole(String uuid, String newRoleName) {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName(newRoleName);
		Future<RoleResponse> future = getClient().updateRole(uuid, request);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	// Tag
	protected TagResponse createTag(String projectName, String tagName, String tagFamilyName) {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setFields(new TagFieldContainer().setName(tagName));
		tagCreateRequest.setTagFamilyReference(new TagFamilyReference().setName(tagFamilyName));
		Future<TagResponse> future = getClient().createTag(projectName, tagCreateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagResponse readTag(String projectName, String uuid) {
		Future<TagResponse> future = getClient().findTagByUuid(projectName, uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagResponse updateTag(String projectName, String uuid, String newTagName) {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setFields(new TagFieldContainer().setName(newTagName));
		Future<TagResponse> future = getClient().updateTag(projectName, uuid, tagUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteTag(String projectName, String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteTag(projectName, uuid);
		latchFor(future);
		assertSuccess(future);
	}

	// Node

	protected NodeResponse createNode(String projectName, String nameField) {
		NodeCreateRequest request = new NodeCreateRequest();
		Future<NodeResponse> future = getClient().createNode(projectName, request);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected NodeResponse readNode(String projectName, String uuid) {
		Future<NodeResponse> future = getClient().findNodeByUuid(projectName, uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteNode(String projectName, String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteNode(projectName, uuid);
		latchFor(future);
		assertSuccess(future);
	}

	protected NodeResponse updateNode(String projectName, String uuid, String nameFieldValue) {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		Future<NodeResponse> future = getClient().updateNode(projectName, uuid, nodeUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	// TagFamily
	protected TagFamilyResponse createTagFamily(String projectName, String tagFamilyName) {
		TagFamilyCreateRequest tagFamilyCreateRequest = new TagFamilyCreateRequest();
		tagFamilyCreateRequest.setName(tagFamilyName);
		Future<TagFamilyResponse> future = getClient().createTagFamily(projectName, tagFamilyCreateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagFamilyResponse readTagFamily(String projectName, String uuid) {
		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(projectName, uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagFamilyResponse updateTagFamily(String projectName, String uuid, String newTagFamilyName) {
		TagFamilyUpdateRequest tagFamilyUpdateRequest = new TagFamilyUpdateRequest();
		tagFamilyUpdateRequest.setName(newTagFamilyName);
		Future<TagFamilyResponse> future = getClient().updateTagFamily(projectName, uuid, tagFamilyUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteTagFamily(String projectName, String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteTagFamily(projectName, uuid);
		latchFor(future);
		assertSuccess(future);
	}

	// Project
	protected ProjectResponse createProject(String projectName) {
		ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
		projectCreateRequest.setName(projectName);
		Future<ProjectResponse> future = getClient().createProject(projectCreateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected ProjectResponse readProject(String uuid) {
		Future<ProjectResponse> future = getClient().findProjectByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected ProjectResponse updateProject(String uuid, String projectName) {
		ProjectUpdateRequest projectUpdateRequest = new ProjectUpdateRequest();
		projectUpdateRequest.setName(projectName);
		Future<ProjectResponse> future = getClient().updateProject(uuid, projectUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteProject(String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteProject(uuid);
		latchFor(future);
		assertSuccess(future);
	}

	// Schema

	// Microschema

	public void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
	}

	protected void expectMessageResponse(String i18nKey, Future<GenericMessageResponse> responseFuture, String... i18nParams) {
		assertTrue("The given future has not yet completed.", responseFuture.isComplete());
		Locale en = Locale.ENGLISH;
		String message = i18n.get(en, i18nKey, i18nParams);
		assertEquals("The response message does not match.", message, responseFuture.result().getMessage());
	}

	protected void expectMessage(Future<?> future, HttpResponseStatus status, String message) {
		assertTrue("We expected the future to have failed but it succeeded.", future.failed());
		assertNotNull(future.cause());

		if (future.cause() instanceof MeshRestClientHttpException) {
			MeshRestClientHttpException exception = ((MeshRestClientHttpException) future.cause());
			assertEquals(status.code(), exception.getStatusCode());
			assertEquals(status.reasonPhrase(), exception.getMessage());
			assertNotNull(exception.getResponseMessage());
			assertEquals(message, exception.getResponseMessage().getMessage());
		} else {
			future.cause().printStackTrace();
			fail("Unhandled exception");
		}
	}

	protected void expectException(Future<?> future, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = i18n.get(en, bodyMessageI18nKey, i18nParams);
		expectMessage(future, status, message);
	}

}
