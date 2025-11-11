package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Strings;
import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.SimpleDataHolderContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.search.index.node.NodeIndexHandlerImpl;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT, elasticsearch = ElasticsearchTestMode.CONTAINER_ES8, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public class NodeIndexQueryCountingTest extends AbstractNodeQueryCountingTest {
	protected NodeIndexHandlerImpl nodeIndexHandler;

	@Override
	@Before
	public void setup() {
		super.setup();
		try {
			syncIndex();
		} catch (TimeoutException e) {
			fail("Index sync failed", e);
		}
		waitForSearchIdleEvent();
		nodeIndexHandler = (NodeIndexHandlerImpl)mesh().nodeContainerIndexHandler();
	}

	@Test
	public void testSyncIndices() {
		doTest(tx -> nodeIndexHandler.syncIndices(Optional.empty()).count().blockingGet(), 50, 1);
	}

	@Test
	public void testClearAndSyncIndices() {
		try {
			clearIndex();
		} catch (TimeoutException e) {
			fail("Index clear failed", e);
		}
		assertThat(nodeIndexHandler.init().blockingAwait(1, TimeUnit.MINUTES)).as("Creating index completed within timeout").isTrue();
		doTest(tx -> nodeIndexHandler.syncIndices(Optional.empty()).count().blockingGet(), 50, 1);
	}

	@Test
	public void testLoadAllElements() {
		List<? extends HibNodeFieldContainer> elements = doTest(tx -> {
			ProjectDao projectDao = tx.projectDao();
			SchemaDao schemaDao = tx.schemaDao();

			HibProject project = projectDao.findByName(PROJECT_NAME);
			HibBranch branch = project.getLatestBranch();
			HibSchemaVersion schemaVersion = schemaDao.findActiveSchemaVersions(branch).stream()
					.filter(v -> Strings.CI.equals(v.getName(), "folder")).findFirst().orElseThrow();

			return nodeIndexHandler.loadAllElements(branch, schemaVersion, ContainerType.DRAFT, ONLY_BUCKET, null);
		}, 5, 1);
		assertThat(elements).as("List of Elements").hasSize(NUM_NODES + 1);
	}

	@Test
	public void testLoadAndGenerateVersion() {
		DataHolderContext dhc = new SimpleDataHolderContext();

		List<String> versions = doTest(tx -> {
			ProjectDao projectDao = tx.projectDao();
			SchemaDao schemaDao = tx.schemaDao();

			HibProject project = projectDao.findByName(PROJECT_NAME);
			HibBranch branch = project.getLatestBranch();
			HibSchemaVersion schemaVersion = schemaDao.findActiveSchemaVersions(branch).stream()
					.filter(v -> Strings.CI.equals(v.getName(), "folder")).findFirst().orElseThrow();

			return nodeIndexHandler.loadAllElements(branch, schemaVersion, ContainerType.DRAFT, ONLY_BUCKET, dhc)
					.stream().map(e -> nodeIndexHandler.generateVersion(e, project, branch, ContainerType.DRAFT, dhc))
					.collect(Collectors.toList());
		}, 10, 1);
		assertThat(versions).as("List of versions").hasSize(NUM_NODES + 1);
	}

	@Test
	public void testLoadAndToDocument() {
		DataHolderContext dhc = new SimpleDataHolderContext();

		List<JsonObject> documents =  doTest(tx -> {
			ProjectDao projectDao = tx.projectDao();
			SchemaDao schemaDao = tx.schemaDao();

			HibProject project = projectDao.findByName(PROJECT_NAME);
			HibBranch branch = project.getLatestBranch();
			HibSchemaVersion schemaVersion = schemaDao.findActiveSchemaVersions(branch).stream()
					.filter(v -> Strings.CI.equals(v.getName(), "folder")).findFirst().orElseThrow();

			return nodeIndexHandler.loadAllElements(branch, schemaVersion, ContainerType.DRAFT, ONLY_BUCKET, dhc).stream()
					.map(e -> nodeIndexHandler.getTransformer().toDocument(e, project, branch, ContainerType.DRAFT, dhc))
					.collect(Collectors.toList());
		}, 12, 1);
		assertThat(documents).as("List of Documents").hasSize(NUM_NODES + 1);
	}
}
