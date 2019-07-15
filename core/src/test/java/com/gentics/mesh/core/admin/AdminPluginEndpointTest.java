package com.gentics.mesh.core.admin;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.handler.VersionHandler.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.reactivex.core.Vertx;

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
		String uuid = deployment.getUuid();
		call(() -> client().findPlugin(uuid), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testNonPluginDeployment() throws IOException {
		grantAdminRole();
		String name = "non-mesh.jar";

		int before = Mesh.mesh().pluginIds().size();
		copyAndDeploy(NON_MESH_PATH, name, BAD_REQUEST, "admin_plugin_error_plugin_did_not_register");

		int after = Mesh.mesh().pluginIds().size();
		assertEquals("The verticle should not stay deployed.", before, after);
	}

	@Test
	public void testUndeployPluginMissingPermission() throws IOException {
		grantAdminRole();
		PluginResponse deployment = copyAndDeploy(BASIC_PATH, "plugin.jar");

		revokeAdminRole();
		String uuid = deployment.getUuid();
		call(() -> client().undeployPlugin(uuid), FORBIDDEN, "error_admin_permission_required");
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
		assertTrue(UUIDUtil.isUUID(deployment.getUuid()));

		assertEquals("world", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/" + API_NAME + "/hello"));
		assertEquals("world-project", httpGetNow(CURRENT_API_BASE_PATH + "/" + PROJECT_NAME + "/plugins/" + API_NAME + "/hello"));

		PluginListResponse list = call(() -> client().findPlugins());
		assertEquals(1, list.getMetainfo().getTotalCount());

		PluginResponse response = call(() -> client().findPlugin(deployment.getUuid()));
		assertEquals(deployment.getName(), response.getName());

		call(() -> client().undeployPlugin(deployment.getUuid()));

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

		PluginResponse response2 = call(() -> client().findPlugin(response.getUuid()));
		assertEquals(response.getUuid(), response2.getUuid());

		call(() -> client().findPlugin("bogus"), NOT_FOUND, "admin_plugin_error_plugin_not_found", "bogus");
		PluginListResponse pluginList = call(() -> client().findPlugins());
		assertNull(pluginList.getMetainfo().getPerPage());
		assertEquals(1, pluginList.getMetainfo().getCurrentPage());
		assertEquals(1, pluginList.getMetainfo().getPageCount());
		assertEquals(1, pluginList.getMetainfo().getTotalCount());

		pluginList = call(() -> client().findPlugins(new PagingParametersImpl().setPerPage(1L)));
		assertEquals(1, pluginList.getMetainfo().getPerPage().longValue());
		assertEquals(1, pluginList.getMetainfo().getCurrentPage());
		assertEquals(1, pluginList.getMetainfo().getPageCount());
		assertEquals(1, pluginList.getMetainfo().getTotalCount());

		GenericMessageResponse msg = call(() -> client().undeployPlugin(response.getUuid()));
		assertThat(msg).matches("admin_plugin_undeployed", response.getUuid());
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
		assertEquals("content2", httpGetNow(CURRENT_API_BASE_PATH + "/plugins/basic2/static/file.txt"));
	}

	@Test
	public void testInvalidManifest() {
		ManifestInjectorPlugin.manifest = new PluginManifest()
			.setApiName("api")
			.setAuthor("Joe Doe")
			.setName("test")
			.setInception("2018")
			.setDescription("some Text")
			.setLicense("Apache 2.0")
			.setVersion(null);

		grantAdminRole();
		final String DEPLOYMENT_NAME = ManifestInjectorPlugin.class.getCanonicalName();
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(DEPLOYMENT_NAME)), BAD_REQUEST,
			"admin_plugin_error_validation_failed_field_missing", "version");
	}

	@Test
	public void testManifestWithInvalidAPIName() {
		ManifestInjectorPlugin.manifest = new PluginManifest()
			.setApiName("api with spaces")
			.setAuthor("Joe Doe")
			.setDescription("some Text")
			.setName("test")
			.setInception("2018")
			.setLicense("Apache 2.0")
			.setVersion("1.0");

		grantAdminRole();
		final String DEPLOYMENT_NAME = ManifestInjectorPlugin.class.getCanonicalName();
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(DEPLOYMENT_NAME)), BAD_REQUEST,
			"admin_plugin_error_validation_failed_apiname_invalid", "test");

		ManifestInjectorPlugin.manifest.setApiName("some/slash");
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(DEPLOYMENT_NAME)), BAD_REQUEST,
			"admin_plugin_error_validation_failed_apiname_invalid", "test");

		ManifestInjectorPlugin.manifest.setApiName("ok");
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(DEPLOYMENT_NAME)));
	}

	@Test
	public void testMultipleDeployments() throws IOException {
		grantAdminRole();

		final String CLONE_PLUGIN_DEPLOYMENT_NAME = ClonePlugin.class.getCanonicalName();
		for (int i = 0; i < 100; i++) {
			call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(CLONE_PLUGIN_DEPLOYMENT_NAME)));
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
	public void testDuplicateDeployment() {
		grantAdminRole();
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(BASIC_PATH)));
		assertEquals(1, pluginManager().getPluginIds().size());

		int before = Vertx.vertx().deploymentIDs().size();
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setPath(BASIC_PATH)), BAD_REQUEST,
			"admin_plugin_error_plugin_already_deployed", "Basic Plugin", "basic");
		assertEquals("No additional plugins should have been deployed", before, Vertx.vertx().deploymentIDs().size());
		assertEquals(1, pluginManager().getPluginIds().size());
	}

}
