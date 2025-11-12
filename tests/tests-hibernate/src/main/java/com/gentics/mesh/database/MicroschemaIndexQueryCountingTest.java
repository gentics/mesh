package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.SimpleDataHolderContext;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.search.index.microschema.MicroschemaIndexHandler;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT, elasticsearch = ElasticsearchTestMode.CONTAINER_ES8, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public class MicroschemaIndexQueryCountingTest extends AbstractMicroschemaQueryCountingTest {
	protected MicroschemaIndexHandler microschemaIndexHandler;

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
		microschemaIndexHandler = mesh().microschemaContainerIndexHandler();
	}

	@Test
	public void testSyncIndices() {
		doTest(tx -> microschemaIndexHandler.syncIndices(Optional.empty()).count().blockingGet(), 7, 1);
	}

	@Test
	public void testClearAndSyncIndices() {
		try {
			clearIndex();
		} catch (TimeoutException e) {
			fail("Index clear failed", e);
		}
		assertThat(microschemaIndexHandler.init().blockingAwait(1, TimeUnit.MINUTES)).as("Creating index completed within timeout").isTrue();
		doTest(tx -> microschemaIndexHandler.syncIndices(Optional.empty()).count().blockingGet(), 7, 1);
	}

	@Test
	public void testLoadAllElements() {
		Collection<? extends HibMicroschema> elements = doTest(tx -> microschemaIndexHandler.loadAllElements(ONLY_BUCKET, null), 1, 1);
		assertThat(elements).as("List of Elements").hasSize(totalNumMicroschemas);
	}

	@Test
	public void testLoadAndGenerateVersion() {
		DataHolderContext dhc = new SimpleDataHolderContext();
		List<String> versions = doTest(tx -> microschemaIndexHandler.loadAllElements(ONLY_BUCKET, dhc).stream()
				.map(e -> microschemaIndexHandler.generateVersion(e, dhc)).collect(Collectors.toList()), 3, 1);
		assertThat(versions).as("List of versions").hasSize(totalNumMicroschemas);
	}

	@Test
	public void testLoadAndToDocument() {
		DataHolderContext dhc = new SimpleDataHolderContext();
		List<JsonObject> documents = doTest(
				tx -> microschemaIndexHandler.loadAllElements(ONLY_BUCKET, dhc).stream()
						.map(e -> microschemaIndexHandler.getTransformer().toDocument(e, dhc)).collect(Collectors.toList()),
				5, 1);
		assertThat(documents).as("List of Documents").hasSize(totalNumMicroschemas);
	}
}
