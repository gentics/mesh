package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.test.docker.MeshContainer;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Tests various interacts with the cluster. (e.g.: Adding new nodes, Removing nodes)
 */
public class ErrorHandlingClusterTest extends AbstractClusterTest {

	private static final Logger log = LoggerFactory.getLogger(ClusterConcurrencyTest.class);

	private static final int TEST_DATA_SIZE = 30000;

	private static String clusterPostFix = randomUUID();

	@ClassRule
	public static MeshContainer serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
		.withClusterName("dockerCluster" + clusterPostFix)
		.withNodeName("nodeA")
		.withDataPathPostfix(randomToken())
		.withInitCluster()
		.withWriteQuorum(1)
		.waitForStartup()
		.withFilesystem()
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
	public void testProjectInitForNode() throws Exception {
		String newProjectName = randomName();
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(newProjectName);
		request.setSchemaRef("folder");
		ProjectResponse response = call(() -> serverA.client().createProject(request));

		MeshContainer serverB = addSlave("dockerCluster" + clusterPostFix, "nodeB", randomToken(), true, 1);
		serverB.login();
		// serverB.dropTraffic();
		call(() -> serverB.client().findProjectByUuid(response.getUuid()));
	}
	
	@Test
	public void testSecondaryKilledDuringMigration() throws Exception {
		final int numProjects = 80;
		
		SchemaListResponse schemas = call(() -> serverA.client().findSchemas());
		SchemaResponse contentSchema = schemas.getData().stream().filter(s -> s.getName().equals("content")).findFirst().get();
		String schemaUuid = contentSchema.getUuid();
		
		log.info("Created at localhost:" + serverA.getPort());

		Map<ProjectResponse, List<NodeResponse>> projects = IntStream.range(0, numProjects).mapToObj(unused -> {
			String newProjectName = randomName();
			// Node A: Create Project
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(newProjectName);
			request.setSchemaRef("folder");
			ProjectResponse project = call(() -> serverA.client().createProject(request));		
			
			call(() -> serverA.client().assignSchemaToProject(project.getName(), schemaUuid));
			
			return project;
		}).collect(Collectors.toMap(p -> p, v -> new ArrayList<>()));
		
		String parentUuid = null;
		Map.Entry<ProjectResponse, List<NodeResponse>> currentProject = null;
		
		for (int i = 0; i < TEST_DATA_SIZE; i++) {
			if (currentProject == null) {
				currentProject = projects.entrySet().stream().skip(new Random().nextInt(numProjects)).findFirst().get();
				parentUuid = null;
			}
			if (parentUuid == null) {
				parentUuid = currentProject.getKey().getRootNode().getUuid();
			}
			String projectName = currentProject.getKey().getName();
			
			int rnd = new Random().nextInt();			
			
			// Create test data
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.setParentNodeUuid(parentUuid);
			
			if ((rnd % 3) == 2) {
				nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some rorschach teaser"));
				nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
				nodeCreateRequest.setSchemaName("content");
				nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("page-" + i + ".html"));
				log.info("Creating node {" + i + "/" + TEST_DATA_SIZE + "}");
			} else {
				nodeCreateRequest.setSchemaName("folder");
				nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("folder-" + i));
			}	
			
			NodeResponse newNode = call(() -> serverA.client().createNode(projectName, nodeCreateRequest));
			
			currentProject.getValue().add(newNode);
			
			if ((rnd % 2) == 1) {
				currentProject = null;
			} else if ((rnd % 3) == 1) {
				parentUuid = newNode.getUuid();
			}
		}
		
		String dataPathPostfix = randomToken();
		{
			MeshContainer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true, 1);
			Thread.sleep(2000);
			serverB1.stop();
		}
		
		{
			MeshContainer serverB1 = prepareSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false, false, 1);
			serverB1.start();
			Thread.sleep(2000);
			
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					SchemaUpdateRequest schemaUpdateRequest = contentSchema.toUpdateRequest();
					schemaUpdateRequest.removeField("teaser");
					schemaUpdateRequest.addField(new DateFieldSchemaImpl().setName("teaser"), "content");
					
					call(() -> serverA.client().updateSchema(schemaUuid, schemaUpdateRequest));
				}
			}).run();
			
			serverB1.killHardContainer();
		}
		Thread.sleep(2000);
		
		{
			MeshContainer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false, 1);
			projects.entrySet().stream().forEach(entry -> {
				call(() -> serverB1.client().findProjectByUuid(entry.getKey().getUuid()));
				
				entry.getValue().stream().forEach(node -> {
					call(() -> serverB1.client().findNodeByUuid(entry.getKey().getName(), node.getUuid()));
				});
			});
		}
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
		MeshContainer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true, 1);
		Thread.sleep(2000);
		serverB1.stop();

		// Node A: Create another project
		ProjectCreateRequest request2 = new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder");
		ProjectResponse response2 = call(() -> serverA.client().createProject(request2));

		// Now start the stopped instance again
		Thread.sleep(2000);
		MeshContainer serverB2 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false, 1)
			.login();

		ProjectCreateRequest request3 = new ProjectCreateRequest().setName(randomName()).setSchemaRef("folder");
		ProjectResponse response3 = call(() -> serverA.client().createProject(request3));

		List<MeshContainer> servers = Arrays.asList(serverA, serverB2);
		List<ProjectResponse> projects = Arrays.asList(response, response2, response3);
		// All projects should be found
		for (MeshContainer server : servers) {
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
		MeshContainer serverB = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true, 1)
			.login();

		// Stop and restart each of the nodes alternatively and create projects in between each phase of the start/stop actions.
		for (int i = 0; i < 2; i++) {
			boolean handleFirst = i % 2 == 0;
			MeshContainer server = handleFirst ? serverA : serverB;
			MeshContainer otherServer = handleFirst ? serverB : serverA;
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
			MeshContainer serverAfterRestart = addSlave("dockerCluster" + clusterPostFix, server.getNodeName(), server.getDataPathPostfix(), false,
				1)
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
			List<MeshContainer> servers = Arrays.asList(serverA, serverB);
			for (MeshContainer currentServer : servers) {
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
		MeshContainer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true, 1);

		// Start slave NodeC
		MeshContainer serverC1 = addSlave("dockerCluster" + clusterPostFix, "nodeC", dataPathPostfix, true, 1);

		// Now stop NodeB and a bit later NodeC
		serverB1.stop();
		Thread.sleep(14000);
		serverC1.stop();

		// Node A: Create another project
		ProjectCreateRequest request2 = new ProjectCreateRequest().setName("onNodeA2").setSchemaRef("folder");
		ProjectResponse response2 = call(() -> serverA.client().createProject(request2));

		// Now start the stopped NodeC again
		Thread.sleep(2000);
		MeshContainer serverC2 = addSlave("dockerCluster" + clusterPostFix, "nodeC", dataPathPostfix, false, 1).login();
		ProjectCreateRequest request3 = new ProjectCreateRequest().setName("onNodeC").setSchemaRef("folder");
		ProjectResponse response3 = call(() -> serverC2.client().createProject(request3));

		Thread.sleep(2000);
		MeshContainer serverB2 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false, 1).login();
		ProjectCreateRequest request4 = new ProjectCreateRequest().setName("onNodeB").setSchemaRef("folder");
		ProjectResponse response4 = call(() -> serverB2.client().createProject(request4));

		Thread.sleep(1000);
		List<MeshContainer> servers = Arrays.asList(serverA, serverB2, serverC2);
		List<ProjectResponse> projects = Arrays.asList(response, response2, response3, response4);

		// All projects should be found
		for (MeshContainer server : servers) {
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
