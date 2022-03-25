package com.gentics.mesh.plugin.comment.integration;

import static com.gentics.mesh.plugin.comment.TestCommentClient.User.ANONYMOUS;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;

import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.DeleteParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.plugin.comment.TestClient;
import com.gentics.mesh.plugin.comment.TestCommentClient;
import com.gentics.mesh.plugin.comment.TestCommentPlugin;
import com.gentics.mesh.plugin.comment.model.NestedCommentSection;
import com.gentics.mesh.plugin.test.common.AbstractPluginLocalIntegrationTest;
import com.gentics.mesh.plugin.test.common.OrientDBMeshLocalServer;
import com.gentics.mesh.plugin.test.common.util.PluginTestUtil;
import com.gentics.mesh.test.local.MeshLocalServer;

import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;
import okhttp3.Response;

public abstract class AbstractLocalIntegrationTest extends AbstractPluginLocalIntegrationTest {

	@ClassRule
	public static final MeshLocalServer server = new OrientDBMeshLocalServer()
		.withInMemoryMode()
		.withNodeName("comment-plugin")
		.waitForStartup();

	protected static TestCommentClient client;
	protected static ProjectResponse project;

	protected static NodeResponse commentFolder;
	protected static NodeResponse publicNode;
	protected static TestClient testClient;
	protected NodeResponse playground;

	protected static UserResponse anonymousUser;
	protected static UserResponse editorUser;
	protected static UserResponse adminUser;

	protected static void initMesh(String testConfig) throws InterruptedException {
		initMesh(testConfig, null);
	}

	protected static void initMesh(String testConfig, String nodeConfigJson) throws InterruptedException {
		testClient = new TestClient(server.client());
		TestDataSetup.TestData data = new TestDataSetup(server.client()).setup(nodeConfigJson);
		project = data.project;
		commentFolder = data.commentFolder;
		publicNode = data.publicNode;
		anonymousUser = data.anonymousUser;
		editorUser = data.editorUser;
		adminUser = data.adminUser;
		Map<TestCommentClient.User, String> tokens = data.tokens;

		client = new TestCommentClient(
			"http://" + server.getHostname() + ":" + server.getPort(),
			data
		);

		TestCommentPlugin.config = testConfig;
		server.getMesh().deployPlugin(TestCommentPlugin.class, "comment").blockingAwait();
		PluginTestUtil.waitForPluginRegistration(server, 5_000, "comment");
	}

	@Before
	public void setUp() {
		client.setUser(ANONYMOUS);
		playground = testClient.createFolder(project, "playground");
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
