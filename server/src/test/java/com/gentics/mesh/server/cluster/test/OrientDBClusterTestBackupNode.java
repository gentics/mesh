package com.gentics.mesh.server.cluster.test;

import org.junit.Before;

import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.server.cluster.test.task.BackupTask;

public class OrientDBClusterTestBackupNode extends AbstractClusterTest {

	@Before
	public void testServer() throws Exception {
		MeshOptions options = init(null);
		options.setNodeName("gentics-mesh-backup");
		options.getStorageOptions().setDirectory("data4/graphdb");
		options.getClusterOptions().setVertxPort(6154);
		options.getHttpServerOptions().setPort(8084);
		options.getMonitoringOptions().setPort(8884);
		setup(options);
		triggerSlowLoad(new BackupTask(this));
		waitAndShutdown();
	}

}
