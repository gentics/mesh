package com.gentics.mesh.plugin;

import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.plugin.manager.MeshPluginManager;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.twelvemonkeys.io.FileUtil;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import okhttp3.HttpUrl;
import okhttp3.Request;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class PluginManagerTest extends AbstractMeshTest {

	private static final String NAME = "basic";

	private static final String BASIC_PLUGIN_PATH = "target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar";

	public static final String PLUGIN_DIR = "target/plugins" + System.currentTimeMillis();

	@Before
	public void clearDeployments() throws IOException {
		File pluginsFolder = new File("plugins");
		if (pluginsFolder.exists()) {
			FileUtils.forceDelete(pluginsFolder);
		}

		MeshPluginManager manager = pluginManager();

		// Copy the uuids to avoid concurrency issues
		Set<String> uuids = new HashSet<>(manager.getPluginsMap().keySet());
		for (String uuid : uuids) {
			manager.undeploy(uuid).blockingAwait();
		}
	}

	@AfterClass
	public static void cleanup() throws IOException {
		File dir = new File(PLUGIN_DIR);
		if (dir.exists()) {
			FileUtils.forceDelete(dir);
		}
	}

	@Test
	public void testStop() {
		MeshPluginManager manager = pluginManager();
		int before = manager.getPlugins().size();
		for (int i = 0; i < 100; i++) {
			manager.deploy(ClonePlugin.class, "clone" + i).blockingGet();
		}
		assertEquals(before + 100, manager.getPlugins().size());

		assertEquals(100, manager.getPlugins().size());
		manager.stop().blockingAwait();
		assertEquals(0, manager.getPlugins().size());
		assertEquals("Not all deployed verticles have been undeployed.", before, manager.getPlugins().size());
	}

	@Test
	public void testFilesystemDeployment() throws Exception {
		setPluginBaseDir("abc");
		pluginManager().deploy(new File(BASIC_PLUGIN_PATH).getAbsolutePath()).blockingGet();

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
		setPluginBaseDir(PLUGIN_DIR);
		FileUtil.copy(new File(BASIC_PLUGIN_PATH), new File(PLUGIN_DIR, "plugin.jar"));
		FileUtil.copy(new File(BASIC_PLUGIN_PATH), new File(PLUGIN_DIR, "duplicate-plugin.jar"));
		FileUtil.copy(new File(BASIC_PLUGIN_PATH), new File(PLUGIN_DIR, "plugin.blub"));

		assertEquals(0, manager.getPlugins().size());
		manager.deployExistingPluginFiles().blockingAwait();
		manager.deployExistingPluginFiles().blockingAwait();
		manager.deployExistingPluginFiles().blockingAwait();
		assertEquals("Only one instance should have been deployed because all others are copies.", 1, manager.getPlugins().size());

		manager.stop().blockingAwait();
		assertEquals(0, manager.getPlugins().size());

		manager.deployExistingPluginFiles().blockingAwait();
		assertEquals(1, manager.getPlugins().size());
	}

	/**
	 * Test whether plugin requests are authenticated via the regular authentication chain of the Gentics Mesh server.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testPluginAuth() throws IOException {
		MeshPluginManager manager = pluginManager();
		manager.deploy(ClientPlugin.class, "client").blockingGet();
		JsonObject json = new JsonObject(getJSONViaClient(CURRENT_API_BASE_PATH + "/plugins/client/user"));
		assertNotNull(json.getString("uuid"));
	}

	/**
	 * Test whether the user and admin client are working as expected.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testClientAPI() throws IOException {
		PluginWrapper wrapper = mock(PluginWrapper.class);
		ClientPlugin plugin = new ClientPlugin(wrapper, pluginEnv());
		pluginManager().deploy(plugin).blockingGet();

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
		manager.deploy(DummyPlugin.class, "dummy").blockingGet();
		assertEquals(1, manager.getPlugins().size());

		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName("test");
		request.setSchemaRef("folder");
		call(() -> client().createProject(request));

		String apiName = DummyPlugin.API_NAME;
		PluginManifest manifest = JsonUtil.readValue(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + apiName + "/manifest"), PluginManifest.class);
		assertEquals("Johannes Schüth", manifest.getAuthor());

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + apiName + "/hello"));
		assertEquals("project", httpGetNow(CURRENT_API_BASE_PATH + "/test/plugins/" + apiName + "/hello"));
	}

	@Test
	public void testRedeployAfterInitFailure() {
		MeshPluginManager manager = pluginManager();
		try {
			manager.deploy(FailingPlugin.class, "failing").blockingGet();
			fail("Deployment should have failed");
		} catch (Exception e) {
			e.printStackTrace();
		}
		assertEquals(0, manager.getPlugins().size());

		manager.deploy(new SucceedingPlugin(null, null)).blockingGet();
		assertEquals(1, manager.getPlugins().size());
	}

	private void setPluginBaseDir(String baseDir) {
		File pluginDir = new File(baseDir);
		pluginDir.mkdirs();
		MeshOptions options = new MeshOptions();
		options.setPluginDirectory(baseDir);
		pluginManager().init(options);
	}
}
