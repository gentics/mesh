package com.gentics.mesh.test.context;

import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
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
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.LinkType;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.SchemaUpdateParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.search.DummySearchProvider;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.test.TestDataProvider;
import com.gentics.mesh.util.VersionNumber;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.test.core.TestUtils;

public interface TestHelperMethods {

	MeshTestContext getTestContext();

	// MeshRestClient client();
	//
	// int port();
	//
	// Tag tag(String key);
	//
	// Project project();
	//
	// Node folder(String key);
	//
	// Node content(String key);
	//
	// TagFamily tagFamily(String key);
	//
	// SchemaContainer schemaContainer(String key);
	//
	// Database db();
	//
	// TestDataProvider data();

	default public Database db() {
		return MeshInternal.get().database();
	}

	default public BootstrapInitializer boot() {
		return MeshInternal.get().boot();
	}

	default public TestDataProvider data() {
		return getTestContext().getData();
	}

	default public Role role() {
		return data().role();
	}

	default public MeshAuthUser getRequestUser() {
		return data().getUserInfo().getUser().reframe(MeshAuthUserImpl.class);
	}

	default public User user() {
		User user = data().user();
//		user.reload();
		return user;
	}

	default public MeshRoot meshRoot() {
		return data().getMeshRoot();
	}

	default public Group group() {
		Group group = data().getUserInfo().getGroup();
		group.reload();
		return group;
	}

	default public MeshRestClient client() {
		return getTestContext().getClient();
	}

	default public DummySearchProvider dummySearchProvider() {
		return getTestContext().getDummySearchProvider();
	}

	default public Project project() {
		return data().getProject();
	}

	default public int port() {
		return getTestContext().getPort();
	}

	default public Node folder(String key) {
		Node node = data().getFolder(key);
		node.reload();
		return node;
	}

	default public Node content(String key) {
		return data().getContent(key);
	}

	default public TagFamily tagFamily(String key) {
		TagFamily family = data().getTagFamily(key);
		family.reload();
		return family;
	}

	default public Tag tag(String key) {
		Tag tag = data().getTag(key);
		tag.reload();
		return tag;
	}

	default public SchemaContainer schemaContainer(String key) {
		SchemaContainer container = data().getSchemaContainer(key);
		container.reload();
		return container;
	}

	default public Map<String, SchemaContainer> schemaContainers() {
		return data().getSchemaContainers();
	}

	default public Map<String, Role> roles() {
		return data().getRoles();
	}

	default public Map<String, ? extends Tag> tags() {
		return data().getTags();
	}

	default public Language english() {
		Language language = data().getEnglish();
		language.reload();
		return language;
	}

	default public Language german() {
		Language language = data().getGerman();
		language.reload();
		return language;
	}

	default public Map<String, Group> groups() {
		return data().getGroups();
	}

	default public Map<String, MicroschemaContainer> microschemaContainers() {
		return data().getMicroschemaContainers();
	}

	default public MicroschemaContainer microschemaContainer(String key) {
		MicroschemaContainer container = data().getMicroschemaContainers().get(key);
		container.reload();
		return container;
	}

	/**
	 * Returns the news overview node which has no tags.
	 * 
	 * @return
	 */
	default public Node content() {
		Node content = data().getContent("news overview");
		content.reload();
		return content;
	}

	default public UserResponse readUser(String uuid) {
		return call(() -> client().findUserByUuid(uuid));
	}

	default public UserResponse updateUser(String uuid, String newUserName) {
		UserUpdateRequest userUpdateRequest = new UserUpdateRequest();
		userUpdateRequest.setUsername(newUserName);
		return call(() -> client().updateUser(uuid, userUpdateRequest));
	}

	default public void deleteUser(String uuid) {
		call(() -> client().deleteUser(uuid));
	}

	default public GroupResponse createGroup(String groupName) {
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(groupName);
		return call(() -> client().createGroup(request));
	}

	default public GroupResponse readGroup(String uuid) {
		return call(() -> client().findGroupByUuid(uuid));
	}

	default public GroupResponse updateGroup(String uuid, String newGroupName) {
		GroupUpdateRequest groupUpdateRequest = new GroupUpdateRequest();
		groupUpdateRequest.setName(newGroupName);
		return call(() -> client().updateGroup(uuid, groupUpdateRequest));
	}

