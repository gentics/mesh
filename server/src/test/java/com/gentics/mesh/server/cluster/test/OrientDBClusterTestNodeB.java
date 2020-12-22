package com.gentics.mesh.server.cluster.test;

import org.junit.Test;

import com.gentics.mesh.etc.config.AbstractMeshOptions;
import com.gentics.mesh.server.cluster.test.task.RoleCRUDGlobalLockInserterTask;

/**
 * Dedicated test runner class for manual cluster tests.
 */
public class OrientDBClusterTestNodeB extends AbstractClusterTest {

	@Test
	public void testServer() throws Exception {
		AbstractMeshOptions options = init(null);
		options.setNodeName("gentics-mesh-2");
		options.getStorageOptions().setDirectory("data2/graphdb");
		options.getClusterOptions().setVertxPort(6152);
		options.getHttpServerOptions().setPort(8082);
		options.getMonitoringOptions().setPort(8882);
		setup(options);
		triggerLoad(new RoleCRUDGlobalLockInserterTask(this));
		waitAndShutdown();
	}

}
