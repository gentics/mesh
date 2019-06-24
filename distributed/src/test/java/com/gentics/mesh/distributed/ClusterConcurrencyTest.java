package com.gentics.mesh.distributed;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.util.TokenUtil.randomToken;
import static com.gentics.mesh.util.UUIDUtil.randomUUID;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.RuleChain;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.impl.LoggingConfigurator;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.distributed.containers.MeshDockerServer;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class ClusterConcurrencyTest extends AbstractClusterTest {

	private static final Logger log = LoggerFactory.getLogger(ClusterConcurrencyTest.class);

	private static final int TEST_DATA_SIZE = 100;

	private static String clusterPostFix = randomUUID();

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
	
//	public static MeshDockerServer serverD = new MeshDockerServer(vertx)
//		.withClusterName("dockerCluster" + clusterPostFix)
//		.withNodeName("nodeD")
//		.withDataPathPostfix(randomToken())
//		.withClearFolders();
	
//	public static MeshDockerServer serverE = new MeshDockerServer(vertx)
//		.withClusterName("dockerCluster" + clusterPostFix)
//		.withNodeName("nodeE")
//		.withDataPathPostfix(randomToken())
//		.withClearFolders();


	public static MeshRestClient clientA;
	public static MeshRestClient clientB;

	@ClassRule
	public static RuleChain chain = RuleChain.outerRule(serverC).around(serverB).around(serverA);

	@BeforeClass
	public static void waitForNodes() throws InterruptedException {
		LoggingConfigurator.init();
		serverB.awaitStartup(200);
		clientA = serverA.client();
		clientB = serverB.client();
	}

	@Before
	public void setupLogin() {
		clientA.setLogin("admin", "admin");
		clientA.login().blockingGet();
		clientB.setLogin("admin", "admin");
		clientB.login().blockingGet();
	}

	@Test
	public void testConcurrencyWithSchemaMigration() throws InterruptedException {
		SchemaListResponse schemas = call(() -> clientA.findSchemas());
		SchemaResponse contentSchema = schemas.getData().stream().filter(s -> s.getName().equals("content")).findFirst().get();
		String schemaUuid = contentSchema.getUuid();

		String projectName = randomName();
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(projectName);
		request.setSchemaRef("folder");
		ProjectResponse project = call(() -> clientA.createProject(request));

		call(() -> clientA.assignSchemaToProject(projectName, schemaUuid));

		// Create test data
		List<String> uuids = new ArrayList<>();
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some rorschach teaser"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
		for (int i = 0; i < TEST_DATA_SIZE; i++) {
			nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
			if (i % 10 == 0) {
				log.info("Creating node {" + i + "/" + TEST_DATA_SIZE + "}");
			}
			uuids.add(call(() -> clientA.createNode(projectName, nodeCreateRequest)).getUuid());
		}

		SchemaUpdateRequest schemaUpdateRequest = contentSchema.toUpdateRequest();
		schemaUpdateRequest.addField(FieldUtil.createStringFieldSchema("dummy"));
		Completable opA = clientA.updateSchema(contentSchema.getUuid(), schemaUpdateRequest).toCompletable();
		Completable opB = clientB.deleteNode(projectName, uuids.get(0)).toCompletable().delay(2000, TimeUnit.MILLISECONDS);

		Completable.merge(Arrays.asList(opA, opB)).blockingAwait();
		Thread.sleep(30000);

		// Finally assert that both nodes can still access the graph
		call(() -> clientA.findSchemaByUuid(contentSchema.getUuid()));
		call(() -> clientB.findSchemaByUuid(contentSchema.getUuid()));
	}

	@Test
	public void testConcurrencyViaUpdateOnNodeA() throws InterruptedException {
		SchemaListResponse schemas = call(() -> clientA.findSchemas());
		SchemaResponse contentSchema = schemas.getData().stream().filter(s -> s.getName().equals("content")).findFirst().get();
		String schemaUuid = contentSchema.getUuid();

		String projectName = randomName();
		// Node A: Create Project
		ProjectCreateRequest request = new ProjectCreateRequest();
		request.setName(projectName);
		request.setSchemaRef("folder");
		ProjectResponse project = call(() -> clientA.createProject(request));

		call(() -> clientA.assignSchemaToProject(projectName, schemaUuid));

		// Create test data
		List<String> uuids = new ArrayList<>();
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("some rorschach teaser"));
		nodeCreateRequest.getFields().put("content", FieldUtil.createStringField("Blessed mealtime again!"));
		nodeCreateRequest.setSchemaName("content");
		nodeCreateRequest.setParentNodeUuid(project.getRootNode().getUuid());
		for (int i = 0; i < TEST_DATA_SIZE; i++) {
			nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("new-page" + i + ".html"));
			if (i % 10 == 0) {
				log.info("Creating node {" + i + "/" + TEST_DATA_SIZE + "}");
			}
			uuids.add(call(() -> clientA.createNode(projectName, nodeCreateRequest)).getUuid());
		}

		AtomicLong longValue = new AtomicLong(0);
		Observable.fromIterable(uuids).flatMapCompletable(uuid -> {
			NodeUpdateRequest updateRequest = new NodeUpdateRequest();
			updateRequest.setLanguage("en");
			updateRequest.getFields().put("teaser", FieldUtil.createStringField("some rorschach teaser " + uuid + longValue.incrementAndGet()));
			updateRequest.setVersion("draft");
			return clientA.updateNode(projectName, uuid, updateRequest).toCompletable().repeat(20);
		}).blockingAwait();
		
		Thread.sleep(12000);

	}

	// -------

	// NodeA: Create nodes
	// NodeA: Publish nodes / Set permissions
	// NodeB: Modify many nodes

}
