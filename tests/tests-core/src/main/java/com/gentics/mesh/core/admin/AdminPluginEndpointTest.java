package com.gentics.mesh.core.admin;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.plugin.PluginStatus.REGISTERED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.plugin.AbstractPluginTest;
import com.gentics.mesh.plugin.ClonePlugin;
import com.gentics.mesh.plugin.ManifestInjectorPlugin;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.PluginTests;
import com.gentics.mesh.test.helper.ExpectedEvent;

/**
 * Tests for admin endpoint for plugins.
 * 
 * These tests require the test plugins to be build. You can build these plugins using the /core/build-test-plugins.sh script.
 */
@Category(PluginTests.class)
@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminPluginEndpointTest extends AbstractPluginTest {

	private static final String API_NAME = "basic";

	@Test
	public void testDeployPluginMissingPermission() throws IOException {
		revokeAdmin();
		copyAndDeploy(BASIC_PATH, "plugin.jar", FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReadPluginMissingPermission() throws IOException {
		grantAdmin();
		PluginResponse deployment = copyAndDeploy(BASIC_PATH, "plugin.jar");

		revokeAdmin();
		String id = deployment.getId();
		call(() -> client().findPlugin(id), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testNonPluginDeployment() throws IOException {
		grantAdmin();
		String name = "non-mesh.jar";

		int before = meshApi().pluginIds().size();
		copyAndDeploy(NON_MESH_PATH, name, INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);

		int after = meshApi().pluginIds().size();
		assertEquals("The verticle should not stay deployed.", before, after);
	}

	@Test
	public void testUndeployPluginMissingPermission() throws IOException {
		grantAdmin();
		PluginResponse deployment = copyAndDeploy(BASIC_PATH, "plugin.jar");

		revokeAdmin();
		String id = deployment.getId();
		call(() -> client().undeployPlugin(id), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testUndeployBogusPlugin() {
		grantAdmin();
		String id = "bogus";
		call(() -> client().undeployPlugin(id), NOT_FOUND, "object_not_found_for_uuid", id);
	}

	@Test
	public void testReadPluginListMissingPermission() {
		revokeAdmin();
		call(() -> client().findPlugins(), FORBIDDEN, "error_admin_permission_required");
	}

	/**
	 * Deploy the basic plugin and assert that the plugin routes can be reached.
	 * 
	 * @throws IOException
	 * @throws TimeoutException 
	 */
	@Test
	public void testDeployPlugin() throws IOException, TimeoutException {
		grantAdmin();

		PluginResponse deployment = null;
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			deployment = copyAndDeploy(BASIC_PATH, "basic-plugin.jar");
		}
		String deploymentId = deployment.getId();
		assertEquals("basic", deployment.getId());
		assertEquals(REGISTERED, deployment.getStatus());

		PluginResponse response1 = call(() -> client().findPlugin(deploymentId));
		assertEquals(REGISTERED, response1.getStatus());

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + API_NAME + "/hello"));
		assertEquals("world-project", httpGetNow(CURRENT_API_BASE_PATH + "/" + PROJECT_NAME + "/plugins/" + API_NAME + "/hello"));

		PluginListResponse list = call(() -> client().findPlugins());
		assertEquals(1, list.getMetainfo().getTotalCount());

		PluginResponse response2 = call(() -> client().findPlugin(deploymentId));
		assertEquals(deployment.getName(), response2.getName());

		call(() -> client().undeployPlugin(deploymentId));

		assertEquals(404, httpGet(CURRENT_API_BASE_PATH + "/plugins/" + API_NAME + "/hello").execute().code());

	}

	@Test
	public void testPluginList() throws IOException {
		grantAdmin();
		PluginResponse response = copyAndDeploy(BASIC_PATH, "plugin.jar");
		assertEquals(1, pluginManager().getPluginIds().size());

		String bogusName = "bogus.jar";

		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(bogusName)), BAD_REQUEST,
			"admin_plugin_error_plugin_deployment_failed", bogusName);

		PluginResponse response2 = call(() -> client().findPlugin(response.getId()));
		assertEquals(response.getId(), response2.getId());

		call(() -> client().findPlugin("bogus"), NOT_FOUND, "admin_plugin_error_plugin_not_found", "bogus");
		PluginListResponse pluginList = call(() -> client().findPlugins());
		assertNull(pluginList.getMetainfo().getPerPage());
		assertEquals(1, pluginList.getMetainfo().getCurrentPage());
		assertEquals(1, pluginList.getMetainfo().getPageCount());
		assertEquals(1, pluginList.getMetainfo().getTotalCount());
		PluginResponse first = pluginList.getData().get(0);
		assertEquals("The id of the plugin did not match", "basic", first.getId());

		pluginList = call(() -> client().findPlugins(new PagingParametersImpl().setPerPage(1L)));
		assertEquals(1, pluginList.getMetainfo().getPerPage().longValue());
		assertEquals(1, pluginList.getMetainfo().getCurrentPage());
		assertEquals(1, pluginList.getMetainfo().getPageCount());
		assertEquals(1, pluginList.getMetainfo().getTotalCount());

		GenericMessageResponse msg = call(() -> client().undeployPlugin(response.getId()));
		assertThat(msg).matches("admin_plugin_undeployed", response.getId());
		pluginList = call(() -> client().findPlugins(new PagingParametersImpl().setPerPage(1L)));
		assertEquals(1, pluginList.getMetainfo().getPerPage().longValue());
		assertEquals(1, pluginList.getMetainfo().getCurrentPage());
		assertEquals(0, pluginList.getMetainfo().getPageCount());
		assertEquals(0, pluginList.getMetainfo().getTotalCount());

	}

	@Test
	public void testStaticHandler() throws IOException, TimeoutException {
		grantAdmin();

		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(BASIC_PATH, "plugin.jar");
		}
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(BASIC2_PATH, "plugin2.jar");
		}

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic/hello"));
		assertEquals("world2", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic2/hello"));
		assertEquals("content", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic/static/file.txt"));
		assertEquals("content2", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic2/static2/file.txt"));
		assertThat(httpGet(CURRENT_API_BASE_PATH + "/plugins/basic/static/nonexistent.txt").execute().code())
				.as("Response code for nonexistent.txt").isEqualTo(404);
	}

	/**
	 * Test static file delivery, after the plugin was redeployed.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	@Test
	public void testStaticHandlerRedeploy() throws IOException, TimeoutException {
		grantAdmin();

		// deploy first version of plugin (containing changed.txt with old content and removed.txt)
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(STATIC_PATH, "plugin.jar");
		}
		assertEquals("before change", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/static/static/changed.txt"));
		assertEquals("removed", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/static/static/removed.txt"));
		assertThat(httpGet(CURRENT_API_BASE_PATH + "/plugins/static/static/added.txt").execute().code())
			.as("Response code for added.txt").isEqualTo(404);

		// undeploy plugin
		call(() -> client().undeployPlugin("static"));

		// deploy second version of plugin (containing changed.txt with new content and added.txt)
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(STATIC2_PATH, "plugin.jar");
		}
		assertEquals("after change", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/static/static/changed.txt"));
		assertThat(httpGet(CURRENT_API_BASE_PATH + "/plugins/static/static/removed.txt").execute().code())
			.as("Response code for removed.txt").isEqualTo(404);
		assertEquals("added", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/static/static/added.txt"));
	}

	/**
	 * Test static file delivery, when the plugin directory is configured with an absolute path.
	 * @throws IOException
	 * @throws TimeoutException
	 */
	@Test
	public void testStaticHandlerAbsolutePluginDir() throws IOException, TimeoutException {
		grantAdmin();

		// make the configured plugin dir absolute
		File pluginDir = new File(pluginDir());
		setPluginBaseDir(pluginDir.getAbsolutePath());

		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(STATIC_PATH, "plugin.jar");
		}
		assertEquals("before change", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/static/static/changed.txt"));
	}

	@Test
	public void testClassLoaderHandling() throws IOException, TimeoutException {
		grantAdmin();

		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(CLASSLOADER_PATH, "plugin.jar");
		}

		assertEquals("plugin", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/classloader/scope"));
		assertEquals("plugin", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/classloader/check"));
	}

	@Test
	public void testInvalidManifest() throws IOException {
		ManifestInjectorPlugin.manifest = new PluginManifest()
			.setAuthor("Joe Doe")
			.setId("injector")
			.setName("The injector test plugin")
			.setInception("2018")
			.setDescription("some Text")
			.setLicense("Apache 2.0")
			.setVersion(null);
		ManifestInjectorPlugin.apiName = "api";

		grantAdmin();
		deployPlugin(ManifestInjectorPlugin.class, "inject", BAD_REQUEST, "admin_plugin_error_validation_failed_field_missing", "version");
	}

	@Test
	public void testManifestWithInvalidAPIName() {
		ManifestInjectorPlugin.manifest = new PluginManifest()
			.setId("injector")
			.setName("Injector test plugin")
			.setDescription("some Text")
			.setAuthor("Joe Doe")
			.setInception("2018")
			.setLicense("Apache 2.0")
			.setVersion("1.0");
		ManifestInjectorPlugin.apiName = "api with spaces";

		grantAdmin();

		deployPlugin(ManifestInjectorPlugin.class, "injector", BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", "injector");

		ManifestInjectorPlugin.apiName = "api/with/slashes";
		deployPlugin(ManifestInjectorPlugin.class, "injector", BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", "injector");

		ManifestInjectorPlugin.apiName = "ok";
		deployPlugin(ManifestInjectorPlugin.class, "injector");
	}

	@Test
	public void testMultipleDeployments() throws IOException, TimeoutException {
		grantAdmin();

		for (int i = 1; i <= 100; i++) {
			try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
				deployPlugin(ClonePlugin.class, "clone" + i);
			}
		}

		PluginListResponse result = call(() -> client().findPlugins(new PagingParametersImpl().setPerPage(10L).setPage(10)));
		PluginResponse lastElement = result.getData().get(9);

		assertEquals("Clone Plugin 100", lastElement.getName());
		assertEquals(10, result.getMetainfo().getPerPage().longValue());
		assertEquals(10, result.getMetainfo().getCurrentPage());
		assertEquals(10, result.getMetainfo().getPageCount());
		assertEquals(100, result.getMetainfo().getTotalCount());

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/clone100/hello"));

	}

	@Test
	public void testDuplicateDeployment() throws IOException {
		grantAdmin();

		copyAndDeploy(BASIC_PATH, "plugin.jar");
		assertEquals(1, pluginManager().getPluginIds().size());

		long before = pluginCount();
		copyAndDeploy(BASIC_PATH, "plugin2.jar", BAD_REQUEST, "admin_plugin_error_plugin_with_id_already_deployed", "plugin2.jar");
		assertEquals("No additional plugins should have been deployed", before, pluginCount());
		assertEquals(1, pluginManager().getPluginIds().size());
	}

	@Test
	public void testExtensionHandling() throws IOException, TimeoutException {
		grantAdmin();

		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(EXTENSION_CONSUMER_PATH, "extension-consumer.jar");
		}
		try (ExpectedEvent ee = expectEvent(MeshEvent.PLUGIN_REGISTERED, 10_000)) {
			copyAndDeploy(EXTENSION_PROVIDER_PATH, "extension-provider.jar");
		}

		assertEquals("My dummy extension\n", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/extension-consumer/extensions"));

		call(() -> client().undeployPlugin("extension-provider"));
		assertEquals("", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/extension-consumer/extensions"));

	}

	@Test
	public void testClientPlugin() throws IOException {
		grantAdmin();

		copyAndDeploy(CLIENT_PATH, "client.jar");
		assertEquals(1, pluginManager().getPluginIds().size());

		assertNotNull("Should find a default John Doe user", JsonUtil.readValue(httpGetNow(CURRENT_API_BASE_PATH + "/plugins/client/me"), UserResponse.class));
	}

}
