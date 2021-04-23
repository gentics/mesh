package com.gentics.mesh.distributed;

import org.junit.Test;

/**
 * Cluster Torture: Stop (SIGTERM) all nodes.
 * 
 * @author plyhun
 *
 */
public class ClusterTortureAllNodesStoppedTest extends AbstractClusterTortureTest {
	
	@Test
	public void testAllStopped() throws Exception {
		torture((serverA, serverB, contentSchema) -> {
			new Thread(() -> {
					serverB.stop();
					serverA.stop();
			}).run();
		});
	}
}
