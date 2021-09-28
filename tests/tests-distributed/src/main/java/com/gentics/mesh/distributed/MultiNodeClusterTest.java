package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.RuleChain;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.test.category.ClusterTests;
import com.gentics.mesh.test.docker.MeshContainer;

/**
 * Test how a cluster behaves with more than two nodes.
 */
@Category(ClusterTests.class)
public class MultiNodeClusterTest extends AbstractClusterTest {

	private static final int STARTUP_TIMEOUT = 500;

	private static String clusterPostFix = randomUUID();

	// public static MeshLocalServer serverA = new MeshLocalServer("localNodeA", true, true);

	public static MeshContainer serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withFilesystem()
		.withClearFolders();

	public static MeshContainer serverB = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeB")
		.withDataPathPostfix(randomToken())
		.withFilesystem()
		.withClearFolders();

	public static MeshContainer serverC = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeC")
		.withDataPathPostfix(randomToken())
		.withFilesystem()
		.withClearFolders();

	public static MeshContainer serverD = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeD")
		.withDataPathPostfix(randomToken())
		.withFilesystem()
		.withClearFolders();

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverD).around(serverC).around(serverB).around(serverA);

	@BeforeClass
	public static void login() throws InterruptedException {
		serverB.awaitStartup(STARTUP_TIMEOUT);
		serverB.login();
		serverC.awaitStartup(STARTUP_TIMEOUT);
		serverC.login();
		serverD.awaitStartup(STARTUP_TIMEOUT);
		serverD.login();
		serverA.awaitStartup(STARTUP_TIMEOUT);
		serverA.login();
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
