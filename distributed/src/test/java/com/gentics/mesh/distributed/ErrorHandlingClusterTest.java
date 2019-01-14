package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.distributed.containers.MeshDockerServer;

/**
 * Tests various interacts with the cluster. (e.g.: Adding new nodes, Removing nodes)
 */
@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
public class ErrorHandlingClusterTest extends AbstractClusterTest {

	private static String clusterPostFix = randomUUID();

	@ClassRule
	public static MeshDockerServer serverA = new MeshDockerServer(vertx)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.waitForStartup()
		.withClearFolders();

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
		ProjectCreateRequest request = new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder");
		ProjectResponse response = call(() -> serverA.client().createProject(request));

		String dataPathPostfix = randomToken();
		MeshDockerServer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true);
		Thread.sleep(2000);
		serverB1.stop();

		// Node A: Create another project
		ProjectCreateRequest request2 = new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder");
		ProjectResponse response2 = call(() -> serverA.client().createProject(request2));

		// Now start the stopped instance again
		Thread.sleep(2000);
		MeshDockerServer serverB2 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false)
			.awaitStartup(20)
			.login();

		ProjectCreateRequest request3 = new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder");
		ProjectResponse response3 = call(() -> serverA.client().createProject(request3));

		List<MeshDockerServer> servers = Arrays.asList(serverA, serverB2);
		List<ProjectResponse> projects = Arrays.asList(response, response2, response3);
		// All projects should be found
		for (MeshDockerServer server : servers) {
			for (ProjectResponse project : projects) {
				call(() -> server.client().findProjectByUuid(project.getUuid()));
			}
		}
	}

	/**
	 * Start and stop the instances in the cluster alternately.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRestartingTwoSlaves() throws Exception {
		List<ProjectResponse> responses = new ArrayList<>();

		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest().setName("initial").setSchemaRef("folder");
		responses.add(call(() -> serverA.client().createProject(request)));
		System.out.println(call(() -> serverA.client().me()).toJson());

		String dataPathPostfix = randomToken();
		MeshDockerServer serverB = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true)
			.awaitStartup(20).login();

		// Stop and restart each of the nodes alternatively and create projects in between each phase of the start/stop actions.
		for (int i = 0; i < 2; i++) {
			boolean handleFirst = i % 2 == 0;
			MeshDockerServer server = handleFirst ? serverA : serverB;
			MeshDockerServer otherServer = handleFirst ? serverB : serverA;
			System.out.println("Run {" + i + "} handling node {" + server.getNodeName() + "}");

			// Before stop
			ProjectCreateRequest request2 = new ProjectCreateRequest().setName(server.getNodeName() + "A" + i).setSchemaRef("folder");
			responses.add(call(() -> server.client().createProject(request2)));

			Thread.sleep(10_000);
			System.out.println("Stopping {" + server.getNodeName() + "}");
			server.stop();

			// After stop
			ProjectCreateRequest request3 = new ProjectCreateRequest().setName(otherServer.getNodeName() + "B" + i).setSchemaRef("folder");
			responses.add(call(() -> otherServer.client().createProject(request3)));

			// Now start the server again
			Thread.sleep(10_000);
			System.out.println("Starting server {" + server.getNodeName() + "}");
			MeshDockerServer serverAfterRestart = addSlave("dockerCluster" + clusterPostFix, server.getNodeName(), server.getDataPathPostfix(), false)
				.awaitStartup(30)
				.login();
			if (handleFirst) {
				serverA = serverAfterRestart;
			} else {
				serverB = serverAfterRestart;
			}

			// After restart
			ProjectCreateRequest request4 = new ProjectCreateRequest().setName(server.getNodeName() + "C" + i).setSchemaRef("folder");
			responses.add(call(() -> serverAfterRestart.client().createProject(request4)));

			// Check that all projects can be found across all nodes.
			Thread.sleep(10_000);
			List<MeshDockerServer> servers = Arrays.asList(serverA, serverB);
			for (MeshDockerServer currentServer : servers) {
				for (ProjectResponse project : responses) {
					try {
						call(() -> currentServer.client().findProjectByUuid(project.getUuid()));
					} catch (AssertionError e) {
						e.printStackTrace();
						fail("Could not load project {" + project.getName() + "} from server {" + currentServer.getNodeName() + "}");
					}
				}
			}
		}

	}

	/**
	 * Same as {@link #testRestartingSlave()} but with multiple slaves being stopped and started.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRestartingMultipleSlaves() throws Exception {
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest().setName("onNodeA").setSchemaRef("folder");
		ProjectResponse response = call(() -> serverA.client().createProject(request));

		// Start slave NodeB
		String dataPathPostfix = randomToken();
		MeshDockerServer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true)
			.awaitStartup(20);

		// Start slave NodeC
		MeshDockerServer serverC1 = addSlave("dockerCluster" + clusterPostFix, "nodeC", dataPathPostfix, true)
			.awaitStartup(20);

		// Now stop NodeB and a bit later NodeC
		serverB1.stop();
		Thread.sleep(14000);
		serverC1.stop();

		// Node A: Create another project
		ProjectCreateRequest request2 = new ProjectCreateRequest().setName("onNodeA2").setSchemaRef("folder");
		ProjectResponse response2 = call(() -> serverA.client().createProject(request2));

		// Now start the stopped NodeC again
		Thread.sleep(2000);
		MeshDockerServer serverC2 = addSlave("dockerCluster" + clusterPostFix, "nodeC", dataPathPostfix, false).awaitStartup(30).login();
		ProjectCreateRequest request3 = new ProjectCreateRequest().setName("onNodeC").setSchemaRef("folder");
		ProjectResponse response3 = call(() -> serverC2.client().createProject(request3));

		Thread.sleep(2000);
		MeshDockerServer serverB2 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false).awaitStartup(30).login();
		ProjectCreateRequest request4 = new ProjectCreateRequest().setName("onNodeB").setSchemaRef("folder");
		ProjectResponse response4 = call(() -> serverB2.client().createProject(request4));

		Thread.sleep(1000);
		List<MeshDockerServer> servers = Arrays.asList(serverA, serverB2, serverC2);
		List<ProjectResponse> projects = Arrays.asList(response, response2, response3, response4);

		// All projects should be found
		for (MeshDockerServer server : servers) {
			for (ProjectResponse project : projects) {
				try {
					call(() -> server.client().findProjectByUuid(project.getUuid()));
				} catch (AssertionError e) {
					e.printStackTrace();
					fail("Could not load project {" + project.getName() + "} from server {" + server.getNodeName() + "}");
				}
			}
		}
	}

}
