package com.gentics.mesh.distributed;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.distributed.containers.MeshDockerServer;

import io.vertx.core.Vertx;

/**
 * Assert that a node will not be able to join the cluster if the mesh versions are not matching.
 */
public class NodeRejectionClusterTest {

	private static Vertx vertx = Vertx.vertx();

	public static MeshDockerServer serverA = new MeshDockerServer("dockerCluster", "nodeA", true, true, true, vertx, 8000, "-Dmesh.internal.version=0.10.0");

	public static MeshDockerServer serverB = new MeshDockerServer("dockerCluster", "nodeB", false, false, true, vertx, null, "-Dmesh.internal.version=0.10.1");

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
	}
	
	@Test
	public void testCluster( ) {
			
	}
}
