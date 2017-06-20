package com.gentics.mesh.server;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

/**
 * Main runner that is used to deploy a preconfigured set of verticles.
 */
public class ServerRunner {

	private static final Logger log;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(ServerRunner.class);
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = OptionsLoader.createOrloadOptions();
		setupKeystore(options);
		options.setClusterMode(true);

		Mesh mesh = Mesh.mesh(options);
		mesh.setCustomLoader((vertx) -> {
			JsonObject config = new JsonObject();
			config.put("port", options.getHttpServerOptions().getPort());
			// DeploymentUtil.deployAndWait(vertx, config, new AdminGUIVerticle(MeshInternal.get().routerStorage()), false);
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
