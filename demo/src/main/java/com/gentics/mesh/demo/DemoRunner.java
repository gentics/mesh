package com.gentics.mesh.demo;

import static com.gentics.mesh.demo.DemoZipHelper.unzip;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.demo.verticle.DemoAppEndpoint;
import com.gentics.mesh.demo.verticle.DemoVerticle;
import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.router.EndpointRegistry;
import com.gentics.mesh.util.DeploymentUtil;
import com.gentics.mesh.verticle.admin.AdminGUI2Endpoint;
import com.gentics.mesh.verticle.admin.AdminGUIEndpoint;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import net.lingala.zip4j.exception.ZipException;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class DemoRunner {

	private static Logger log;

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
		System.setProperty("storage.trackChangedRecordsInWAL", "true");
	}

	/**
	 * Start the process.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		LoggingConfigurator.init();
		log = LoggerFactory.getLogger(DemoRunner.class);
		// Extract dump file on first time startup to speedup startup
		setupDemo();

		AbstractMeshOptions options = OptionsLoader.createOrloadOptions(args);

		Mesh mesh = Mesh.create(options);
		mesh.setCustomLoader(vertx -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
			MeshComponent meshInternal = mesh.internal();
			EndpointRegistry registry = meshInternal.endpointRegistry();

			// Add demo content provider
			registry.register(DemoAppEndpoint.class);
			DemoDataProvider data = new DemoDataProvider(meshInternal.database(), meshInternal.meshLocalClientImpl(), meshInternal.boot());
			DemoVerticle demoVerticle = new DemoVerticle(meshInternal.boot(), data);
			DeploymentUtil.deployAndWait(vertx, config, demoVerticle, false);

			// Add admin ui
			registry.register(AdminGUIEndpoint.class);
			registry.register(AdminGUI2Endpoint.class);
		});
		try {
			mesh.run();
		} catch (Throwable t) {
			log.error("Error while starting mesh. Invoking shutdown.", t);
			mesh.shutdownAndTerminate(10);
		}
	}

	private static void setupDemo() throws FileNotFoundException, IOException, ZipException {
		File dataDir = new File("data");
		if (!dataDir.exists() || dataDir.list().length == 0) {
			log.info("Extracting demo data since this is the first time you start mesh...");
			unzip("/mesh-dump.zip", "data");
			log.info("Demo data extracted to {" + dataDir.getAbsolutePath() + "}");
		}
	}

}
