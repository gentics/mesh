package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.admin.cluster.ClusterStatusResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

/**
 * Assert that a node will not be able to join the cluster if the mesh versions and the database revision are not matching.
 */
public class NodeRejectionClusterTest extends AbstractClusterTest {

	private static final int STARTUP_TIMEOUT = 110;

	private static String clusterPostFix = randomUUID();

	public static MeshDockerServer serverA = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.withClearFolders()
		.waitForStartup()
		.withExtraOpts("-Dmesh.internal.version=0.10.0 -Dmesh.internal.dbrev=EFG");

	public static MeshDockerServer serverB = new MeshDockerServer()
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withClearFolders()
		.withExtraOpts("-Dmesh.internal.version=0.10.1 -Dmesh.internal.dbrev=ABC");

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		serverA.awaitStartup(STARTUP_TIMEOUT);
		serverA.login();
	}

	@Test
	public void testCluster() throws InterruptedException {
		// Wait some time to give the other node time to join the cluster
		Thread.sleep(30_000);
		// Verify that the node did not join the cluster since the version is different.
		ClusterStatusResponse response = call(() -> serverA.client().clusterStatus());
		assertThat(response.getInstances()).hasSize(1);
		assertEquals("nodeA", response.getInstances().get(0).getName());
		assertEquals("EFG", call(() -> serverA.client().getApiInfo()).getDatabaseRevision());

	}
}
