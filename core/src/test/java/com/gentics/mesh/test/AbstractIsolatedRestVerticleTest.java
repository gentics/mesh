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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.Mesh;
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
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
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
import com.gentics.mesh.dagger.MeshCore;
import com.gentics.mesh.demo.TestDataProvider;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientHttpException;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.impl.EventLoopContext;
import io.vertx.core.impl.VertxInternal;
import io.vertx.core.json.JsonObject;
import io.vertx.test.core.TestUtils;

public abstract class AbstractIsolatedRestVerticleTest extends AbstractDBTest {

	protected Vertx vertx;

	protected int port;

	private MeshRestClient client;

	@Before
	public void setupVerticleTest() throws Exception {
		super.setup();

		//TODO move dir creation to init of MeshTestModule
		File uploadDir = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory());
		FileUtils.deleteDirectory(uploadDir);
		uploadDir.mkdirs();

		File tempDir = new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory());
		FileUtils.deleteDirectory(tempDir);
		tempDir.mkdirs();

		File imageCacheDir = new File(Mesh.mesh().getOptions().getImageOptions().getImageCacheDirectory());
		FileUtils.deleteDirectory(imageCacheDir);
		imageCacheDir.mkdirs();

		Mesh.mesh().getOptions().getUploadOptions().setByteLimit(Long.MAX_VALUE);

		setupData();
		port = com.gentics.mesh.test.performance.TestUtils.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(TestDataProvider.PROJECT_NAME);

		JsonObject config = new JsonObject();
		config.put("port", port);
		EventLoopContext context = ((VertxInternal) vertx).createEventLoopContext("test", null, config,
				Thread.currentThread().getContextClassLoader());

		CountDownLatch latch = new CountDownLatch(getVertices().size());

		// Start each verticle
		for (AbstractVerticle verticle : getVertices()) {
			verticle.init(vertx, context);
			Future<Void> future = Future.future();
			verticle.start(future);
			future.setHandler(rh -> {
				latch.countDown();
			});
		}

		failingLatch(latch);

		try (NoTx trx = db.noTx()) {
			client = MeshRestClient.create("localhost", getPort(), vertx,
					Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod());
			client.setLogin(user().getUsername(), getUserInfo().getPassword());
			client.login().toBlocking().value();
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
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getImageOptions().getImageCacheDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory()));
		FileUtils.deleteDirectory(new File(Mesh.mesh().getOptions().getUploadOptions().getTempDirectory()));
		searchProvider.reset();
		for (AbstractVerticle verticle : getVertices()) {
			verticle.stop();
		}
		resetDatabase();

	}

	public abstract List<AbstractVerticle> getAdditionalVertices();

	private List<AbstractVerticle> getVertices() {
		List<AbstractVerticle> list = getAdditionalVertices();
		list.add(meshDagger.authenticationVerticle());
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

		MeshResponse<UserResponse> future = getClient().createUser(request).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected UserResponse readUser(String uuid) {
		MeshResponse<UserResponse> future = getClient().findUserByUuid(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected UserResponse updateUser(String uuid, String newUserName) {
		UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
		userUpdateRequest.setUsername(newUserName);
		MeshResponse<UserResponse> future = getClient().updateUser(uuid, userUpdateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteUser(String uuid) {
		MeshResponse<Void> future = getClient().deleteUser(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	// Group

	protected GroupResponse createGroup(String groupName) {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(groupName);
		MeshResponse<GroupResponse> future = getClient().createGroup(request).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected GroupResponse readGroup(String uuid) {
		MeshResponse<GroupResponse> future = getClient().findGroupByUuid(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected GroupResponse updateGroup(String uuid, String newGroupName) {
		GroupUpdateRequest groupUpdateRequest = new GroupUpdateRequest();
		groupUpdateRequest.setName(newGroupName);
		MeshResponse<GroupResponse> future = getClient().updateGroup(uuid, groupUpdateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteGroup(String uuid) {
		MeshResponse<Void> future = getClient().deleteGroup(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	// Role

	protected RoleResponse createRole(String roleName, String groupUuid) {
		RoleCreateRequest roleCreateRequest = new RoleCreateRequest();
		roleCreateRequest.setName(roleName);
		MeshResponse<RoleResponse> future = getClient().createRole(roleCreateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected RoleResponse readRole(String uuid) {
		MeshResponse<RoleResponse> future = getClient().findRoleByUuid(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteRole(String uuid) {
		MeshResponse<Void> future = getClient().deleteRole(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	protected RoleResponse updateRole(String uuid, String newRoleName) {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName(newRoleName);
		MeshResponse<RoleResponse> future = getClient().updateRole(uuid, request).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	// Tag
	protected TagResponse createTag(String projectName, String tagFamilyUuid, String tagName) {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.getFields().setName(tagName);
		MeshResponse<TagResponse> future = getClient().createTag(projectName, tagFamilyUuid, tagCreateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagResponse readTag(String projectName, String tagFamilyUuid, String uuid) {
		MeshResponse<TagResponse> future = getClient().findTagByUuid(projectName, tagFamilyUuid, uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagResponse updateTag(String projectName, String tagFamilyUuid, String uuid, String newTagName) {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setFields(new TagFieldContainer().setName(newTagName));
		MeshResponse<TagResponse> future = getClient().updateTag(projectName, tagFamilyUuid, uuid, tagUpdateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteTag(String projectName, String tagFamilyUuid, String uuid) {
		MeshResponse<Void> future = getClient().deleteTag(projectName, tagFamilyUuid, uuid).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	// Node

	protected NodeResponse createNode(String projectName, String nameField) {
		NodeCreateRequest request = new NodeCreateRequest();
		MeshResponse<NodeResponse> future = getClient().createNode(projectName, request).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected MeshRequest<NodeResponse> createNodeAsync(String fieldKey, Field field) {
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
		MeshResponse<Void> future = getClient().deleteNode(projectName, uuid).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	protected NodeResponse updateNode(String projectName, String uuid, String nameFieldValue) {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		MeshResponse<NodeResponse> future = getClient().updateNode(projectName, uuid, nodeUpdateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagFamilyResponse createTagFamily(String projectName, String tagFamilyName) {
		TagFamilyCreateRequest tagFamilyCreateRequest = new TagFamilyCreateRequest();
		tagFamilyCreateRequest.setName(tagFamilyName);
		MeshResponse<TagFamilyResponse> future = getClient().createTagFamily(projectName, tagFamilyCreateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagFamilyResponse readTagFamily(String projectName, String uuid) {
		MeshResponse<TagFamilyResponse> future = getClient().findTagFamilyByUuid(projectName, uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected TagFamilyResponse updateTagFamily(String projectName, String uuid, String newTagFamilyName) {
		TagFamilyUpdateRequest tagFamilyUpdateRequest = new TagFamilyUpdateRequest();
		tagFamilyUpdateRequest.setName(newTagFamilyName);
		MeshResponse<TagFamilyResponse> future = getClient().updateTagFamily(projectName, uuid, tagFamilyUpdateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteTagFamily(String projectName, String uuid) {
		MeshResponse<Void> future = getClient().deleteTagFamily(projectName, uuid).invoke();
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
		MeshResponse<ProjectResponse> future = getClient().createProject(projectCreateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected ProjectResponse readProject(String uuid) {
		MeshResponse<ProjectResponse> future = getClient().findProjectByUuid(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected ProjectResponse updateProject(String uuid, String projectName) {
		ProjectUpdateRequest projectUpdateRequest = new ProjectUpdateRequest();
		projectUpdateRequest.setName(projectName);
		MeshResponse<ProjectResponse> future = getClient().updateProject(uuid, projectUpdateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteProject(String uuid) {
		MeshResponse<Void> future = getClient().deleteProject(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
	}

	// Schema
	protected Schema createSchema(String schemaName) {
		Schema schema = FieldUtil.createMinimalValidSchema();
		schema.setName(schemaName);
		MeshResponse<Schema> future = getClient().createSchema(schema).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected Schema readSchema(String uuid) {
		MeshResponse<Schema> future = getClient().findSchemaByUuid(uuid).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected GenericMessageResponse updateSchema(String uuid, String schemaName) {
		Schema schema = FieldUtil.createMinimalValidSchema();
		schema.setName(schemaName);
		MeshResponse<GenericMessageResponse> future = getClient().updateSchema(uuid, schema).invoke();
		latchFor(future);
		assertSuccess(future);
		return future.result();
	}

	protected void deleteSchema(String uuid) {
		MeshResponse<Void> future = getClient().deleteSchema(uuid).invoke();
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

	protected void expectResponseMessage(MeshResponse<GenericMessageResponse> responseFuture, String i18nKey, String... i18nParams) {
		assertTrue("The given future has not yet completed.", responseFuture.isComplete());
		expectResponseMessage(responseFuture.result(), i18nKey, i18nParams);
	}

	protected void expectResponseMessage(GenericMessageResponse response, String i18nKey, String... i18nParams) {
		Locale en = Locale.ENGLISH;
		String message = I18NUtil.get(en, i18nKey, i18nParams);
		assertEquals("The response message does not match.", message, response.getMessage());
	}

	protected void expectFailureMessage(MeshResponse<?> future, HttpResponseStatus status, String message) {
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

	protected void expectException(MeshResponse<?> future, HttpResponseStatus status, String bodyMessageI18nKey, String... i18nParams) {
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
		MeshResponse<T> future;
		try {
			future = handler.handle().invoke();
		} catch (Exception e) {
			future = new MeshResponse<>(Future.failedFuture(e));
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
		MeshResponse<T> future;
		try {
			future = handler.handle().invoke();
		} catch (Exception e) {
			future = new MeshResponse<>(Future.failedFuture(e));
		}
		latchFor(future);
		expectException(future, status, bodyMessageI18nKey, i18nParams);
	}

	@FunctionalInterface
	protected static interface ClientHandler<T> {
		MeshRequest<T> handle() throws Exception;
	}

	/**
	 * Prepare the schema of the given node by adding the binary content field to its schema fields. This method will also update the clientside schema storage.
	 * 
	 * @param node
	 * @param mimeTypeWhitelist
	 * @param binaryFieldName
	 * @throws IOException
	 */
	protected void prepareSchema(Node node, String mimeTypeWhitelist, String binaryFieldName) throws IOException {
		// Update the schema and enable binary support for folders
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(new BinaryFieldSchemaImpl().setAllowedMimeTypes(mimeTypeWhitelist).setName(binaryFieldName).setLabel("Binary content"));
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		MeshCore.get().serverSchemaStorage().clear();
		// node.getSchemaContainer().setSchema(schema);
	}

	protected MeshRequest<GenericMessageResponse> uploadRandomData(String uuid, String languageTag, String fieldKey, int binaryLen,
			String contentType, String fileName) {

		// role().grantPermissions(node, UPDATE_PERM);
		Buffer buffer = TestUtils.randomBuffer(binaryLen);
		return getClient().updateNodeBinaryField(PROJECT_NAME, uuid, languageTag, fieldKey, buffer, fileName, contentType);
	}

	protected void uploadImage(Node node, String languageTag, String fieldName) throws IOException {
		String contentType = "image/jpeg";
		String fileName = "blume.jpg";
		prepareSchema(node, "image/.*", fieldName);

		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);

		MeshResponse<GenericMessageResponse> future = getClient()
				.updateNodeBinaryField(PROJECT_NAME, node.getUuid(), languageTag, fieldName, buffer, fileName, contentType).invoke();
		latchFor(future);
		assertSuccess(future);
	}

}
