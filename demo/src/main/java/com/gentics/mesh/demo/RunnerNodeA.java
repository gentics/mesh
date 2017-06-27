package com.gentics.mesh.demo;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.verticle.ElasticsearchHeadVerticle;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class RunnerNodeA {

	private static final String basePath = "data-nodeA";
	private static final Logger log;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", basePath + File.separator + "tmp");
		log = LoggerFactory.getLogger(RunnerNodeA.class);
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		options.getStorageOptions().setDirectory(basePath + "/graph");
		options.getSearchOptions().setDirectory(basePath + "/es");
		options.getUploadOptions().setDirectory(basePath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(basePath + "/temp");
		options.getHttpServerOptions().setPort(8080);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		//options.getSearchOptions().setHttpEnabled(true);
		options.setClusterMode(true);
		setupKeystore(options);

		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add demo content provider
			MeshComponent meshInternal = MeshInternal.get();
			DemoVerticle demoVerticle = new DemoVerticle(
					new DemoDataProvider(meshInternal.database(), meshInternal.meshLocalClientImpl(), meshInternal.boot()),
					MeshInternal.get().routerStorage());
			DeploymentUtil.deployAndWait(vertx, config, demoVerticle, false);

			// Add admin ui
			AdminGUIVerticle adminVerticle = new AdminGUIVerticle(MeshInternal.get().routerStorage());
			DeploymentUtil.deployAndWait(vertx, config, adminVerticle, false);

			// Add elastichead
			if (options.getSearchOptions().isHttpEnabled()) {
				ElasticsearchHeadVerticle headVerticle = new ElasticsearchHeadVerticle(MeshInternal.get().routerStorage());
				DeploymentUtil.deployAndWait(vertx, config, headVerticle, false);
			}
		});
		mesh.run();
	}

	private static void setupKeystore(MeshOptions options) throws Exception {
		String keyStorePath = options.getAuthenticationOptions().getKeystorePath();
		// Copy the demo keystore file to the destination
		if (!new File(keyStorePath).exists()) {
			log.info("Could not find keystore {" + keyStorePath + "}. Creating one for you..");
			KeyStoreHelper.gen(keyStorePath, options.getAuthenticationOptions().getKeystorePassword());
			log.info("Keystore {" + keyStorePath + "} created. The keystore password is listed in your {" + OptionsLoader.MESH_CONF_FILENAME
					+ "} file.");
		}
	}
}
