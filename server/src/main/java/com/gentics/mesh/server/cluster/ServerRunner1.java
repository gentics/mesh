package com.gentics.mesh.server.cluster;

import java.io.File;

import com.gentics.mesh.etc.config.MeshOptions;

/**
 * Cluster Runner 1
 */
public class ServerRunner1 extends ClusterServer {

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
		options.setInitCluster(true);
		options.setNodeName("gentics-mesh-1");
		options.getStorageOptions().setDirectory("data1/graphdb");
		options.getClusterOptions().setVertxPort(6151);
		options.getHttpServerOptions().setPort(8081);
		options.getMonitoringOptions().setPort(8881);
		run(options);
	}

}
