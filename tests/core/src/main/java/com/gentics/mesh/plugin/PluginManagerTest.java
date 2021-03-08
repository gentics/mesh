package com.gentics.mesh.plugin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.FAILED;
import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.plugin.PluginStatus;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.twelvemonkeys.io.FileUtil;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.Request;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class PluginManagerTest extends AbstractPluginTest {

	private static final String NAME = "basic";

	@Test
	public void testStop() {
		MeshPluginManager manager = pluginManager();
		int before = manager.getPluginIds().size();
		for (int i = 0; i < 100; i++) {
			manager.deploy(ClonePlugin.class, "clone" + i).blockingAwait();
		}
		assertEquals(before + 100, manager.getPluginIds().size());

		assertEquals(100, manager.getPluginIds().size());
		manager.stop().blockingAwait();
		manager.unloadPlugins();
		assertEquals(0, manager.getPluginIds().size());
		assertEquals("Not all deployed verticles have been undeployed.", before, manager.getPluginIds().size());
	}

	@Test
	public void testFilesystemDeployment() throws Exception {
		setPluginBaseDir("abc");
		String id = pluginManager().deploy(Paths.get(BASIC_PATH)).blockingGet();
		assertEquals("basic", id);
		waitForPluginRegistration();

		for (int i = 0; i < 2; i++) {
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName("test" + i);
			request.setSchemaRef("folder");
			call(() -> client().createProject(request));
		}

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + NAME + "/hello"));
		assertEquals("world-project", httpGetNow(CURRENT_API_BASE_PATH + "/test0/plugins/" + NAME + "/hello"));
		assertEquals("world-project", httpGetNow(CURRENT_API_BASE_PATH + "/test1/plugins/" + NAME + "/hello"));
	}

	@Test
	public void testStartupDeployment() throws IOException {
		MeshPluginManager manager = pluginManager();
		FileUtil.copy(new File(BASIC_PATH), new File(pluginDir(), "plugin.jar"));
		FileUtil.copy(new File(BASIC_PATH), new File(pluginDir(), "duplicate-plugin.jar"));
		FileUtil.copy(new File(BASIC_PATH), new File(pluginDir(), "plugin.blub"));
		FileUtil.copy(new File(GRAPHQL_PATH), new File(pluginDir(), "gql-plugin.jar"));
		FileUtil.copy(new File(GRAPHQL_PATH), new File(pluginDir(), "gql-plugin2.jar"));

		assertEquals(0, manager.getPluginIds().size());
		manager.deployExistingPluginFiles().blockingAwait();
		assertEquals(2, manager.getPluginIds().size());

		manager.deployExistingPluginFiles().blockingAwait();
		manager.deployExistingPluginFiles().blockingAwait();
		manager.deployExistingPluginFiles().blockingAwait();
		assertEquals("Only two plugins should have been deployed because all others are copies.", 2, manager.getPluginIds().size());

		manager.stop().blockingAwait();
		assertEquals(0, manager.getPluginIds().size());

		manager.start();
		manager.deployExistingPluginFiles().blockingAwait();
		assertEquals(2, manager.getPluginIds().size());
		waitForPluginRegistration();

		// Assert that plugins were registered and initialized
		String queryName = "plugin/graphql-plugin-query";
		GraphQLResponse response = call(
			() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		System.out.println(response.toJson());
		assertThat(new JsonObject(response.toJson())).compliesToAssertions(queryName);

	}

	/**
	 * Test whether plugin requests are authenticated via the regular authentication chain of the Gentics Mesh server.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPluginAuth() throws IOException {
		MeshPluginManager manager = pluginManager();
		manager.deploy(ClientPlugin.class, "client").blockingAwait();
		waitForPluginRegistration();
		JsonObject json = new JsonObject(getJSONViaClient(CURRENT_API_BASE_PATH + "/plugins/client/user"));
		assertNotNull(json.getString("uuid"));
	}

	@Test
	public void testClientFlooding() throws GenericRestException, IOException {
		pluginManager().deploy(ClientPlugin.class, "client").blockingAwait();
		waitForPluginRegistration();

		int before = threadCount();
		for (int i = 0; i < 200; i++) {
			UserResponse anonymous = JsonUtil.readValue(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/client/me"), UserResponse.class);
			assertEquals("The plugin should return the anonymous user since no api key was passed along", "anonymous", anonymous.getUsername());
		}
		int after = threadCount();

		int diff = after - before;
		assertThat(diff).isLessThan(100);
	}

	/**
	 * Test whether the user and admin client are working as expected.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testClientAPI() throws IOException {
		pluginManager().deploy(ClientPlugin.class, "client").blockingAwait();
		waitForPluginRegistration();

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("testabc");
		request.setSchemaRef("folder");
		call(() -> client().createProject(request));

		UserResponse anonymous = JsonUtil.readValue(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/client/me"), UserResponse.class);
		assertEquals("The plugin should return the anonymous user since no api key was passed along", "anonymous", anonymous.getUsername());

		UserResponse user = getViaClient(UserResponse.class, CURRENT_API_BASE_PATH + "/plugins/client/me");
		assertEquals("The plugin should return the authenticated response", "joe1", user.getUsername());

		UserResponse admin = getViaClient(UserResponse.class, CURRENT_API_BASE_PATH + "/plugins/client/admin");
		assertEquals("The admin endpoint should return the response which was authenticated using the admin user", "admin", admin.getUsername());

		ProjectResponse project = getViaClient(ProjectResponse.class, CURRENT_API_BASE_PATH + "/testabc/plugins/client/project");
		assertEquals("testabc", project.getName());
	}

	private String getJSONViaClient(String path) throws IOException {
		HttpUrl url = prepareUrl(path);

		Request.Builder b = new Request.Builder();
		b.url(url);
		b.method("GET", null);
		b.addHeader(HttpHeaders.AUTHORIZATION.toString(), "Bearer " + client().getAuthentication().getToken());

		String json = httpClient().newCall(b.build()).execute().body().string();
		return json;
	}

	private <T extends RestModel> T getViaClient(Class<T> clazz, String path) throws IOException {
		String json = getJSONViaClient(path);
		return JsonUtil.readValue(json, clazz);
	}

	@Test
	public void testJavaDeployment() throws IOException {
		MeshPluginManager manager = pluginManager();
		manager.deploy(DummyPlugin.class, "dummy").blockingAwait();
		assertEquals(1, manager.getPluginIds().size());
		waitForPluginRegistration();

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("test");
		request.setSchemaRef("folder");
		call(() -> client().createProject(request));

		String apiName = DummyPlugin.API_NAME;
		PluginManifest manifest = JsonUtil.readValue(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + apiName + "/manifest"), PluginManifest.class);
		assertEquals("Unknown Author", manifest.getAuthor());
		String pluginApiName = ((RestPlugin) manager.getPlugin("dummy").getPlugin()).restApiName();
		assertEquals(apiName, pluginApiName);

		JsonObject idInfo = new JsonObject(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + apiName + "/id"));
		String id = idInfo.getString("id");
		assertEquals("Invalid id", "dummy", id);

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + apiName + "/hello"));
		assertEquals("project", httpGetNow(CURRENT_API_BASE_PATH + "/test/plugins/" + apiName + "/hello"));
	}

	@Test
	public void testDeployAfterShutdownFailure() {
		MeshPluginManager manager = pluginManager();
		manager.deploy(FailingShutdownPlugin.class, "failing").blockingAwait();
		manager.undeploy("failing").blockingAwait(2, TimeUnit.SECONDS);
		assertEquals(0, manager.getPluginIds().size());

		manager.deploy(SucceedingPlugin.class, "succeeding").blockingAwait();
		assertEquals(1, manager.getPluginIds().size());
	}

	@Test
	public void testInitializeTimeoutPlugin() {
		options().setPluginTimeout(1);
		MeshPluginManager manager = pluginManager();
		manager.deploy(InitializeTimeoutPlugin.class, "timeout").blockingAwait();
		waitForEvent(MeshEvent.PLUGIN_DEPLOY_FAILED);
		assertEquals(1, manager.getPluginIds().size());
		PluginStatus status = manager.getStatus(manager.getPluginIds().iterator().next());
		assertEquals(FAILED, status);
	}

	@Test
	public void testRedeployAfterInitFailure() {
		MeshPluginManager manager = pluginManager();
		manager.deploy(FailingInitializePlugin.class, "failing").blockingAwait();
		waitForPluginRegistration();
		PluginStatus status = manager.getStatus(manager.getPluginIds().iterator().next());
		assertEquals(FAILED, status);
		assertEquals(1, manager.getPluginIds().size());
		assertEquals(1, manager.getPluginsMap().size());

		manager.deploy(SucceedingPlugin.class, "succeeding").blockingAwait();
		assertEquals(2, manager.getPluginIds().size());
		assertEquals(2, manager.getPluginsMap().size());
	}

}