	default public void deleteGroup(String uuid) {
		call(() -> client().deleteGroup(uuid));
	}

	default public RoleResponse createRole(String roleName, String groupUuid) {
		RoleCreateRequest roleCreateRequest = new RoleCreateRequest();
		roleCreateRequest.setName(roleName);
		return call(() -> client().createRole(roleCreateRequest));
	}

	default public RoleResponse readRole(String uuid) {
		return call(() -> client().findRoleByUuid(uuid));
	}

	default public void deleteRole(String uuid) {
		call(() -> client().deleteRole(uuid));
	}

	default public RoleResponse updateRole(String uuid, String newRoleName) {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName(newRoleName);
		return call(() -> client().updateRole(uuid, request));
	}

	default public TagResponse createTag(String projectName, String tagFamilyUuid, String tagName) {
		TagCreateRequest tagCreateRequest = new TagCreateRequest();
		tagCreateRequest.setName(tagName);
		return call(() -> client().createTag(projectName, tagFamilyUuid, tagCreateRequest));
	}

	default public TagResponse readTag(String projectName, String tagFamilyUuid, String uuid) {
		return call(() -> client().findTagByUuid(projectName, tagFamilyUuid, uuid));
	}

	default public TagResponse updateTag(String projectName, String tagFamilyUuid, String uuid, String newTagName) {
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		tagUpdateRequest.setName(newTagName);
		return call(() -> client().updateTag(projectName, tagFamilyUuid, uuid, tagUpdateRequest));
	}

	default public NodeResponse createNode(String projectName, String nameField) {
		NodeCreateRequest request = new NodeCreateRequest();
		return call(() -> client().createNode(projectName, request));
	}

