package com.gentics.mesh.distributed;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.gentics.mesh.test.category.ClusterTests;

/**
 * Cluster Torture: Stop (SIGTERM) all nodes.
 * 
 * @author plyhun
 *
 */
@Category(ClusterTests.class)
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
