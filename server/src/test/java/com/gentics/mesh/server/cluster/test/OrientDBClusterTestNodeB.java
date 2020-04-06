package com.gentics.mesh.server.cluster.test;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.server.cluster.test.task.RoleInserterTask;

public class OrientDBClusterTestNodeB extends AbstractClusterTest {

	@Test
	public void testServer() throws Exception {
		MeshOptions options = init(null);
		options.setNodeName("gentics-mesh-2");
		options.getStorageOptions().setDirectory("data2/graphdb");
		options.getClusterOptions().setVertxPort(6152);
		options.getHttpServerOptions().setPort(8082);
		options.getMonitoringOptions().setPort(8882);
		setup(options);
		triggerLoad(new RoleInserterTask(this));
		waitAndShutdown();
	}

}
