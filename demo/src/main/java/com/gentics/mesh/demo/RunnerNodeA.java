package com.gentics.mesh.demo;

import java.io.File;

import javax.inject.Provider;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.cli.MeshCLI;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.RouterStorage;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.search.verticle.ElasticsearchHeadVerticle;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUIVerticle;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class RunnerNodeA {

	private static final String basePath = "data-nodeA";

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", basePath + File.separator + "tmp");
		System.setProperty("mesh.confDirName", "config-nodeA");
	}

	public static void main(String[] args) throws Exception {

		MeshOptions options = OptionsLoader.createOrloadOptions("-" + MeshCLI.INIT_CLUSTER);
		options.getStorageOptions().setDirectory(basePath + "/graph");
		options.getSearchOptions().setDirectory(basePath + "/es");
		options.getUploadOptions().setDirectory(basePath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(basePath + "/temp");
		options.getHttpServerOptions().setPort(8080);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getAuthenticationOptions().setKeystorePath(basePath + "/keystore.jkms");
		// options.getSearchOptions().setHttpEnabled(true);
		options.getClusterOptions().setEnabled(true);

		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());

			// Add demo content provider
			MeshComponent meshInternal = MeshInternal.get();
			DemoVerticle demoVerticle = new DemoVerticle(
					new DemoDataProvider(meshInternal.database(), meshInternal.meshLocalClientImpl(), meshInternal.boot()), meshInternal.routerStorageProvider());
			DeploymentUtil.deployAndWait(vertx, config, demoVerticle, false);

			// Add admin ui
			AdminGUIVerticle adminVerticle = new AdminGUIVerticle(MeshInternal.get().routerStorageProvider());
			DeploymentUtil.deployAndWait(vertx, config, adminVerticle, false);

			// Add elastichead
			if (options.getSearchOptions().isHttpEnabled()) {
				ElasticsearchHeadVerticle headVerticle = new ElasticsearchHeadVerticle(meshInternal.routerStorageProvider());
				DeploymentUtil.deployAndWait(vertx, config, headVerticle, false);
			}
		});
		mesh.run();
	}

}
