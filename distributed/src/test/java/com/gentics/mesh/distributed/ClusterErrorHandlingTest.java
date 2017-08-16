package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

public class ClusterErrorHandlingTest extends AbstractClusterTest {

	@ClassRule
	public static MeshDockerServer serverA = new MeshDockerServer("dockerCluster", "nodeA", true, true, vertx, 8000);

	public static MeshRestClient clientA;

	@BeforeClass
	public static void setupClient() {
		clientA = serverA.getMeshClient();
	}

	/**
	 * Add a new node to the cluster and check that previously setup projects are setup correctly.
	 * @throws InterruptedException 
	 * @throws IOException 
	 * @throws UnsupportedOperationException 
	 */
	@Test
	public void testProjectInitForNode() throws UnsupportedOperationException, IOException, InterruptedException {
		String newProjectName = randomName();
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> clientA.createProject(request));

		MeshDockerServer serverB = addSlave("dockerCluster", "nodeB");
		serverB.dropTraffic();
		call(() -> serverB.getMeshClient().findProjectByUuid(response.getUuid()));
	}

}
