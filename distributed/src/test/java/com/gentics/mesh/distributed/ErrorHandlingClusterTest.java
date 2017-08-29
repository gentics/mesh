package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

public class ErrorHandlingClusterTest extends AbstractClusterTest {

	@ClassRule
	public static MeshDockerServer serverA = new MeshDockerServer("dockerCluster", "nodeA", true, true, true, vertx, 8000);

	public static MeshRestClient clientA;

	@BeforeClass
	public static void setupClient() {
		clientA = serverA.getMeshClient();
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
		ProjectResponse response = call(() -> clientA.createProject(request));

		MeshDockerServer serverB = addSlave("dockerCluster", "nodeB", true);
		//serverB.dropTraffic();
		call(() -> serverB.getMeshClient().findProjectByUuid(response.getUuid()));
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
		ProjectResponse response = call(() -> clientA.createProject(request));

		MeshDockerServer serverB1 = addSlave("dockerCluster", "nodeB", true);
		Thread.sleep(2000);
		serverB1.stop();

		// Node A: Create another project
		ProjectCreateRequest request2 = new ProjectCreateRequest();
		request2.setName(randomName());
		request2.setSchemaRef("folder");
		ProjectResponse response2 = call(() -> clientA.createProject(request2));

		// Now start the stopped instance again
		Thread.sleep(2000);
		MeshDockerServer serverB2 = addSlave("dockerCluster", "nodeB", false);

		ProjectCreateRequest request3 = new ProjectCreateRequest();
		request3.setName(randomName());
		request3.setSchemaRef("folder");
		ProjectResponse response3 = call(() -> clientA.createProject(request3));

		// Both projects should be found
		call(() -> serverB2.getMeshClient().findProjectByUuid(response.getUuid()));
		call(() -> clientA.findProjectByUuid(response3.getUuid()));
		call(() -> serverB2.getMeshClient().findProjectByUuid(response3.getUuid()));
		call(() -> clientA.findProjectByUuid(response2.getUuid()));
		call(() -> serverB2.getMeshClient().findProjectByUuid(response2.getUuid()));
	}

}
