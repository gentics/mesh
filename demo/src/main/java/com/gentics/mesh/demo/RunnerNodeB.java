package com.gentics.mesh.demo;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.demo.verticle.DemoAppEndpoint;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class RunnerNodeB {

	private static final String basePath = "data-nodeB";

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", basePath + File.separator + "tmp");
		System.setProperty("mesh.confDirName", "config-nodeB");
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		options.getStorageOptions().setDirectory(basePath + "/graph");
//		options.getSearchOptions().setDirectory(basePath + "/es");
		options.getUploadOptions().setDirectory(basePath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(basePath + "/temp");
		options.getHttpServerOptions().setPort(8081);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getAuthenticationOptions().setKeystorePath(basePath + "/keystore.jkms");
		// options.getSearchOptions().setHttpEnabled(true);
		options.getClusterOptions().setEnabled(true);
		options.getClusterOptions().setClusterName("testcluster");

		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
			MeshComponent meshInternal = mesh.internal();
			EndpointRegistry registry = meshInternal.endpointRegistry();

			// Add demo content provider
			registry.register(DemoAppEndpoint.class);
			DemoVerticle demoVerticle = new DemoVerticle(meshInternal.boot(), new DemoDataProvider(meshInternal.database(), meshInternal.meshLocalClientImpl(),
					meshInternal.boot()));
			DeploymentUtil.deployAndWait(vertx, config, demoVerticle, false);

			// Add admin ui
			registry.register(AdminGUIEndpoint.class);

			// Add elastichead
			// if (options.getSearchOptions().isHttpEnabled()) {
			// registry.register(ElasticsearchHeadEndpoint.class);
			// }
		});
		mesh.run();
	}
}
