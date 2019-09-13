package com.gentics.mesh.plugin.mustread;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.docker.MeshDockerServer;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class MustReadPluginIT extends AbstractPluginTest {

	static {
		// Use slf4j instead of JUL
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	@ClassRule
	public static final MeshDockerServer meshServer = new MeshDockerServer("gentics/mesh:0.40.2", Vertx.vertx())
		// .withFileSystemBind("target/mesh-mustread-plugin-1.0.0.jar", "/plugins/mustread.jar")
		.withFileSystemBind("docker", "/plugins")
		.waitForStartup();

	@Test
	public void testIt() throws IOException, InterruptedException {
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("Test");
		request.setSchemaRef("folder");
		ProjectResponse projectResponse = meshServer.client().createProject(request).blockingGet();

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("folder");
		nodeCreateRequest.setParentNodeUuid(projectResponse.getRootNode().getUuid());
		nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("testnode"));
		String nodeUuid = "2c572ab6f65e4f42972ab6f65ebf4245";
		NodeResponse nodeResponse = meshServer.client().createNode(nodeUuid, "Test", nodeCreateRequest).blockingGet();

		Thread.sleep(2000);
		JsonObject response2 = getResponseJsonGetRequest("/api/v2/admin/plugins");
		System.out.println(response2.encodePrettily());
		JsonObject response = getResponseJsonGetRequest("/api/v2/Test/plugins/" + API_NAME + "/unread");
		System.out.println(response.encodePrettily());

		JsonObject testData = new JsonObject();
		testData.put("uuids", new JsonArray().add(nodeUuid));
		assertEquals(200, getResponseCodePostRequest("/api/v2/Test/plugins/" + API_NAME + "/read", testData));

		JsonObject response3 = getResponseJsonGetRequest("/api/v2/Test/plugins/" + API_NAME + "/unread");
		System.out.println(response3.encodePrettily());
	}

	@Override
	protected MeshRestClient meshClient() {
		return meshServer.client();
	}

	@Override
	protected String serverURL() {
		return "http://" + meshServer.getHostname() + ":" + meshServer.getPort();
	}
}
