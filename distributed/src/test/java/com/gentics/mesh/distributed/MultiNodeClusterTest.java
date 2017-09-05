package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

import io.vertx.core.Vertx;

/**
 * Test how a cluster behaves with more then two nodes.
 */
@Ignore
public class MultiNodeClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static Vertx vertx = Vertx.vertx();
	// public static MeshLocalServer serverA = new MeshLocalServer("localNodeA", true, true);

	public static MeshDockerServer serverA = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeA", randomToken(), true, true, true, vertx,
			null, null);

	public static MeshDockerServer serverB = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeB", randomToken(), false, false, true, vertx,
			null, null);

	public static MeshDockerServer serverC = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeC", randomToken(), false, false, true, vertx,
			null, null);

	public static MeshDockerServer serverD = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeD", randomToken(), false, false, true, vertx,
			null, null);

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverD).around(serverC).around(serverB).around(serverA);

	/**
	 * Test that a cluster with multiple nodes can form and that changes are distributed.
	 */
	@Test
	public void testCluster() throws InterruptedException {
		ProjectResponse response = call(
				() -> serverA.getMeshClient().createProject(new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder")));
		Thread.sleep(1000);

		serverB.awaitStartup(200);
		serverC.awaitStartup(200);
		serverD.awaitStartup(200);

		call(() -> serverB.getMeshClient().findProjectByUuid(response.getUuid()));
		call(() -> serverC.getMeshClient().findProjectByUuid(response.getUuid()));
		call(() -> serverD.getMeshClient().findProjectByUuid(response.getUuid()));

		ProjectResponse response2 = call(
				() -> serverD.getMeshClient().createProject(new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder")));

		Thread.sleep(1000);
		call(() -> serverA.getMeshClient().findProjectByUuid(response2.getUuid()));
		call(() -> serverB.getMeshClient().findProjectByUuid(response2.getUuid()));
		call(() -> serverC.getMeshClient().findProjectByUuid(response2.getUuid()));
		call(() -> serverD.getMeshClient().findProjectByUuid(response2.getUuid()));
	}
}
