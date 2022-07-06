package com.gentics.mesh.demo;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.demo.verticle.DemoAppEndpoint;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.config.OrientDBMeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public abstract class AbstractRunnerNode extends AbstractMeshOptionsDemoContext<OrientDBMeshOptions> {

	static {
		System.setProperty("memory.directMemory.preallocate", "false");
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
	}

	public AbstractRunnerNode(String[] args) {
		super(args, OrientDBMeshOptions.class);
	}

	protected abstract String getBasePath();

	protected abstract int getPort();

	/**
	 * Run the server.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public void run() throws Exception {
		OrientDBMeshOptions options = getOptions();

		options.getStorageOptions().setDirectory(getBasePath() + "/graph");
		// options.getSearchOptions().setDirectory(getBasePath() + "/es");
		options.getUploadOptions().setDirectory(getBasePath() + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(getBasePath() + "/temp");
		options.getHttpServerOptions().setPort(getPort());
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getAuthenticationOptions().setKeystorePath(getBasePath() + "/keystore.jkms");
		// options.getSearchOptions().setHttpEnabled(true);
		options.getClusterOptions().setEnabled(true);
		options.getClusterOptions().setClusterName("testcluster");

		final Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader(vertx -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
			MeshComponent meshInternal = mesh.internal();
			EndpointRegistry registry = meshInternal.endpointRegistry();

			// Add demo content provider
			registry.register(DemoAppEndpoint.class);
			DemoVerticle demoVerticle = new DemoVerticle(meshInternal.boot(),
				new DemoDataProvider(meshInternal.database(), meshInternal.meshLocalClientImpl()));
			DeploymentUtil.deployAndWait(vertx, config, demoVerticle, false);

			// Add admin ui
			registry.register(AdminGUIEndpoint.class);

			// // Add elastichead
			// if (options.getSearchOptions().isHttpEnabled()) {
			// registry.register(ElasticsearchHeadEndpoint.class);
			// }
		});
		try {
			mesh.run();
		} catch (Throwable t) {
			mesh.shutdownAndTerminate(10);
		}
	}
}
