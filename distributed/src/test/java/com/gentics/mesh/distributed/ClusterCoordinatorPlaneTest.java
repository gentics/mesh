package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.assertEquals;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.MeshServerInfoModel;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

public class ClusterCoordinatorPlaneTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	public static MeshDockerServer serverA = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.withCoordinatorPlane()
		.withCoordinatorRegex("nodeA")
		.waitForStartup()
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withCoordinatorPlane()
		.withCoordinatorRegex("nodeA")
		.withDataPathPostfix(randomToken())
		.withClearFolders();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverB).around(serverA);

	@Test
	public void testReadClusterConfig() {
		MeshServerInfoModel info = call(() -> serverB.client().getApiInfo());
		System.out.println(info.toJson());
		assertEquals("The request should have been re-directed to serverA", "nodeA", info.getMeshNodeName());
	}

}
