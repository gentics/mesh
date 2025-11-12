package com.gentics.mesh.database;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.context.SimpleDataHolderContext;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.hibernate.util.QueryCounter;
import com.gentics.mesh.search.index.user.UserIndexHandler;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.eventhandler.MainEventHandler;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.ResetTestDb;
import com.gentics.mesh.test.TestSize;

import io.reactivex.functions.Consumer;
import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.PROJECT, elasticsearch = ElasticsearchTestMode.CONTAINER_ES8, monitoring = true, startServer = true, customOptionChanger = QueryCounter.EnableHibernateStatistics.class, resetBetweenTests = ResetTestDb.NEVER)
public class UserIndexQueryCountingTest extends AbstractUserQueryCountingTest {
	protected UserIndexHandler userIndexHandler;

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
		userIndexHandler = mesh().userIndexHandler();
	}

	@Test
	public void testSyncIndices() {
		doTest(tx -> userIndexHandler.syncIndices(Optional.empty()).count().blockingGet(), 7, 1);
	}

	@Test
	public void testClearAndSyncIndices() {
		try {
			clearIndex();
		} catch (TimeoutException e) {
			fail("Index clear failed", e);
		}
		assertThat(userIndexHandler.init().blockingAwait(1, TimeUnit.MINUTES)).as("Creating index completed within timeout").isTrue();
		doTest(tx -> userIndexHandler.syncIndices(Optional.empty()).count().blockingGet(), 7, 1);
	}

	@Test
	public void testLoadAllElements() {
		Collection<? extends HibUser> elements = doTest(tx -> userIndexHandler.loadAllElements(ONLY_BUCKET, null), 1, 1);
		assertThat(elements).as("List of Elements").hasSize(totalNumUsers);
	}

	@Test
	public void testLoadAndGenerateVersion() {
		DataHolderContext dhc = new SimpleDataHolderContext();
		List<String> versions = doTest(tx -> userIndexHandler.loadAllElements(ONLY_BUCKET, dhc).stream()
				.map(e -> userIndexHandler.generateVersion(e, dhc)).collect(Collectors.toList()), 3, 1);
		assertThat(versions).as("List of versions").hasSize(totalNumUsers);
	}

	@Test
	public void testLoadAndToDocument() {
		DataHolderContext dhc = new SimpleDataHolderContext();
		List<JsonObject> documents = doTest(
				tx -> userIndexHandler.loadAllElements(ONLY_BUCKET, dhc).stream()
						.map(e -> userIndexHandler.getTransformer().toDocument(e, dhc)).collect(Collectors.toList()),
				5, 1);
		assertThat(documents).as("List of Documents").hasSize(totalNumUsers);
	}

	@Test
	public void testUpdateCommonGroup() {
		MeshElementEventModelImpl eventModel = new MeshElementEventModelImpl();
		eventModel.setEvent(MeshEvent.GROUP_UPDATED);
		eventModel.setName("common");
		eventModel.setUuid(commonGroup.getUuid());

		MessageEvent event = new MessageEvent(MeshEvent.GROUP_UPDATED, eventModel);

		Map<String, AtomicInteger> requestsPerIndex = new HashMap<>();

		Consumer<SearchRequest> requestConsumer = request -> {
			if (request instanceof CreateDocumentRequest) {
				String index = ((CreateDocumentRequest) request).getIndex();
				requestsPerIndex.computeIfAbsent(index, key -> new AtomicInteger()).incrementAndGet();
			}
		};

		MainEventHandler mainEventhandler = getSearchVerticle().getMainEventhandler();
		doTest(() -> mainEventhandler.handle(event).doOnNext(requestConsumer).blockingSubscribe(), 11, 1);

		assertThat(requestsPerIndex.getOrDefault("group", new AtomicInteger(0)).get())
				.as("Number of create requests for groups").isEqualTo(1);
		assertThat(requestsPerIndex.getOrDefault("user", new AtomicInteger(0)).get())
				.as("Number of create requests for users").isEqualTo(NUM_USERS);
	}
}
