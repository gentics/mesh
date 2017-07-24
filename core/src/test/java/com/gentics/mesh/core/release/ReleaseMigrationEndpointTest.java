package com.gentics.mesh.core.release;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.gentics.ferma.Tx;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class ReleaseMigrationEndpointTest extends AbstractMeshTest {

	@Before
	public void setupVerticleTest() throws Exception {
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx().deployVerticle(meshDagger().nodeMigrationVerticle(), options);
	}

	@Test
	public void testStartReleaseMigration() throws Throwable {
		Release newRelease;
		List<? extends Node> nodes;
		List<? extends Node> published;

		try (Tx tx = tx()) {
			Project project = project();
			assertThat(project.getInitialRelease().isMigrated()).as("Initial release migration status").isEqualTo(true);

			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			published = Arrays.asList(folder("news"), folder("2015"), folder("2014"), folder("march"));
			nodes = project.getNodeRoot().findAll().stream().filter(node -> node.getParentNode(project.getLatestRelease().getUuid()) != null)
					.collect(Collectors.toList());

			assertThat(nodes).as("Nodes list").isNotEmpty();

			// publish some nodes
			published.forEach(node -> {
				call(() -> client().publishNode(project.getName(), node.getUuid()));
			});

			newRelease = project.getReleaseRoot().create("newrelease", user());
			assertThat(newRelease.isMigrated()).as("Release migration status").isEqualTo(false);
			tx.success();
		}
		try (Tx tx = tx()) {

			nodes.forEach(node -> {
				Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT, ContainerType.PUBLISHED).forEach(type -> {
					assertThat(node.getGraphFieldContainers(newRelease, type)).as(type + " Field Containers before Migration").isNotNull().isEmpty();
				});
			});

			CompletableFuture<AsyncResult<Message<Object>>> future = requestReleaseMigration(projectUuid(), newRelease.getUuid());

			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			if (result.cause() != null) {
				throw result.cause();
			}

			newRelease.reload();
			assertThat(newRelease.isMigrated()).as("Release migration status").isEqualTo(true);

			nodes.forEach(node -> {
				node.reload();
				Arrays.asList(ContainerType.INITIAL, ContainerType.DRAFT).forEach(type -> {
					assertThat(node.getGraphFieldContainers(newRelease, type)).as(type + " Field Containers after Migration").isNotNull()
							.isNotEmpty();
				});

				if (published.contains(node)) {
					assertThat(node.getGraphFieldContainers(newRelease, ContainerType.PUBLISHED)).as("Published field containers after migration")
							.isNotNull().isNotEmpty();
				} else {
					assertThat(node.getGraphFieldContainers(newRelease, ContainerType.PUBLISHED)).as("Published field containers after migration")
							.isNotNull().isEmpty();
				}

				Node initialParent = node.getParentNode(initialReleaseUuid());
				if (initialParent == null) {
					assertThat(node.getParentNode(newRelease.getUuid())).as("Parent in new release").isNull();
				} else {
					assertThat(node.getParentNode(newRelease.getUuid())).as("Parent in new release").isNotNull()
							.isEqualToComparingOnlyGivenFields(initialParent, "uuid");
				}

				// TODO assert tags
			});
		}

	}

	@Test
	public void testStartForInitial() throws Throwable {
		try (Tx tx = tx()) {
			CompletableFuture<AsyncResult<Message<Object>>> future = requestReleaseMigration(projectUuid(), initialReleaseUuid());
			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			assertThat(result.failed()).isTrue();
		}
	}

	@Test
	public void testStartAgain() throws Throwable {
		Release newRelease;
		try (Tx tx = tx()) {
			newRelease = project().getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			CompletableFuture<AsyncResult<Message<Object>>> future = requestReleaseMigration(projectUuid(), newRelease.getUuid());
			AsyncResult<Message<Object>> result = future.get(20, TimeUnit.SECONDS);
			assertTrue("The migration did run into a timeout after 20 seconds", result.succeeded());

			future = requestReleaseMigration(projectUuid(), newRelease.getUuid());
			result = future.get(10, TimeUnit.SECONDS);
			assertThat(result.failed()).isTrue();
		}

	}

	@Test
	public void testStartOrder() throws Throwable {

		Release newRelease;
		Release newestRelease;
		try (Tx tx = tx()) {
			Project project = project();
			newRelease = project.getReleaseRoot().create("newrelease", user());
			newestRelease = project.getReleaseRoot().create("newestrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			CompletableFuture<AsyncResult<Message<Object>>> future = requestReleaseMigration(projectUuid(), newestRelease.getUuid());
			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			assertThat(result.failed()).isTrue();

			future = requestReleaseMigration(projectUuid(), newRelease.getUuid());
			result = future.get(10, TimeUnit.SECONDS);
			assertThat(result.succeeded()).isTrue();

			future = requestReleaseMigration(projectUuid(), newestRelease.getUuid());
			result = future.get(10, TimeUnit.SECONDS);
			assertThat(result.succeeded()).isTrue();
		}
	}

	@Test
	public void testBigData() throws Throwable {

		MetricRegistry metrics = new MetricRegistry();
		Meter createdNode = metrics.meter("Create Node");
		Timer migrationTimer = metrics.timer("Migration");
		Release newRelease;
		try (Tx tx = tx()) {
			int numThreads = 1;
			int numFolders = 1000;
			Project project = project();
			String projectName = project.getName();
			String baseNodeUuid = project.getBaseNode().getUuid();

			ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS)
					.build();
			reporter.start(1, TimeUnit.SECONDS);

			ExecutorService service = Executors.newFixedThreadPool(numThreads);

			List<Future<Boolean>> futures = new ArrayList<>();

			for (int i = 0; i < numFolders; i++) {
				futures.add(service.submit(() -> {
					NodeCreateRequest create = new NodeCreateRequest();
					create.setLanguage("en");
					create.setSchema(new SchemaReference().setName("folder"));
					create.setParentNodeUuid(baseNodeUuid);
					call(() -> client().createNode(projectName, create));
					createdNode.mark();
					return true;
				}));
			}

			for (Future<Boolean> future : futures) {
				future.get();
			}

			newRelease = project.getReleaseRoot().create("newrelease", user());
			tx.success();
		}

		try (Tx tx = tx()) {
			CompletableFuture<AsyncResult<Message<Object>>> future = requestReleaseMigration(projectUuid(), newRelease.getUuid());

			try (Timer.Context ctx = migrationTimer.time()) {
				AsyncResult<Message<Object>> result = future.get(10, TimeUnit.MINUTES);
				if (result.cause() != null) {
					throw result.cause();
				}
			}
		}
	}

	/**
	 * Request release migration and return the future
	 * 
	 * @param projectUuid
	 *            project Uuid
	 * @param releaseUuid
	 *            release Uuid
	 * @return future
	 */
	protected CompletableFuture<AsyncResult<Message<Object>>> requestReleaseMigration(String projectUuid, String releaseUuid) {
		DeliveryOptions options = new DeliveryOptions();
		options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, projectUuid);
		options.addHeader(NodeMigrationVerticle.UUID_HEADER, releaseUuid);
		CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
		vertx().eventBus().send(NodeMigrationVerticle.RELEASE_MIGRATION_ADDRESS, null, options, (rh) -> {
			future.complete(rh);
		});

		return future;
	}
}
