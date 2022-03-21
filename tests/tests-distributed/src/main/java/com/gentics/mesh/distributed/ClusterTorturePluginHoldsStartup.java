package com.gentics.mesh.distributed;

import static com.gentics.mesh.util.TokenUtil.randomToken;

import java.io.File;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.test.category.ClusterTests;
import com.gentics.mesh.test.docker.MeshContainer;

@Category(ClusterTests.class)
public class ClusterTorturePluginHoldsStartup extends AbstractClusterTortureTest {

	private static final int STARTUP_TIMEOUT = 60 * 6;
	
	@Test
	public void testSecondaryBackupCreated() throws Exception {
		torture((a, b, c) -> {
			MeshContainer serverB2 = prepareSlave("dockerCluster" + clusterPostFix, "nodeB2", randomToken(), true, true, 1)
					.withPlugin(new File("../core/target/test-plugins/failing-first/target/failing-first-plugin-0.0.1-SNAPSHOT.jar"), "failing-first.jar");
			serverB2.start();
			serverB2.awaitStartup(STARTUP_TIMEOUT);
			login(serverB2);
		});
	}	
}
