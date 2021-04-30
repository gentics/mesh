package com.gentics.mesh.distributed;

import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.docker.MeshContainer;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Tests for cluster nodes reliability under utter circumstances.
 * The cluster tests cannot be merged into a single unit test file because of the memory leaks happening on the in-memory DB mode.
 * 
 * @author plyhun
 *
 */
public abstract class AbstractClusterTortureTest extends AbstractClusterTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractClusterTortureTest.class);

	private static final int TEST_DATA_SIZE = 1000;

	private final int NUM_PROJECTS = 80;
	
	protected static String clusterPostFix = randomUUID();
	
	protected void torture(Torture torture) throws Exception {
		MeshContainer serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
				.withClusterName("dockerCluster" + clusterPostFix)
				.withNodeName("nodeA")
				.withDataPathPostfix(randomToken())
				.withInitCluster()
				.withWriteQuorum(1)
				.waitForStartup()
				.withFilesystem()
				.withClearFolders();
		serverA.start();
		serverA.login();
		
		// Find content schema
		SchemaListResponse schemas = serverA.client().findSchemas().blockingGet();
		SchemaResponse contentSchema = schemas.getData().stream().filter(s -> s.getName().equals("content")).findFirst().get();
		String schemaUuid = contentSchema.getUuid();
		
		Map<ProjectResponse, List<NodeResponse>> projects = new HashMap<>();
		
		for (int i = 0; i < NUM_PROJECTS; i++) {
			String newProjectName = randomName();
			// Node A: Create Project
			ProjectCreateRequest request = new ProjectCreateRequest();
			request.setName(newProjectName);
			request.setSchemaRef("folder");
			ProjectResponse project = serverA.client().createProject(request).blockingGet();		
			
			// Assign content schema
			serverA.client().assignSchemaToProject(project.getName(), schemaUuid).blockingGet();
			
			projects.put(project, new ArrayList<NodeResponse>());
		}
		
		String parentUuid = null;
		Map.Entry<ProjectResponse, List<NodeResponse>> currentProject = null;
		
		for (int i = 0; i < TEST_DATA_SIZE; i++) {
			if (currentProject == null) {
				currentProject = projects.entrySet().stream().skip(new Random().nextInt(NUM_PROJECTS)).findFirst().get();
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
				log.info("Creating node {" + i + "/" + TEST_DATA_SIZE + "} on " + projectName);
			} else {
				nodeCreateRequest.setSchemaName("folder");
				nodeCreateRequest.getFields().put("name", FieldUtil.createStringField("folder-" + i));
				log.info("Creating folder {" + i + "/" + TEST_DATA_SIZE + "} on " + projectName);
			}	
			
			NodeResponse newNode = serverA.client().createNode(projectName, nodeCreateRequest).blockingGet();
			
			currentProject.getValue().add(newNode);
			
			if ((rnd % 2) == 1) {
				currentProject = null;
			} else if ((rnd % 3) == 1) {
				parentUuid = newNode.getUuid();
			}
		}
		
		// Create secondary node, sync it, and stop.
		String dataPathPostfix = randomToken();
		MeshContainer serverB1 = addSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, true, 1);
		Thread.sleep(2000);
		
		// Torture
		torture.torture(serverA, serverB1, contentSchema);
		Thread.sleep(2000);

		boolean serverArestarted = false;
		boolean serverB1restarted = false;

		// Start the secondary node, check for start errors.
		if (!serverB1.isRunning()) {
			serverB1 = prepareSlave("dockerCluster" + clusterPostFix, "nodeB", dataPathPostfix, false, false, 1);
			serverB1.start();
			serverB1restarted = true;
		}

		// Start the primary node, check for start errors.
		if (!serverA.isRunning()) {
			serverA = new MeshContainer(MeshContainer.LOCAL_PROVIDER)
					.withDataPathPostfix(serverA.getDataPathPostfix())
					.withClusterName("dockerCluster" + clusterPostFix)
					.withNodeName("nodeA")
					.withFilesystem()
					.withWriteQuorum(1);			
			serverA.start();
			serverArestarted = true;
		}
		
		Thread.sleep(5000);

		if (serverB1restarted) {
			serverB1.awaitStartup(300);
			serverB1.login();
		}
		if (serverArestarted) {
			serverA.awaitStartup(300);
			serverA.login();
		}

		// Read all the data from the secondary node.
		for (Entry<ProjectResponse, List<NodeResponse>> entry: projects.entrySet()) {
			// Read project
			serverA.client().findProjectByUuid(entry.getKey().getUuid()).blockingGet();
			serverB1.client().findProjectByUuid(entry.getKey().getUuid()).blockingGet();
			
			// Read project data
			for (NodeResponse node: entry.getValue()) {
				serverA.client().findNodeByUuid(entry.getKey().getName(), node.getUuid()).blockingGet();
				serverB1.client().findNodeByUuid(entry.getKey().getName(), node.getUuid()).blockingGet();
			}
		}
		
		serverB1.close();
		serverA.close();
	}
	
	@FunctionalInterface
	public static interface Torture {
		public void torture(MeshContainer primaryNode, MeshContainer secondaryNode, SchemaResponse contentSchema) throws Exception;
	}
}
