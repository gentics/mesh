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

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.plugin.PluginDeploymentRequest;
import com.gentics.mesh.core.rest.plugin.PluginListResponse;
import com.gentics.mesh.core.rest.plugin.PluginManifest;
import com.gentics.mesh.core.rest.plugin.PluginResponse;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.plugin.ClonePlugin;
import com.gentics.mesh.plugin.ManifestInjectorPlugin;
import com.gentics.mesh.plugin.NonMeshPluginVerticle;
import com.gentics.mesh.plugin.PluginManager;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.ServiceHelper;
import io.vertx.reactivex.core.Vertx;

@MeshTestSetting(testSize = PROJECT, startServer = true, inMemoryDB = true)
public class AdminPluginEndpointTest extends AbstractMeshTest {

	private static final String API_NAME = "basic";

	private static final String DEPLOYMENT_NAME = "filesystem:target/test-plugins/basic/target/basic-plugin-0.0.1-SNAPSHOT.jar";

	private static final String DEPLOYMENT2_NAME = "filesystem:target/test-plugins/basic2/target/basic2-plugin-0.0.1-SNAPSHOT.jar";

	private static PluginManager manager = ServiceHelper.loadFactory(PluginManager.class);

	@Before
	public void clearDeployments() {
		// Copy the uuids to avoid concurrency issues
		Set<String> uuids = new HashSet<>(manager.getPlugins().keySet());
		for (String uuid : uuids) {
			manager.undeploy(uuid).blockingAwait();
		}

		setPluginBaseDir(".");
	}

	@Test
	public void testDeployPluginMissingPermission() {
		revokeAdminRole();
		PluginDeploymentRequest request = new PluginDeploymentRequest().setName(DEPLOYMENT_NAME);
		call(() -> client().deployPlugin(request), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReadPluginMissingPermission() {
		grantAdminRole();
		PluginDeploymentRequest request = new PluginDeploymentRequest().setName(DEPLOYMENT_NAME);
		PluginResponse deployment = call(() -> client().deployPlugin(request));

		revokeAdminRole();
		String uuid = deployment.getUuid();
		call(() -> client().findPlugin(uuid), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testNonPluginDeployment() {
		grantAdminRole();
		String name = NonMeshPluginVerticle.class.getCanonicalName();
		System.out.println(name);
		int before = vertx().deploymentIDs().size();
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(name)), BAD_REQUEST, "admin_plugin_error_plugin_did_not_register");
		assertEquals("The verticle should not stay deployed.", before, vertx().deploymentIDs().size());
	}

	@Test
	public void testUndeployPluginMissingPermission() {
		grantAdminRole();
		PluginDeploymentRequest request = new PluginDeploymentRequest().setName(DEPLOYMENT_NAME);
		PluginResponse deployment = call(() -> client().deployPlugin(request));

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
		PluginDeploymentRequest request = new PluginDeploymentRequest().setName(DEPLOYMENT_NAME);
		PluginResponse deployment = call(() -> client().deployPlugin(request));
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
	public void testPluginList() {
		grantAdminRole();
		PluginResponse response = call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)));
		assertEquals(1, manager.getPlugins().size());

		String bogusName = "filesystem:bogus.jar";

		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(bogusName)), BAD_REQUEST,
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
	@Ignore("The current classpath handling issue requires us to disable isolation levels and thus the test fails.")
	public void testStaticHandler() throws IOException {
		grantAdminRole();
		PluginDeploymentRequest request = new PluginDeploymentRequest().setName(DEPLOYMENT_NAME);
		call(() -> client().deployPlugin(request));
		request.setName(DEPLOYMENT2_NAME);
		call(() -> client().deployPlugin(request));

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
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)), BAD_REQUEST,
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
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)), BAD_REQUEST,
			"admin_plugin_error_validation_failed_apiname_invalid", "test");

		ManifestInjectorPlugin.manifest.setApiName("some/slash");
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)), BAD_REQUEST,
			"admin_plugin_error_validation_failed_apiname_invalid", "test");

		ManifestInjectorPlugin.manifest.setApiName("ok");
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)));
	}

	@Test
	public void testMulitpleDeployments() throws IOException {
		grantAdminRole();

		final String CLONE_PLUGIN_DEPLOYMENT_NAME = ClonePlugin.class.getCanonicalName();
		for (int i = 0; i < 100; i++) {
			call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(CLONE_PLUGIN_DEPLOYMENT_NAME)));
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
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)));
		assertEquals(1, manager.getPlugins().size());

		int before = Vertx.vertx().deploymentIDs().size();
		call(() -> client().deployPlugin(new PluginDeploymentRequest().setName(DEPLOYMENT_NAME)), BAD_REQUEST,
			"admin_plugin_error_plugin_already_deployed", "Basic Plugin", "basic");
		assertEquals("No additional plugins should have been deployed", before, Vertx.vertx().deploymentIDs().size());
		assertEquals(1, manager.getPlugins().size());
	}

	private void setPluginBaseDir(String baseDir) {
		File pluginDir = new File(baseDir);
		pluginDir.mkdirs();
		MeshOptions options = new MeshOptions();
		options.setPluginDirectory(baseDir);
		manager.init(options);
	}

}
