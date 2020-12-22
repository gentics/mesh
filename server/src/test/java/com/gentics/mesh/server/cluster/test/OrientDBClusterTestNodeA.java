package com.gentics.mesh.server.cluster.test;

import org.junit.Test;

import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.server.cluster.test.task.RoleCRUDGlobalLockInserterTask;

/**
 * Dedicated test runner class for manual cluster tests.
 */
public class OrientDBClusterTestNodeA extends AbstractClusterTest {

	@Test
	public void testServer() throws Exception {
		AbstractMeshOptions options = init(null);
		options.setInitCluster(true);
		options.setNodeName("gentics-mesh-1");
		options.getStorageOptions().setDirectory("data1/graphdb");
		options.getClusterOptions().setVertxPort(6151);
		options.getHttpServerOptions().setPort(8081);
		options.getMonitoringOptions().setPort(8881);
		setup(options);
		triggerLoad(new RoleCRUDGlobalLockInserterTask(this));
		waitAndShutdown();
	}

}
