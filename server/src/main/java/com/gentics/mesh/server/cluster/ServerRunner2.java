package com.gentics.mesh.server.cluster;

import java.io.File;

import com.gentics.mesh.etc.config.MeshOptions;

public class ServerRunner2 extends ClusterServer {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = init(args);
		options.setNodeName("gentics-mesh-2");
		options.getStorageOptions().setDirectory("data2/graphdb");
		options.getClusterOptions().setVertxPort(6152);
		options.getHttpServerOptions().setPort(8082);
		options.getMonitoringOptions().setPort(8882);
		run(options);
	}

}
