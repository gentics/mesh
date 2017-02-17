package com.gentics.mesh.test;

import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

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
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
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
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.BinaryFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.SchemaUpdateParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.RestAPIVerticle;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientHttpException;
import com.gentics.mesh.util.VersionNumber;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.test.core.TestUtils;

public abstract class AbstractRestEndpointTest extends AbstractDBTest {

	protected Vertx vertx;

	protected int port;

	private MeshRestClient client;

	private RestAPIVerticle restVerticle;

	private NodeMigrationVerticle nodeMigrationVerticle;

	private List<String> deploymentIds = new ArrayList<>();

	@Before
	public void setupVerticleTest() throws Exception {
		Mesh.mesh().getOptions().getUploadOptions().setByteLimit(Long.MAX_VALUE);

		port = com.gentics.mesh.test.performance.TestUtils.getRandomPort();
		vertx = Mesh.vertx();

		routerStorage.addProjectRouter(TestFullDataProvider.PROJECT_NAME);
		JsonObject config = new JsonObject();
		config.put("port", port);

		// Start node migration verticle
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		CountDownLatch latch = new CountDownLatch(1);
		nodeMigrationVerticle = meshDagger.nodeMigrationVerticle();
		vertx.deployVerticle(nodeMigrationVerticle, options, rh -> {
			String deploymentId = rh.result();
			deploymentIds.add(deploymentId);
			latch.countDown();
		});
		failingLatch(latch);

		// Start rest verticle
		CountDownLatch latch2 = new CountDownLatch(1);
		restVerticle = MeshInternal.get().restApiVerticle();
		vertx.deployVerticle(restVerticle, new DeploymentOptions().setConfig(config), rh -> {
			String deploymentId = rh.result();
			deploymentIds.add(deploymentId);
			latch2.countDown();
		});
		failingLatch(latch2);

		// Setup the rest client
		try (NoTx trx = db.noTx()) {
			client = MeshRestClient.create("localhost", getPort(), vertx);
			client.setLogin(user().getUsername(), getUserInfo().getPassword());
			client.login().toBlocking().value();
		}
		if (dummySearchProvider != null) {
			dummySearchProvider.clear();
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
	public void undeployAndReset() throws Exception {
		for (String id : deploymentIds) {
			vertx.undeploy(id);
		}
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

	public MeshRestClient client() {
		return client;
	}

	// User
	protected UserResponse createUser(String username) {
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		request.setGroupUuid(group().getUuid());

		return call(() -> client().createUser(request));
	}

	protected UserResponse readUser(String uuid) {
		return call(() -> client().findUserByUuid(uuid));
	}

	protected UserResponse updateUser(String uuid, String newUserName) {
		UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
		userUpdateRequest.setUsername(newUserName);
		return call(() -> client().updateUser(uuid, userUpdateRequest));
	}

	protected void deleteUser(String uuid) {
		call(() -> client().deleteUser(uuid));
	}

	// Group

	protected GroupResponse createGroup(String groupName) {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(groupName);
		return call(() -> client().createGroup(request));
	}

	protected GroupResponse readGroup(String uuid) {
		return call(() -> client().findGroupByUuid(uuid));
	}

	protected GroupResponse updateGroup(String uuid, String newGroupName) {
		GroupUpdateRequest groupUpdateRequest = new GroupUpdateRequest();
		groupUpdateRequest.setName(newGroupName);
		return call(() -> client().updateGroup(uuid, groupUpdateRequest));
	}

	protected void deleteGroup(String uuid) {
		call(() -> client().deleteGroup(uuid));
	}

	// Role
	protected RoleResponse createRole(String roleName, String groupUuid) {
		RoleCreateRequest roleCreateRequest = new RoleCreateRequest();
		roleCreateRequest.setName(roleName);
		return call(() -> client().createRole(roleCreateRequest));
	}

	protected RoleResponse readRole(String uuid) {
		return call(() -> client().findRoleByUuid(uuid));
	}

	protected void deleteRole(String uuid) {
		call(() -> client().deleteRole(uuid));
	}

	protected RoleResponse updateRole(String uuid, String newRoleName) {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName(newRoleName);
		return call(() -> client().updateRole(uuid, request));
	}

	// Tag
	protected TagResponse createTag(String projectName, String tagFamilyUuid, String tagName) {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setName(tagName);
		return call(() -> client().createTag(projectName, tagFamilyUuid, tagCreateRequest));
	}

	protected TagResponse readTag(String projectName, String tagFamilyUuid, String uuid) {
		return call(() -> client().findTagByUuid(projectName, tagFamilyUuid, uuid));
	}

	protected TagResponse updateTag(String projectName, String tagFamilyUuid, String uuid, String newTagName) {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setName(newTagName);
		return call(() -> client().updateTag(projectName, tagFamilyUuid, uuid, tagUpdateRequest));
	}

	// Node
	protected NodeResponse createNode(String projectName, String nameField) {
		NodeCreateRequest request = new NodeCreateRequest();
		return call(() -> client().createNode(projectName, request));
	}

	protected MeshRequest<NodeResponse> createNodeAsync(String fieldKey, Field field) {
		Node parentNode = folder("2015");
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setParentNode(new NodeReference().setUuid(parentNode.getUuid()));
		nodeCreateRequest.setSchema(new SchemaReference().setName("folder"));
		nodeCreateRequest.setLanguage("en");
		if (fieldKey != null) {
			nodeCreateRequest.getFields().put(fieldKey, field);
		}
		return client().createNode(PROJECT_NAME, nodeCreateRequest, new NodeParameters().setLanguages("en"));
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
		return call(() -> client().findNodeByUuid(projectName, uuid, new VersioningParameters().draft()));
	}

	protected void deleteNode(String projectName, String uuid) {
		call(() -> client().deleteNode(projectName, uuid));
	}

	protected NodeResponse updateNode(String projectName, String uuid, String nameFieldValue) {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		return call(() -> client().updateNode(projectName, uuid, nodeUpdateRequest));
	}

	protected TagFamilyResponse createTagFamily(String projectName, String tagFamilyName) {
		TagFamilyCreateRequest tagFamilyCreateRequest = new TagFamilyCreateRequest();
		tagFamilyCreateRequest.setName(tagFamilyName);
		return call(() -> client().createTagFamily(projectName, tagFamilyCreateRequest));
	}

	protected TagFamilyResponse readTagFamily(String projectName, String uuid) {
		return call(() -> client().findTagFamilyByUuid(projectName, uuid));
	}

	protected TagFamilyResponse updateTagFamily(String projectName, String uuid, String newTagFamilyName) {
		TagFamilyUpdateRequest tagFamilyUpdateRequest = new TagFamilyUpdateRequest();
		tagFamilyUpdateRequest.setName(newTagFamilyName);
		return call(() -> client().updateTagFamily(projectName, uuid, tagFamilyUpdateRequest));
	}

	protected void deleteTagFamily(String projectName, String uuid) {
		call(() -> client().deleteTagFamily(projectName, uuid));
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
				() -> client().findNodeByUuid(projectName, uuid, new VersioningParameters().setRelease(sourceReleaseName).draft()));

		Schema schema = schemaContainer(nodeResponse.getSchema().getName()).getLatestVersion().getSchema();

		// update node for target release
		NodeUpdateRequest update = new NodeUpdateRequest();
		update.setLanguage(nodeResponse.getLanguage());

		nodeResponse.getFields().keySet().forEach(key -> update.getFields().put(key, nodeResponse.getFields().getField(key, schema.getField(key))));
		return call(() -> client().updateNode(projectName, uuid, update, new VersioningParameters().setRelease(targetReleaseName)));
	}

	// Project
	protected ProjectResponse createProject(String projectName) {
		ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
		projectCreateRequest.setName(projectName);
		projectCreateRequest.setSchema(new SchemaReference().setName("folder"));
		return call(() -> client().createProject(projectCreateRequest));
	}

	protected ProjectResponse readProject(String uuid) {
		return call(() -> client().findProjectByUuid(uuid));
	}

	protected ProjectResponse updateProject(String uuid, String projectName) {
		ProjectUpdateRequest projectUpdateRequest = new ProjectUpdateRequest();
		projectUpdateRequest.setName(projectName);
		return call(() -> client().updateProject(uuid, projectUpdateRequest));
	}

	protected void deleteProject(String uuid) {
		call(() -> client().deleteProject(uuid));
	}

	protected SchemaResponse createSchema(String schemaName) {
		SchemaCreateRequest schema = FieldUtil.createSchemaCreateRequest();
		schema.setName(schemaName);
		return call(() -> client().createSchema(schema));
	}

	protected Schema readSchema(String uuid) {
		return call(() -> client().findSchemaByUuid(uuid));
	}

	protected GenericMessageResponse updateSchema(String uuid, String schemaName, SchemaUpdateParameters... updateParameters) {
		SchemaUpdateRequest schema = new SchemaUpdateRequest();
		schema.setName(schemaName);
		return call(() -> client().updateSchema(uuid, schema, updateParameters));
	}

	protected void deleteSchema(String uuid) {
		call(() -> client().deleteSchema(uuid));
	}

	protected MicroschemaResponse createMicroschema(String microschemaName) {
		MicroschemaCreateRequest microschema = new MicroschemaCreateRequest();
		microschema.setName(microschemaName);
		return call(() -> client().createMicroschema(microschema));
	}

	protected GenericMessageResponse updateMicroschema(String uuid, String microschemaName, SchemaUpdateParameters... parameters) {
		MicroschemaUpdateRequest microschema = FieldUtil.createMinimalValidMicroschemaUpdateRequest();
		microschema.setName(microschemaName);
		return call(() -> client().updateMicroschema(uuid, microschema, parameters));
	}

	public void assertEqualsSanitizedJson(String msg, String expectedJson, String unsanitizedResponseJson) {
		String sanitizedJson = unsanitizedResponseJson.replaceAll("uuid\":\"[^\"]*\"", "uuid\":\"uuid-value\"");
		assertEquals(msg, expectedJson, sanitizedJson);
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
		MeshInternal.get().serverSchemaStorage().clear();
		// node.getSchemaContainer().setSchema(schema);
	}

	protected MeshRequest<NodeResponse> uploadRandomData(Node node, String languageTag, String fieldKey, int binaryLen, String contentType,
			String fileName) {

		VersionNumber version = node.getGraphFieldContainer("en").getVersion();

		// role().grantPermissions(node, UPDATE_PERM);
		Buffer buffer = TestUtils.randomBuffer(binaryLen);
		return client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), languageTag, version.toString(), fieldKey, buffer, fileName, contentType, new NodeParameters().setResolveLinks(LinkType.FULL));
	}

	protected NodeResponse uploadImage(Node node, String languageTag, String fieldName) throws IOException {
		String contentType = "image/jpeg";
		String fileName = "blume.jpg";
		prepareSchema(node, "image/.*", fieldName);

		InputStream ins = getClass().getResourceAsStream("/pictures/blume.jpg");
		byte[] bytes = IOUtils.toByteArray(ins);
		Buffer buffer = Buffer.buffer(bytes);
		VersionNumber version = node.getGraphFieldContainer(languageTag).getVersion();

		return call(() -> client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), languageTag, version.toString(), fieldName, buffer, fileName,
				contentType));
	}

}
