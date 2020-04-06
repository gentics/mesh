package com.gentics.mesh.server.cluster.test;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;

public class OrientDBClusterTestNodeC extends AbstractClusterTest {

	@Test
	public void testServer() throws Exception {
		MeshOptions options = init(null);
		options.setNodeName("gentics-mesh-3");
		options.getStorageOptions().setDirectory("data3/graphdb");
		options.getClusterOptions().setVertxPort(6153);
		options.getHttpServerOptions().setPort(8083);
		options.getMonitoringOptions().setPort(8883);
		setup(options);
		waitAndShutdown();
	}

}
