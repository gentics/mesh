package com.gentics.mesh.test;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.junit.After;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.I18NUtil;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.project.ProjectUpdateRequest;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagFieldContainer;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.rest.MeshRestClientHttpException;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.util.FieldUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;

public abstract class AbstractIsolatedRestVerticleTest extends AbstractDBTest {

	protected Vertx vertx;

	protected int port;

	private MeshRestClient client;

	@Autowired
	private RouterStorage routerStorage;

	@Autowired
	protected DummySearchProvider searchProvider;

	@Autowired
	protected AuthenticationVerticle authenticationVerticle;

	@Before
	public void setupVerticleTest() throws Exception {
		setupData();
		port = com.gentics.mesh.test.performance.TestUtils.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(TestDataProvider.PROJECT_NAME);

		JsonObject config = new JsonObject();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", null, config, Thread.currentThread().getContextClassLoader());

		CountDownLatch latch = new CountDownLatch(getVertices().size());

		// Inject spring config and start each verticle
		for (AbstractSpringVerticle verticle : getVertices()) {
			verticle.setSpringConfig(springConfig);
			verticle.init(vertx, context);
			Future<Void> future = Future.future();
			verticle.start(future);
			future.setHandler(rh -> {
				latch.countDown();
			});
		}

		failingLatch(latch);

		try (NoTrx trx = db.noTrx()) {
			client = MeshRestClient.create("localhost", getPort(), vertx,
					Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod());
			client.setLogin(user().getUsername(), getUserInfo().getPassword());
			client.login();
		}
	}

	public HttpClient createHttpClient() {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost("localhost");
		options.setDefaultPort(port);
		HttpClient client = Mesh.vertx().createHttpClient(options);
		return client;
	}

	@After
	public void cleanup() throws Exception {
		searchProvider.reset();
		for (AbstractSpringVerticle verticle : getVertices()) {
			verticle.stop();
		}
		resetDatabase();

	}

	public abstract List<AbstractSpringVerticle> getAdditionalVertices();

