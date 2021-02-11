package com.gentics.mesh.core.branch;

import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.branch.BranchCreateRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.DeploymentOptions;

@MeshTestSetting(testSize = FULL, startServer = true)
public class BranchMigrationEndpointTest extends AbstractMeshTest {

	@Before
	public void setupVerticleTest() throws Exception {
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx().deployVerticle(meshDagger().jobWorkerVerticle(), options);
		grantAdmin();
	}

	@Test
	public void testStartBranchMigration() throws Throwable {
		EventQueueBatch batch = createBatch();
		HibBranch newBranch;
		List<? extends HibNode> nodes;
		List<? extends HibNode> published;
		HibProject project = project();

		try (Tx tx = tx()) {
			assertThat(project.getInitialBranch().isMigrated()).as("Initial branch migration status").isEqualTo(true);
		}

		call(() -> client().takeNodeOffline(PROJECT_NAME, tx(() -> project().getBaseNode().getUuid()),
			new PublishParametersImpl().setRecursive(true)));

		published = Arrays.asList(folder("news"), folder("2015"), folder("2014"), folder("march"));
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			nodes = nodeDao.findAll(project).stream().filter(node -> nodeDao.getParentNode(node, project.getLatestBranch().getUuid()) != null)
				.collect(Collectors.toList());
			assertThat(nodes).as("Nodes list").isNotEmpty();
		}

		// publish some nodes
		published.forEach(node -> {
			call(() -> client().publishNode(PROJECT_NAME, tx(() -> node.getUuid())));
		});

