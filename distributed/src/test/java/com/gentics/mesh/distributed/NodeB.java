package com.gentics.mesh.distributed;

import java.io.File;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.OptionsLoader;
import com.gentics.mesh.crypto.KeyStoreHelper;
import com.gentics.mesh.etc.config.MeshOptions;

import ch.qos.logback.core.net.server.ServerRunner;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class NodeB {
	private static final Logger log;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(ServerRunner.class);
	}

	public static void main(String[] args) throws Exception {
		String baseDirPath = "target/nodeB";
		File baseDir = new File(baseDirPath);
		FileUtils.deleteDirectory(baseDir);
		baseDir.mkdirs();

		MeshOptions options = new MeshOptions();
		options.setClusterMode(true);
		options.getHttpServerOptions().setPort(8081);
		options.getAuthenticationOptions().setKeystorePassword("nodeB");
		options.setTempDirectory(baseDirPath + "/tmp");
		options.getStorageOptions().setDirectory(baseDirPath + "/graphdb");
		options.getSearchOptions().setDirectory(baseDirPath + "/search");
		options.getImageOptions().setImageCacheDirectory(baseDirPath + "/imageCache");
		options.getUploadOptions().setDirectory(baseDirPath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(baseDirPath + "/tmpupload");
		options.getAuthenticationOptions().setKeystorePath(baseDirPath + "/keystore.jceks");
		setupKeystore(options);

		Mesh mesh = Mesh.mesh(options);
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
