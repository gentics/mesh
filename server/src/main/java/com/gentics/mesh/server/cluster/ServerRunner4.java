package com.gentics.mesh.server.cluster;

import java.io.File;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Cluster Runner 4
 */
public class ServerRunner4 extends ClusterServer {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	/**
	 * Run the server.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		MeshOptions options = init(args);
		options.setNodeName("gentics-mesh-backup");
		options.getStorageOptions().setDirectory("data4/graphdb");
		options.getClusterOptions().setVertxPort(6154);
		options.getHttpServerOptions().setPort(8084);
		options.getMonitoringOptions().setPort(8884);
		run(options);
	}

}
