package com.gentics.mesh.server.cluster.test;

import org.junit.Test;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.server.cluster.test.task.RoleEdgeInserterTask;

public class OrientDBClusterTestNodeA extends AbstractClusterTest {

	@Test
	public void testServer() throws Exception {
		MeshOptions options = init(null);
		options.setInitCluster(true);
		options.setNodeName("gentics-mesh-1");
		options.getStorageOptions().setDirectory("data1/graphdb");
		options.getClusterOptions().setVertxPort(6151);
		options.getHttpServerOptions().setPort(8081);
		options.getMonitoringOptions().setPort(8881);
		setup(options);
		triggerLoad(new RoleEdgeInserterTask(this));
		waitAndShutdown();
	}

}
