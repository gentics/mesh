package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import org.junit.BeforeClass;
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
@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
public class MultiNodeClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	private static Vertx vertx = Vertx.vertx();
	// public static MeshLocalServer serverA = new MeshLocalServer("localNodeA", true, true);

	public static MeshDockerServer serverA = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withClearFolders();

	public static MeshDockerServer serverB = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withClearFolders();

	public static MeshDockerServer serverC = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeC")
		.withDataPathPostfix(randomToken())
		.withClearFolders();

	public static MeshDockerServer serverD = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeD")
		.withDataPathPostfix(randomToken())
		.withClearFolders();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverD).around(serverC).around(serverB).around(serverA);

	@BeforeClass
	public static void login() throws InterruptedException {
		serverB.awaitStartup(200);
		serverB.login();
		serverC.awaitStartup(200);
		serverC.login();
		serverD.awaitStartup(200);
		serverD.login();
	}

	/**
	 * Test that a cluster with multiple nodes can form and that changes are distributed.
	 */
	@Test
	public void testCluster() throws InterruptedException {
		ProjectResponse response = call(
			() -> serverA.client().createProject(new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder")));
		Thread.sleep(1000);

		call(() -> serverB.client().findProjectByUuid(response.getUuid()));
		call(() -> serverC.client().findProjectByUuid(response.getUuid()));
		call(() -> serverD.client().findProjectByUuid(response.getUuid()));

		ProjectResponse response2 = call(
			() -> serverD.client().createProject(new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder")));

		Thread.sleep(1000);
		call(() -> serverA.client().findProjectByUuid(response2.getUuid()));
		call(() -> serverB.client().findProjectByUuid(response2.getUuid()));
		call(() -> serverC.client().findProjectByUuid(response2.getUuid()));
		call(() -> serverD.client().findProjectByUuid(response2.getUuid()));
	}
}
