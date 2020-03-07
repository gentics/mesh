package com.gentics.mesh.server.cluster;

import java.io.File;

import com.gentics.mesh.etc.config.MeshOptions;

public class ServerRunner3 extends ClusterServer {

	static {
		System.setProperty("vertx.httpServiceFactory.cacheDir", "data" + File.separator + "tmp");
		System.setProperty("vertx.cacheDirBase", "data" + File.separator + "tmp");
	}

	public static void main(String[] args) throws Exception {
		MeshOptions options = init(args);
		options.setNodeName("gentics-mesh-3");
		options.getStorageOptions().setDirectory("data3/graphdb");
		options.getClusterOptions().setVertxPort(6153);
		options.getHttpServerOptions().setPort(8083);
		options.getMonitoringOptions().setPort(8883);
		run(options);
	}

}
