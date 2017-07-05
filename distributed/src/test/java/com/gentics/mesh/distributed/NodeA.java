package com.gentics.mesh.distributed;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.logging.SLF4JLogDelegateFactory;

public class NodeA {

	private static final Logger log;

	static {
		// Use slf4j instead of jul
		System.setProperty(LoggerFactory.LOGGER_DELEGATE_FACTORY_CLASS_NAME, SLF4JLogDelegateFactory.class.getName());
		log = LoggerFactory.getLogger(NodeA.class);
	}

	public static void main(String[] args) throws Exception {
		String baseDirPath = "target/nodeA";
		File baseDir = new File(baseDirPath);
		// FileUtils.deleteDirectory(baseDir);
		baseDir.mkdirs();

		MeshOptions options = new MeshOptions();
		options.setClusterMode(true);
		options.getHttpServerOptions().setEnableCors(true);
		options.getHttpServerOptions().setCorsAllowedOriginPattern("*");
		options.getHttpServerOptions().setPort(8080);
		options.getAuthenticationOptions().setKeystorePassword("nodeA");
		options.setTempDirectory(baseDirPath + "/tmp");
		options.getStorageOptions().setDirectory(baseDirPath + "/graphdb");
		options.getSearchOptions().setDirectory(baseDirPath + "/search");
		options.getImageOptions().setImageCacheDirectory(baseDirPath + "/imageCache");
		options.getUploadOptions().setDirectory(baseDirPath + "/binaryFiles");
		options.getUploadOptions().setTempDirectory(baseDirPath + "/tmpupload");
		options.getAuthenticationOptions().setKeystorePath(baseDirPath + "/keystore.jceks");

		Mesh mesh = Mesh.mesh(options);
		mesh.run();
	}

}
