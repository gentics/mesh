package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.plugin.AbstractPluginTest;
import com.gentics.mesh.plugin.ClonePlugin;
import com.gentics.mesh.plugin.ManifestInjectorPlugin;
import com.gentics.mesh.plugin.PluginManifest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminPluginEndpointTest extends AbstractPluginTest {

	private static final String API_NAME = "basic";

	@Test
	public void testDeployPluginMissingPermission() throws IOException {
		revokeAdminRole();
		copyAndDeploy(BASIC_PATH, "plugin.jar", FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReadPluginMissingPermission() throws IOException {
		grantAdminRole();
		PluginResponse deployment = copyAndDeploy(BASIC_PATH, "plugin.jar");

		revokeAdminRole();
		String id = deployment.getId();
		call(() -> client().findPlugin(id), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testNonPluginDeployment() throws IOException {
		grantAdminRole();
		String name = "non-mesh.jar";

		int before = Mesh.mesh().pluginUuids().size();
		copyAndDeploy(NON_MESH_PATH, name, INTERNAL_SERVER_ERROR, "admin_plugin_error_plugin_loading_failed", name);

		int after = Mesh.mesh().pluginUuids().size();
		assertEquals("The verticle should not stay deployed.", before, after);
	}

	@Test
	public void testUndeployPluginMissingPermission() throws IOException {
		grantAdminRole();
		PluginResponse deployment = copyAndDeploy(BASIC_PATH, "plugin.jar");

		revokeAdminRole();
		String id = deployment.getId();
		call(() -> client().undeployPlugin(id), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReadPluginListMissingPermission() {
		revokeAdminRole();
		call(() -> client().findPlugins(), FORBIDDEN, "error_admin_permission_required");
	}

	/**
	 * Deploy the basic plugin and assert that the plugin routes can be reached.
	 * 
	 * @throws IOException
	 */
	@Test
	public void testDeployPlugin() throws IOException {
		grantAdminRole();

		PluginResponse deployment = copyAndDeploy(BASIC_PATH, "basic-plugin.jar");
		assertEquals("basic", deployment.getId());

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + API_NAME + "/hello"));
		assertEquals("world-project", httpGetNow(CURRENT_API_BASE_PATH + "/" + PROJECT_NAME + "/plugins/" + API_NAME + "/hello"));

		PluginListResponse list = call(() -> client().findPlugins());
		assertEquals(1, list.getMetainfo().getTotalCount());

		PluginResponse response = call(() -> client().findPlugin(deployment.getId()));
		assertEquals(deployment.getName(), response.getName());

		call(() -> client().undeployPlugin(deployment.getId()));

		assertEquals(404, httpGet(CURRENT_API_BASE_PATH + "/plugins/" + API_NAME + "/hello").execute().code());

	}

	@Test
	public void testPluginList() throws IOException {
		grantAdminRole();
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
	public void testStaticHandler() throws IOException {
		grantAdminRole();

		copyAndDeploy(BASIC_PATH, "plugin.jar");
		copyAndDeploy(BASIC2_PATH, "plugin2.jar");

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic/hello"));
		assertEquals("world2", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic2/hello"));
		assertEquals("content", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic/static/file.txt"));
		assertEquals("content2", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic2/static2/file.txt"));
	}

	@Test
	public void testClassLoaderHandling() throws IOException {
		grantAdminRole();

		copyAndDeploy(CLASSLOADER_PATH, "plugin.jar");

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

		grantAdminRole();
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

		grantAdminRole();

		deployPlugin(ManifestInjectorPlugin.class, "injector", BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", "injector");

		ManifestInjectorPlugin.apiName = "api/with/slashes";
		deployPlugin(ManifestInjectorPlugin.class, "injector", BAD_REQUEST, "admin_plugin_error_validation_failed_apiname_invalid", "injector");

		ManifestInjectorPlugin.apiName = "ok";
		deployPlugin(ManifestInjectorPlugin.class, "injector");
	}

	@Test
	public void testMultipleDeployments() throws IOException {
		grantAdminRole();

		for (int i = 0; i < 100; i++) {
			deployPlugin(ClonePlugin.class, "clone" + i);
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
		grantAdminRole();

		copyAndDeploy(BASIC_PATH, "plugin.jar");
		assertEquals(1, pluginManager().getPluginIds().size());

		long before = pluginCount();
		copyAndDeploy(BASIC_PATH, "plugin2.jar", BAD_REQUEST, "admin_plugin_error_plugin_with_id_already_deployed", "plugin2.jar");
		assertEquals("No additional plugins should have been deployed", before, pluginCount());
		assertEquals(1, pluginManager().getPluginIds().size());
	}

}
