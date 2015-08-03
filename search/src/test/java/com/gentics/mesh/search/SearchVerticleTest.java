package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.json.JsonObject;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class SearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SearchVerticle searchVerticle;

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Override
	public AbstractWebVerticle getVerticle() {
		return searchVerticle;
	}

	@BeforeClass
	public static void setup() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	private void setupFullIndex() throws InterruptedException {
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		for (Node node : boot.nodeRoot().findAll()) {
			searchQueue.put(node.getUuid(), Node.TYPE, CREATE_ACTION);
		}
		System.out.println("Search Queue size:" + searchQueue.getSize());

		CountDownLatch latch = new CountDownLatch(1);
		vertx.eventBus().send(SearchVerticle.QUEUE_EVENT_ADDRESS, true, new DeliveryOptions().setSendTimeout(100000L), rh -> {
			latch.countDown();
		});
		latch.await();
	}

	@Test
	public void testRemoveContent() throws InterruptedException {
		setupFullIndex();

	}

	@Test
	public void testAddContent() {
		Node node = folder("2015");
		JsonObject message = new JsonObject();
		message.put("uuid", node.getUuid());
		message.put("type", Node.TYPE);
//		vertx.eventBus().send(SearchVerticle.QUEUE_EVENT_ADDRESS);
	}

	@Test
	public void testUpdateContent() {

	}

	@Test
	public void testSearchContent() throws InterruptedException {
		setupFullIndex();

		QueryBuilder qb = QueryBuilders.queryStringQuery("the");
		Future<NodeListResponse> future = getClient().searchNodes(qb.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(2, response.getData().size());
		assertEquals(9, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}
}
