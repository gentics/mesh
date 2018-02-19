package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

/**
 * Tests various interacts with the cluster. (e.g.: Adding new nodes, Removing nodes)
 */
public class ErrorHandlingClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	@ClassRule
	public static MeshDockerServer serverA = new MeshDockerServer("dockerCluster" + clusterPostFix, "nodeA", randomToken(), true, true, true, vertx,
		8000, null);

	@BeforeClass
	public static void setupClient() {
		serverA.login();
	}

	/**
	 * Add a new node to the cluster and check that previously setup projects are setup correctly.
	 * 
	 * @throws Exception
	 */
	@Test
	@Ignore
	public void testProjectInitForNode() throws Exception {
		String newProjectName = randomName();
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> serverA.client().createProject(request));

		MeshDockerServer serverB = addSlave("dockerCluster" + clusterPostFix, "nodeB", randomToken(), true);
		serverB.awaitStartup(20);
		serverB.login();
		// serverB.dropTraffic();
		call(() -> serverB.client().findProjectByUuid(response.getUuid()));
	}

	/**
	 * Start the first node and create a project. Now start the second node and stop it right after it has synchronized. After that create another project and
	 * start the node again. Assert that the previously stopped node did catch up.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRestartingSlave() throws Exception {
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(randomName());
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> serverA.client().createProject(request));

		String dataPathPostfix = randomToken();
		MeshDockerServer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true);
		Thread.sleep(2000);
		serverB1.stop();

		// Node A: Create another project
		ProjectCreateRequest request2 = new ProjectCreateRequest();
		request2.setName(randomName());
		request2.setSchemaRef("folder");
		ProjectResponse response2 = call(() -> serverA.client().createProject(request2));

		// Now start the stopped instance again
		Thread.sleep(2000);
		MeshDockerServer serverB2 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false);
		serverB2.awaitStartup(20);
		serverB2.login();

		ProjectCreateRequest request3 = new ProjectCreateRequest();
		request3.setName(randomName());
		request3.setSchemaRef("folder");
		ProjectResponse response3 = call(() -> serverA.client().createProject(request3));

		// Both projects should be found
		call(() -> serverB2.client().findProjectByUuid(response.getUuid()));
		call(() -> serverA.client().findProjectByUuid(response3.getUuid()));
		call(() -> serverB2.client().findProjectByUuid(response3.getUuid()));
		call(() -> serverA.client().findProjectByUuid(response2.getUuid()));
		call(() -> serverB2.client().findProjectByUuid(response2.getUuid()));
	}

}