	default public MeshRequest<NodeResponse> createNodeAsync(String fieldKey, Field field) {
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

	default public NodeResponse createNode(String fieldKey, Field field) {
		NodeResponse response = call(() -> createNodeAsync(fieldKey, field));
		assertNotNull("The response could not be found in the result of the future.", response);
		if (fieldKey != null) {
			assertNotNull("The field was not included in the response.", response.getFields().hasField(fieldKey));
		}
		return response;
	}

	default public NodeResponse readNode(String projectName, String uuid) {
		return call(() -> client().findNodeByUuid(projectName, uuid, new VersioningParameters().draft()));
	}

	default public void deleteNode(String projectName, String uuid) {
		call(() -> client().deleteNode(projectName, uuid));
	}

	default public NodeResponse updateNode(String projectName, String uuid, String nameFieldValue) {
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		return call(() -> client().updateNode(projectName, uuid, nodeUpdateRequest));
	}

	default public TagFamilyResponse createTagFamily(String projectName, String tagFamilyName) {
		TagFamilyCreateRequest tagFamilyCreateRequest = new TagFamilyCreateRequest();
		tagFamilyCreateRequest.setName(tagFamilyName);
		return call(() -> client().createTagFamily(projectName, tagFamilyCreateRequest));
	}

	default public TagFamilyResponse readTagFamily(String projectName, String uuid) {
		return call(() -> client().findTagFamilyByUuid(projectName, uuid));
	}

	default public TagFamilyResponse updateTagFamily(String projectName, String uuid, String newTagFamilyName) {
		TagFamilyUpdateRequest tagFamilyUpdateRequest = new TagFamilyUpdateRequest();
		tagFamilyUpdateRequest.setName(newTagFamilyName);
		return call(() -> client().updateTagFamily(projectName, uuid, tagFamilyUpdateRequest));
	}

	default public void deleteTagFamily(String projectName, String uuid) {
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
	default public NodeResponse migrateNode(String projectName, String uuid, String sourceReleaseName, String targetReleaseName) {
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

	default public ProjectResponse createProject(String projectName) {
		ProjectCreateRequest projectCreateRequest = new ProjectCreateRequest();
		projectCreateRequest.setName(projectName);
		projectCreateRequest.setSchema(new SchemaReference().setName("folder"));
		return call(() -> client().createProject(projectCreateRequest));
	}

	default public ProjectResponse readProject(String uuid) {
		return call(() -> client().findProjectByUuid(uuid));
	}

	default public ProjectResponse updateProject(String uuid, String projectName) {
		ProjectUpdateRequest projectUpdateRequest = new ProjectUpdateRequest();
		projectUpdateRequest.setName(projectName);
		return call(() -> client().updateProject(uuid, projectUpdateRequest));
	}

	default public void deleteProject(String uuid) {
		call(() -> client().deleteProject(uuid));
	}

	default public SchemaResponse createSchema(String schemaName) {
		SchemaCreateRequest schema = FieldUtil.createSchemaCreateRequest();
		schema.setName(schemaName);
		return call(() -> client().createSchema(schema));
	}

	default public Schema readSchema(String uuid) {
		return call(() -> client().findSchemaByUuid(uuid));
	}

	default public GenericMessageResponse updateSchema(String uuid, String schemaName, SchemaUpdateParameters... updateParameters) {
		SchemaUpdateRequest schema = new SchemaUpdateRequest();
		schema.setName(schemaName);
		return call(() -> client().updateSchema(uuid, schema, updateParameters));
	}

	default public void deleteSchema(String uuid) {
		call(() -> client().deleteSchema(uuid));
	}

	default public MicroschemaResponse createMicroschema(String microschemaName) {
		MicroschemaCreateRequest microschema = new MicroschemaCreateRequest();
		microschema.setName(microschemaName);
		return call(() -> client().createMicroschema(microschema));
	}

	default public GenericMessageResponse updateMicroschema(String uuid, String microschemaName, SchemaUpdateParameters... parameters) {
		MicroschemaUpdateRequest microschema = FieldUtil.createMinimalValidMicroschemaUpdateRequest();
		microschema.setName(microschemaName);
		return call(() -> client().updateMicroschema(uuid, microschema, parameters));
	}

	/**
	 * Prepare the schema of the given node by adding the binary content field to its schema fields. This method will also update the clientside schema storage.
	 * 
	 * @param node
	 * @param mimeTypeWhitelist
	 * @param binaryFieldName
	 * @throws IOException
	 */
	default public void prepareSchema(Node node, String mimeTypeWhitelist, String binaryFieldName) throws IOException {
		// Update the schema and enable binary support for folders
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		schema.addField(new BinaryFieldSchemaImpl().setAllowedMimeTypes(mimeTypeWhitelist).setName(binaryFieldName).setLabel("Binary content"));
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		MeshInternal.get().serverSchemaStorage().clear();
		// node.getSchemaContainer().setSchema(schema);
	}

	default public MeshRequest<NodeResponse> uploadRandomData(Node node, String languageTag, String fieldKey, int binaryLen, String contentType,
			String fileName) {

		VersionNumber version = node.getGraphFieldContainer("en").getVersion();

		// role().grantPermissions(node, UPDATE_PERM);
		Buffer buffer = TestUtils.randomBuffer(binaryLen);
		return client().updateNodeBinaryField(PROJECT_NAME, node.getUuid(), languageTag, version.toString(), fieldKey, buffer, fileName, contentType,
				new NodeParameters().setResolveLinks(LinkType.FULL));
	}

	default public NodeResponse uploadImage(Node node, String languageTag, String fieldName) throws IOException {
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

	default public UserResponse createUser(String username) {
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(username);
		request.setPassword("test1234");
		request.setGroupUuid(group().getUuid());

		return call(() -> client().createUser(request));
	}

	default MeshComponent meshDagger() {
		return MeshInternal.get();
	}

	default SearchProvider searchProvider() {
		return meshDagger().searchProvider();
	}

	default public int getNodeCount() {
		return data().getNodeCount();
	}

	default public HttpClient createHttpClient() {
		HttpClientOptions options = new HttpClientOptions();
		options.setDefaultHost("localhost");
		options.setDefaultPort(port());
		HttpClient client = Mesh.vertx().createHttpClient(options);
		return client;
	}

	default public SchemaContainer getSchemaContainer() {
		SchemaContainer container = data().getSchemaContainer("content");
		container.reload();
		return container;
	}

	default public Vertx vertx() {
		return getTestContext().getVertx();
	}

	default public SearchQueueBatch createBatch() {
		return MeshInternal.get().searchQueue().create();
	}

	default public Map<String, User> users() {
		return data().getUsers();
	}

}