	private List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = getAdditionalVertices();
		list.add(authenticationVerticle);
		return list;
	}

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
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
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
	protected TagResponse createTag(String projectName, String tagFamilyUuid, String tagName) {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.getFields().setName(tagName);
		Future<TagResponse> future = getClient().createTag(projectName, tagFamilyUuid, tagCreateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagResponse readTag(String projectName, String tagFamilyUuid, String uuid) {
		Future<TagResponse> future = getClient().findTagByUuid(projectName, tagFamilyUuid, uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagResponse updateTag(String projectName, String tagFamilyUuid, String uuid, String newTagName) {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setFields(new TagFieldContainer().setName(newTagName));
		Future<TagResponse> future = getClient().updateTag(projectName, tagFamilyUuid, uuid, tagUpdateRequest);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteTag(projectName, tagFamilyUuid, uuid);
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

	protected Future<NodeResponse> createNodeAsync(String fieldKey, Field field) {
		Node parentNode = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNodeUuid(parentNode.getUuid());
		nodeCreateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		if (fieldKey != null) {
			nodeCreateRequest.getFields().put(fieldKey, field);
		}
		return getClient().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParameters().setLanguages("en"));
	}

	protected NodeResponse createNode(String fieldKey, Field field) {
		NodeResponse response = call(() -> createNodeAsync(fieldKey, field));
		assertNotNull("The response could not be found in the result of the future.", response);
		if (fieldKey != null) {
			assertNotNull("The field was not included in the response.", response.getFields().hasField(fieldKey));
		}
		return response;
	}

	protected NodeResponse readNode(String projectName, String uuid) {
		return call(() -> getClient().findNodeByUuid(projectName, uuid, new VersioningParameters().draft()));
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

	/**
	 * Migrate the node from one release to another
	 * 
	 * @param projectName
	 *            project name
	 * @param uuid
	 *            node Uuid
	 * @param sourceReleaseName
	 *            source release name
	 * @param targetReleaseName
	 *            target release name
	 * @return migrated node
	 */
	protected NodeResponse migrateNode(String projectName, String uuid, String sourceReleaseName, String targetReleaseName) {
		// read node from source release
		NodeResponse nodeResponse = call(
				() -> getClient().findNodeByUuid(projectName, uuid, new VersioningParameters().setRelease(sourceReleaseName).draft()));

		Schema schema = schemaContainer(nodeResponse.getSchema().getName()).getLatestVersion().getSchema();

		// update node for target release
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(nodeResponse.getLanguage());

		nodeResponse.getFields().keySet().forEach(key -> update.getFields().put(key, nodeResponse.getFields().getField(key, schema.getField(key))));
		return call(() -> getClient().updateNode(projectName, uuid, update, new VersioningParameters().setRelease(targetReleaseName)));
	}

	// Project
	protected ProjectResponse createProject(String projectName) {
		ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
		projectCreateRequest.setName(projectName);
		projectCreateRequest.setSchemaReference(new SchemaReference().setName("folder"));
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
	protected Schema createSchema(String schemaName) {
		Schema schema = FieldUtil.createMinimalValidSchema();
		schema.setName(schemaName);
		Future<Schema> future = getClient().createSchema(schema);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected Schema readSchema(String uuid) {
		Future<Schema> future = getClient().findSchemaByUuid(uuid);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected GenericMessageResponse updateSchema(String uuid, String schemaName) {
		Schema schema = FieldUtil.createMinimalValidSchema();
		schema.setName(schemaName);
		Future<GenericMessageResponse> future = getClient().updateSchema(uuid, schema);
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteSchema(String uuid) {
		Future<GenericMessageResponse> future = getClient().deleteSchema(uuid);
		latchFor(future);
		assertSuccess(future);
	}

	// Microschema

	protected Microschema createMicroschema(String microschemaName) {
		Microschema microschema = FieldUtil.createMinimalValidMicroschema();
		microschema.setName(microschemaName);
		return call(() -> getClient().createMicroschema(microschema));
	}

	protected GenericMessageResponse updateMicroschema(String uuid, String microschemaName) {
		Microschema microschema = FieldUtil.createMinimalValidMicroschema();
		microschema.setName(microschemaName);
		return call(() -> getClient().updateMicroschema(uuid, microschema));
	}

	public void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
	}

	protected void expectResponseMessage(Future<GenericMessageResponse> responseFuture, String i18nKey, String... i18nParams) {
		assertTrue("The given future has not yet completed.", responseFuture.isComplete());
		expectResponseMessage(responseFuture.result(), i18nKey, i18nParams);
	}

	protected void expectResponseMessage(GenericMessageResponse response, String i18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, i18nKey, i18nParams);
		assertEquals("The response message does not match.", message, response.getMessage());
	}

	protected void expectFailureMessage(Future<?> future, HttpResponseStatus status, String message) {
		assertTrue("We expected the future to have failed but it succeeded.", future.failed());
		assertNotNull(future.cause());

		if (future.cause() instanceof MeshRestClientHttpException) {
			MeshRestClientHttpException exception = ((MeshRestClientHttpException) future.cause());
			assertEquals("The status code of the nested exception did not match the expected value.", status.code(), exception.getStatusCode());
			assertEquals(message, exception.getMessage());
		} else {
			future.cause().printStackTrace();
			fail("Unhandled exception");
		}
	}

	protected void expectException(Future<?> future, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, bodyMessageI18nKey, i18nParams);
		assertNotEquals("Translation for key " + bodyMessageI18nKey + " not found", message, bodyMessageI18nKey);
		expectFailureMessage(future, status, message);
	}

	/**
	 * Call the given handler, latch for the future and assert success. Then return the result.
	 * 
	 * @param handler
	 *            handler
	 * @param <T>
	 *            type of the returned object
	 * @return result of the future
	 */
	protected <T> T call(ClientHandler<T> handler) {
		Future<T> future;
		try {
			future = handler.handle();
		} catch (Exception e) {
			future = Future.failedFuture(e);
		}
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	/**
	 * Call the given handler, latch for the future and expect the given failure in the future
	 *
	 * @param handler
	 *            handler
	 * @param status
	 *            expected response status
	 * @param bodyMessageI18nKey
	 *            i18n of the expected response message
	 * @param i18nParams
	 *            parameters of the expected response message
	 */
	protected <T> void call(ClientHandler<T> handler, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
		Future<T> future;
		try {
			future = handler.handle();
		} catch (Exception e) {
			future = Future.failedFuture(e);
		}
		latchFor(future);
		expectException(future, status, bodyMessageI18nKey, i18nParams);
	}

	/**
	 * 
	 * @author norbert
	 *
	 * @param <T>
	 */
	@FunctionalInterface
	protected static interface ClientHandler<T> {
		Future<T> handle() throws Exception;
	}
}
