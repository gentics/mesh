package com.gentics.mesh.distributed;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

import io.vertx.core.Vertx;

/**
 * Assert that a node will not be able to join the cluster if the mesh versions are not matching.
 */
@Ignore
public class NodeRejectionClusterTest {

	private static String clusterPostFix = randomUUID();

	private static Vertx vertx = Vertx.vertx();

	public static MeshDockerServer serverA = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeA", randomToken(), true, true, true, vertx,
			null, "-Dmesh.internal.version=0.10.0");

	public static MeshDockerServer serverB = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeB", randomToken(), false, false, true, vertx,
			null, "-Dmesh.internal.version=0.10.1");

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
	}

	@Test
	public void testCluster() throws InterruptedException {
		// Wait some time to give the other node time to join the cluster
		Thread.sleep(30000);
		// Verify that the node did not join the cluster since the version is different.
		ClusterStatusResponse response = call(() -> serverA.getMeshClient().clusterStatus());
		assertThat(response.getInstances()).hasSize(1);
		assertEquals("nodeA", response.getInstances().get(0).getName());
	}
}
