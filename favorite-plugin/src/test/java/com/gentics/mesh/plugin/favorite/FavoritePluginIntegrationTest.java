package com.gentics.mesh.plugin.favorite;

import static com.gentics.mesh.test.ClientHelper.call;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.client.DeleteParametersImpl;
import com.gentics.mesh.test.local.MeshLocalServer;

/**
 * Integration tests for the LikePlugin
 */
public class FavoritePluginIntegrationTest {
	private final static String PROJECT_NAME = "testproject";

	@ClassRule
	public static final MeshLocalServer server = new MeshLocalServer().withInMemoryMode().waitForStartup();

	private static ProjectResponse project;

	private static SchemaResponse schema;

	private static Map<String, String> userTokens = new HashMap<>();

	/**
	 * Get the project specific url for the plugin with the given path appended
	 * 
	 * @param path
	 *            path
	 * @return complete url
	 */
	private static String projectUrl(String path) {
		if (!path.startsWith("/")) {
			path = "/" + path;
		}
		return String.format("http://%s:%d/api/v1/%s/plugins/%s%s", server.getHostname(), server.getPort(), PROJECT_NAME, "favorite", path);
	}

	@BeforeClass
	public static void setupOnce() throws InterruptedException, ExecutionException, TimeoutException, GenericRestException, IOException {
		ProjectCreateRequest createProject = new ProjectCreateRequest().setName(PROJECT_NAME).setSchemaRef("folder");
		project = call(() -> server.client().createProject(createProject));

		SchemaCreateRequest request = JsonUtil.readValue(
			IOUtils.toString(FavoritePluginIntegrationTest.class.getResource("testcontent_schema.json"), "UTF-8"),
			SchemaCreateRequest.class);
		schema = call(() -> server.client().createSchema(request));
		call(() -> server.client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		server.getMesh().deployPlugin(FavoriteTestPlugin.class, "favorite").blockingAwait();

		for (int i = 1; i <= 100; i++) {
			UserCreateRequest createUser = new UserCreateRequest();
			createUser.setFirstname(String.format("User%dFirst", i));
			createUser.setLastname(String.format("User%dLast", i));
			createUser.setUsername(String.format("user%d", i));
			createUser.setPassword(String.format("user%d", i));
			UserResponse user = call(() -> server.client().createUser(createUser));
			UserAPITokenResponse token = call(() -> server.client().issueAPIToken(user.getUuid()));
			userTokens.put(user.getUuid(), token.getToken());
		}
	}

	/**
	 * Clean data before test run
	 */
	@Before
	public void setup() {
		// delete all nodes of the specified schemas
		List<String> schemasToDelete = Arrays.asList(SchemaService.SECTION_SCHEMA_NAME, schema.getName());
		NodeListResponse response = call(() -> server.client().findNodes(PROJECT_NAME));
		for (NodeResponse node : response.getData()) {
			if (schemasToDelete.contains(node.getSchema().getName())) {
				call(() -> server.client().deleteNode(PROJECT_NAME, node.getUuid(), new DeleteParametersImpl().setRecursive(true)));
			}
		}
	}

}
