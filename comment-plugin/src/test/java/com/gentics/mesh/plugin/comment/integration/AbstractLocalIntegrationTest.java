package com.gentics.mesh.plugin.comment.integration;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ADMIN;
import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ANONYMOUS;
import static com.gentics.mesh.plugin.comment.TestCommentClient.User.EDITOR;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.plugin.comment.TestCommentClient;
import com.gentics.mesh.plugin.comment.TestCommentPlugin;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import okhttp3.Response;

public abstract class AbstractLocalIntegrationTest extends AbstractPluginLocalIntegrationTest {

	@ClassRule
	public static final MeshLocalServer server = new MeshLocalServer()
		.withNodeName("comment-plugin")
		.withInMemoryMode()
		.waitForStartup();

	protected static final String PROJECT_NAME = "test";
	protected static TestCommentClient client;
	protected static ProjectResponse project;

	protected static NodeResponse commentFolder;
	protected static NodeResponse publicNode;
	protected NodeResponse playground;

	protected static UserResponse anonymousUser;
	protected static UserResponse editorUser;
	protected static UserResponse adminUser;

	protected static void initMesh(String testConfig) throws InterruptedException {
		initMesh(testConfig, null);
	}

	protected static void initMesh(String testConfig, String nodeConfigJson) throws InterruptedException {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(PROJECT_NAME);
		request.setSchemaRef("folder");
		project = call(() -> server.client().createProject(request));

		updateFolderSchema();

		commentFolder = createFolder("comments");

		UserCreateRequest userCreateRequest = new UserCreateRequest()
			.setUsername("editor")
			.setPassword("test")
			.setEmailAddress("test@example.com");

		editorUser = call(() -> server.client().createUser(userCreateRequest));
		adminUser = call(() -> server.client().me());
		anonymousUser = getUser("anonymous");

		HashMap<TestCommentClient.User, String> tokens = new HashMap<>(TestCommentClient.User.values().length - 1);

		tokens.put(EDITOR, call(() -> server.client().issueAPIToken(editorUser.getUuid())).getToken());
		tokens.put(ADMIN, call(() -> server.client().issueAPIToken(adminUser.getUuid())).getToken());

		setEditorPermissions(project, editorUser);

		server.client().setAPIKey(tokens.get(EDITOR));
		publicNode = createFolder("someNode", nodeConfigJson);
		server.client().setAPIKey(tokens.get(ADMIN));
		grantPermission(publicNode, getRole("anonymous"), Permission.READ, Permission.READ_PUBLISHED);
		client = new TestCommentClient(
			server,
			project.getName(),
			publicNode,
			tokens);

		TestCommentPlugin.config = testConfig;
		server.getMesh().deployPlugin(TestCommentPlugin.class, "comment").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "comment");
	}

	private static UserResponse getUser(String userName) {
		return server.client().findUsers().toSingle()
			.flatMapObservable(response -> Observable.fromIterable(response.getData()))
			.filter(user -> user.getUsername().equals(userName))
			.singleOrError()
			.blockingGet();
	}

	private static void updateFolderSchema() {
		server.client().findSchemas(project.getName()).toSingle()
			.flatMapObservable(schemas -> Observable.fromIterable(schemas.getData()))
			.filter(schema -> schema.getName().equals("folder"))
			.singleOrError()
			.flatMapCompletable(AbstractLocalIntegrationTest::doFolderUpdate)
			.blockingAwait();
	}

	private static Completable doFolderUpdate(SchemaResponse schemaResponse) {
		SchemaUpdateRequest request = new SchemaUpdateRequest();
		request.setFields(schemaResponse.getFields());
		request.getFields().add(new StringFieldSchemaImpl().setName("commentConfig"));
		request.setName(schemaResponse.getName());
		request.setSegmentField(schemaResponse.getSegmentField());
		return server.client().updateSchema(schemaResponse.getUuid(), request).toCompletable();
	}

	private static RoleResponse getRole(String roleName) {
		return server.client().findRoles().toSingle()
			.flatMapObservable(response -> Observable.fromIterable(response.getData()))
			.filter(role -> role.getName().equals(roleName))
			.singleOrError()
			.blockingGet();
	}

	private static void grantPermission(NodeResponse node, RoleResponse role, Permission... permissions) {
		RolePermissionRequest req = new RolePermissionRequest();
		req.setRecursive(true);
		for (Permission p : permissions) {
			req.getPermissions().add(p);
		}
		req.getPermissions().setOthers(false);
		server.client().updateRolePermissions(role.getUuid(), project.getName() + "/nodes/" + node.getUuid(), req).toCompletable().blockingAwait();
	}

	private static NodeResponse createFolder(String name) {
		return createFolder(name, null);
	}

	private static NodeResponse createFolder(String name, String nodeConfigJson) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setLanguage("en")
			.setParentNodeUuid(project.getRootNode().getUuid())
			.setSchemaName("folder");
		request.getFields().put("name", new StringFieldImpl().setString(name));
		request.getFields().put("slug", new StringFieldImpl().setString(name));
		if (nodeConfigJson != null) {
			request.getFields().put("commentConfig", new StringFieldImpl().setString(nodeConfigJson));
		}
		return call(() -> server.client().createNode(project.getName(), request));
	}

	protected NodeResponse createFolder(NodeResponse parent, String name) {
		NodeCreateRequest request = new NodeCreateRequest()
			.setLanguage("en")
			.setParentNodeUuid(parent.getUuid())
			.setSchemaName("folder");
		request.getFields().put("name", new StringFieldImpl().setString(name));
		request.getFields().put("slug", new StringFieldImpl().setString(name));
		return call(() -> server.client().createNode(project.getName(), request));
	}

	private static void setEditorPermissions(ProjectResponse project, UserResponse user) {
		RoleResponse role = createRole();
		createAndAssignGroup(role, user);

		RolePermissionRequest allPermissions = new RolePermissionRequest();
		allPermissions.setRecursive(true);
		allPermissions.getPermissions().setOthers(true);

		Function<String, Completable> grantAll = path -> server.client().updateRolePermissions(role.getUuid(), path, allPermissions).toCompletable();
		Consumer<String> grantAllNow = path -> grantAll.apply(path).blockingAwait();

		server.client().findSchemas(project.getName()).toSingle()
			.flatMapObservable(schemas -> Observable.fromIterable(schemas.getData()))
			.flatMapCompletable(schema -> grantAll.apply("/schemas/" + schema.getUuid()))
			.blockingAwait();

		grantAllNow.accept(project.getName() + "/nodes");
	}

	private static RoleResponse createRole() {
		return call(() -> server.client().createRole(new RoleCreateRequest()
			.setName("editor")));
	}

	private static GroupResponse createAndAssignGroup(RoleResponse role, UserResponse user) {
		GroupResponse group = call(() -> server.client().createGroup(new GroupCreateRequest()
			.setName("editor")));

		call(() -> server.client().addRoleToGroup(group.getUuid(), role.getUuid()));
		call(() -> server.client().addUserToGroup(group.getUuid(), user.getUuid()));

		return group;
	}

	@Before
	public void setUp() {
		client.setUser(ANONYMOUS);
		playground = createFolder("playground");
	}

	@After
	public void cleanUp() {
		// Delete all comments and sections
		server.client().findNodeChildren(project.getName(), commentFolder.getUuid(),
			new PagingParametersImpl().setPerPage(999L)).toSingle()
			.flatMapObservable(nodes -> Observable.fromIterable(nodes.getData()))
			.flatMapCompletable(node -> server.client().deleteNode(
				project.getName(), node.getUuid(),
				new DeleteParametersImpl().setRecursive(true)).toCompletable())
			.blockingAwait();

		server.client().deleteNode(
			project.getName(),
			playground.getUuid(),
			new DeleteParametersImpl().setRecursive(true)).blockingAwait();
	}

	protected String stringResponse(Response response) {
		try {
			return response.body().string();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected JsonObject jsonResponse(Response response) {
		return new JsonObject(stringResponse(response));
	}

	protected Response assertStatusCode(int code, Response response) {
		assertEquals(code, response.code());
		return response;
	}

	protected NestedCommentSection commentsOfNodeResponse(Response response) {
		return JsonUtil.readValue(stringResponse(response), NestedCommentSection.class);
	}
}