		try (Tx tx = tx()) {
			newBranch = tx.branchDao().create(project, "newbranch", user(), batch);
			assertThat(newBranch.isMigrated()).as("Branch migration status").isEqualTo(false);
			tx.success();
		}
		nodes.forEach(node -> {
			Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT, ContainerType.PUBLISHED).forEach(type -> {
				assertThat(tx(() -> boot().contentDao().getGraphFieldContainers(node, newBranch, type).list()))
					.as(type + " Field Containers before Migration").isNotNull()
					.isEmpty();
			});
		});

		triggerAndWaitForJob(requestBranchMigration(newBranch));

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();

			assertThat(newBranch.isMigrated()).as("Branch migration status").isEqualTo(true);

			nodes.forEach(node -> {
				Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT).forEach(type -> {
					assertThat(boot().contentDao().getGraphFieldContainers(node, newBranch, type)).as(type + " Field Containers after Migration")
						.isNotNull()
						.isNotEmpty();
				});

				if (published.contains(node)) {
					assertThat(boot().contentDao().getGraphFieldContainers(node, newBranch, ContainerType.PUBLISHED))
						.as("Published field containers after migration")
						.isNotNull().isNotEmpty();
				} else {
					assertThat(boot().contentDao().getGraphFieldContainers(node, newBranch, ContainerType.PUBLISHED))
						.as("Published field containers after migration")
						.isNotNull().isEmpty();
				}

				HibNode initialParent = nodeDao.getParentNode(node, initialBranchUuid());
				if (initialParent == null) {
					assertThat(nodeDao.getParentNode(node, newBranch.getUuid())).as("Parent in new branch").isNull();
				} else {
					assertThat(nodeDao.getParentNode(node, newBranch.getUuid())).as("Parent in new branch").isNotNull()
						.isEqualToComparingOnlyGivenFields(initialParent, "uuid");
				}

				// TODO assert tags
			});
		}

	}

	@Test
	public void testStartForInitial() throws Throwable {
		try (Tx tx = tx()) {
			triggerAndWaitForJob(requestBranchMigration(initialBranch()), FAILED);
		}
	}

	@Test
	public void testStartAgain() throws Throwable {
		EventQueueBatch batch = createBatch();
		HibBranch newBranch = tx(tx -> {
			return tx.branchDao().create(project(), "newbranch", user(), batch);
		});
		String jobUuidA = requestBranchMigration(newBranch);
		triggerAndWaitForJob(jobUuidA, COMPLETED);

		// The second job should fail because the branch has already been migrated.
		String jobUuidB = requestBranchMigration(newBranch);
		JobListResponse response = triggerAndWaitForJob(jobUuidB, FAILED);
		List<JobStatus> status = response.getData().stream().map(e -> e.getStatus()).collect(Collectors.toList());
		assertThat(status).contains(COMPLETED, FAILED);

	}

	@Test
	public void testStartOrder() throws Throwable {
		HibProject project = project();
		EventQueueBatch batch = createBatch();
		HibBranch newBranch = tx(tx -> {
			return tx.branchDao().create(project, "newbranch", user(), batch);
		});
		HibBranch newestBranch = tx(tx -> {
			return tx.branchDao().create(project, "newestbranch", user(), batch);
		});

		try (Tx tx = tx()) {
			triggerAndWaitForJob(requestBranchMigration(newestBranch), FAILED);

			JobListResponse response = triggerAndWaitForJob(requestBranchMigration(newBranch), COMPLETED);
			List<JobStatus> status = response.getData().stream().map(e -> e.getStatus()).collect(Collectors.toList());
			assertThat(status).contains(FAILED, COMPLETED);

			response = triggerAndWaitForJob(requestBranchMigration(newestBranch), COMPLETED);
			status = response.getData().stream().map(e -> e.getStatus()).collect(Collectors.toList());
			assertThat(status).contains(FAILED, COMPLETED, COMPLETED);
		}
	}

	@Test
	public void testMigrateNodesWithoutSegment() {
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("dummyData");
		request.addField(FieldUtil.createStringFieldSchema("test"));
		SchemaResponse response = call(() -> client().createSchema(request));
		String schemaUuid = response.getUuid();

		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaUuid));

		for (int i = 0; i < 2; i++) {
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setSchemaName("dummyData");
			nodeCreateRequest.setLanguage("en");
			nodeCreateRequest.setParentNodeUuid(baseNodeUuid);

			NodeResponse node = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
			call(() -> client().publishNode(PROJECT_NAME, node.getUuid()));
		}

		grantAdmin();
		waitForJobs(() -> {
			call(() -> client().createBranch(PROJECT_NAME, new BranchCreateRequest().setName("branch1")));
		}, COMPLETED, 1);

	}

	@Test
	public void testBigData() throws Throwable {
		EventQueueBatch batch = createBatch();
		String baseNodeUuid = tx(() -> project().getBaseNode().getUuid());
		createNode(baseNodeUuid);

		HibBranch newBranch;
		try (Tx tx = tx()) {
			int numThreads = 1;
			int numFolders = 1000;

			ExecutorService service = Executors.newFixedThreadPool(numThreads);

			List<Future<Boolean>> futures = new ArrayList<>();

			for (int i = 0; i < numFolders; i++) {
				futures.add(service.submit(() -> {
					createNode(baseNodeUuid);
					return true;
				}));
			}

			for (Future<Boolean> future : futures) {
				future.get();
			}

			newBranch = tx.branchDao().create(project(), "newbranch", user(), batch);
			tx.success();
		}

		String jobUuid = requestBranchMigration(newBranch);
		triggerAndWaitForJob(jobUuid);
	}

	private void createNode(String baseNodeUuid) {
		NodeCreateRequest create = new NodeCreateRequest();
		create.setLanguage("en");
		create.setSchema(new SchemaReferenceImpl().setName("folder"));
		create.setParentNodeUuid(baseNodeUuid);
		call(() -> client().createNode(PROJECT_NAME, create));
	}

	/**
	 * Request branch migration and return the future
	 * 
	 * @param branch
	 * @return future
	 */
	protected String requestBranchMigration(HibBranch branch) {
		return tx(tx -> {
			return tx.jobDao().enqueueBranchMigration(user(), branch).getUuid();
		});
	}
}
