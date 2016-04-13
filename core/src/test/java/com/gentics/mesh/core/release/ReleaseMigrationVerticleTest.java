package com.gentics.mesh.core.release;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.beans.factory.annotation.Autowired;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.core.verticle.release.ReleaseVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

public class ReleaseMigrationVerticleTest extends AbstractRestVerticleTest {
	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Autowired
	private ReleaseVerticle releaseVerticle;

	@Autowired
	private EventbusVerticle eventbusVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(eventbusVerticle);
		list.add(releaseVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Override
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx.deployVerticle(nodeMigrationVerticle, options);
	}

//	@Test
//	public void testStartReleaseMigration() throws Throwable {
//		Project project = project();
//		List<? extends Node> published = Arrays.asList(folder("news"), folder("2015"), folder("2014"), folder("march"));
//		List<? extends Node> nodes = project.getNodeRoot().findAll().stream()
//				.filter(node -> node.getParentNode(project.getLatestRelease().getUuid()) != null)
//				.collect(Collectors.toList());
//
//		assertThat(nodes).as("Nodes list").isNotEmpty();
//
//		// publish some nodes
//		published.forEach(node -> {
//			call(() -> getClient().publishNode(project.getName(), node.getUuid()));
//		});
//
//		Release newRelease = project.getReleaseRoot().create("newrelease", user());
//		nodes.forEach(node -> {
//			Arrays.asList(Type.INITIAL, Type.DRAFT, Type.PUBLISHED).forEach(type -> {
//				assertThat(node.getGraphFieldContainers(newRelease, type))
//						.as(type + " Field Containers before Migration").isNotNull().isEmpty();
//			});
//		});
//
//		DeliveryOptions options = new DeliveryOptions();
//
//		options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project.getUuid());
//		options.addHeader(NodeMigrationVerticle.UUID_HEADER, newRelease.getUuid());
//		CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
//		vertx.eventBus().send(NodeMigrationVerticle.RELEASE_MIGRATION_ADDRESS, null, options, (rh) -> {
//			future.complete(rh);
//		});
//
//		AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
//		if (result.cause() != null) {
//			throw result.cause();
//		}
//
//		nodes.forEach(node -> {
//			node.reload();
//			Arrays.asList(Type.INITIAL, Type.DRAFT).forEach(type -> {
//				assertThat(node.getGraphFieldContainers(newRelease, type))
//						.as(type + " Field Containers after Migration").isNotNull().isNotEmpty();
//			});
//
//			if (published.contains(node)) {
//				assertThat(node.getGraphFieldContainers(newRelease, Type.PUBLISHED))
//						.as("Published field containers after migration").isNotNull().isNotEmpty();
//			} else {
//				assertThat(node.getGraphFieldContainers(newRelease, Type.PUBLISHED))
//						.as("Published field containers after migration").isNotNull().isEmpty();
//			}
//		});
//	}

	@Test
	public void testBigData() throws Throwable {
		int numThreads = 1;
		int numFolders = 1000;
		Project project = project();
		String projectName = project.getName();
		String baseNodeUuid = project.getBaseNode().getUuid();

		MetricRegistry metrics = new MetricRegistry();
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(1, TimeUnit.SECONDS);

		Meter createdNode = metrics.meter("Create Node");
		Timer migrationTimer = metrics.timer("Migration");

		ExecutorService service = Executors.newFixedThreadPool(numThreads);

		List<Future<Boolean>> futures = new ArrayList<>();

		for (int i = 0; i < numFolders; i++) {
			futures.add(service.submit(() -> {
				NodeCreateRequest create = new NodeCreateRequest();
				create.setLanguage("en");
				create.setSchema(new SchemaReference().setName("folder"));
				create.setParentNodeUuid(baseNodeUuid);
				call(() -> getClient().createNode(projectName, create));
				createdNode.mark();
				return true;
			}));
		}

		for (Future<Boolean> future : futures) {
			future.get();
		}

//		Release newRelease = project.getReleaseRoot().create("newrelease", user());
//		DeliveryOptions options = new DeliveryOptions();
//		options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project.getUuid());
//		options.addHeader(NodeMigrationVerticle.UUID_HEADER, newRelease.getUuid());
//		CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
//		vertx.eventBus().send(NodeMigrationVerticle.RELEASE_MIGRATION_ADDRESS, null, options, (rh) -> {
//			future.complete(rh);
//		});
//
//		try (Timer.Context ctx = migrationTimer.time()) {
//			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.MINUTES);
//			if (result.cause() != null) {
//				throw result.cause();
//			}
//		}
	}
}
