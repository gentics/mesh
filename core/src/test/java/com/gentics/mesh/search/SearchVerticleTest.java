package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueue.SEARCH_QUEUE_ENTRY_ADDRESS;
import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.CREATE_ACTION;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.io.FileUtils;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.SpringElasticSearchTestConfiguration;

import io.vertx.core.Future;
import io.vertx.core.eventbus.DeliveryOptions;

@ContextConfiguration(classes = { SpringElasticSearchTestConfiguration.class })
public class SearchVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SearchVerticle searchVerticle;

	@Autowired
	private org.elasticsearch.node.Node elasticSearchNode;

	@Override
	public AbstractWebVerticle getVerticle() {
		return searchVerticle;
	}

	@After
	public void resetElasticSearch() {
		elasticSearchNode.client().prepareDeleteByQuery("node").setQuery(QueryBuilders.matchAllQuery()).execute().actionGet();
	}

	@BeforeClass
	@AfterClass
	public static void cleanup() throws IOException {
		FileUtils.deleteDirectory(new File("data"));
	}

	private void setupFullIndex() throws InterruptedException {
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		for (Node node : boot.nodeRoot().findAll()) {
			searchQueue.put(node.getUuid(), Node.TYPE, CREATE_ACTION);
		}
		System.out.println("Search Queue size:" + searchQueue.getSize());

		CountDownLatch latch = new CountDownLatch(1);
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true, new DeliveryOptions().setSendTimeout(100000L), rh -> {
			latch.countDown();
		});
		latch.await();
	}

	@Test
	public void testSearchAndSort() throws InterruptedException {
		setupFullIndex();

		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"schema.name\" : \"content\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		Future<NodeListResponse> future = getClient().searchNodes(json);
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

		long lastCreated = 0;
		for (NodeResponse nodeResponse : response.getData()) {
			if (lastCreated > nodeResponse.getCreated()) {
				fail("Found entry that was not sorted by create timestamp. Last entry: {" + lastCreated + "} current entry: {"
						+ nodeResponse.getCreated() + "}");
			} else {
				lastCreated = nodeResponse.getCreated();
			}
			assertEquals("content", nodeResponse.getSchema().getName());
		}
	}

	@Test
	public void testRemoveContent() throws InterruptedException {
		setupFullIndex();
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();

		QueryBuilder qb = QueryBuilders.queryStringQuery("Großraumflugzeug");
		Future<NodeListResponse> future = getClient().searchNodes(qb.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(1, response.getData().size());

		// Create a delete entry in the search queue
		NodeResponse nodeResponse = response.getData().get(0);
		searchQueue.put(nodeResponse.getUuid(), Node.TYPE, SearchQueueEntryAction.DELETE_ACTION);
		CountDownLatch latch = new CountDownLatch(1);
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true, new DeliveryOptions().setSendTimeout(100000L), rh -> {
			latch.countDown();
		});
		latch.await();

		future = getClient().searchNodes(qb.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("We added the delete action and therefore the document should no longer be part of the index.", 0, response.getData().size());

	}

	@Test
	public void testBogusQuery() {
		Future<NodeListResponse> future = getClient().searchNodes("bogus}J}son");
		latchFor(future);
		expectException(future, BAD_REQUEST, "search_query_not_parsable");
	}

	@Test
	public void testCustomQuery() throws InterruptedException {
		setupFullIndex();

		QueryBuilder qb = QueryBuilders.termQuery("schema.name", "content");

		Future<NodeListResponse> future = getClient().searchNodes(qb.toString());
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

	}

	@Test
	public void testAddContent() throws InterruptedException, IOException {
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
		Node node = folder("2015");

		GraphStringFieldList list = node.getFieldContainer(english()).createStringList("stringList");
		list.createString("one");
		list.createString("two");
		list.createString("three");
		list.createString("four");

		Schema schema = node.getSchemaContainer().getSchema();
		schema.addField(new ListFieldSchemaImpl().setListType("string").setName("stringList"));
		node.getSchemaContainer().setSchema(schema);

		// Invoke a dummy search on an empty index
		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"fields.stringList\" : \"three\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		Future<NodeListResponse> future = getClient().searchNodes(json, new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(0, response.getData().size());

		// Create the update entry in the search queue
		searchQueue.put(node.getUuid(), Node.TYPE, SearchQueueEntryAction.CREATE_ACTION);
		CountDownLatch latch = new CountDownLatch(1);
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true, new DeliveryOptions().setSendTimeout(100000L), rh -> {
			latch.countDown();
		});
		latch.await();

		// Search again and make sure we found our document
		future = getClient().searchNodes(json, new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(
				"There should be at least one item in the resultset since we added the search queue entry and the index should now contain this item.",
				1, response.getData().size());

	}

	@Test
	public void testUpdateContent() throws InterruptedException {
		setupFullIndex();
		SearchQueue searchQueue = boot.meshRoot().getSearchQueue();

		String oldName = "Airbus A300";
		Node node = content(oldName.toLowerCase());
		assertNotNull(node);
		HtmlGraphField field = node.getFieldContainer(german()).getHtml("content");
		assertNotNull(field);
		String newString = "ABCDEFGHI";
		field.setHtml(newString);

		QueryBuilder qb = QueryBuilders.queryStringQuery("Großraumflugzeug");
		Future<NodeListResponse> future = getClient().searchNodes(qb.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(1, response.getData().size());

		// Create the update entry in the search queue
		searchQueue.put(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION);
		CountDownLatch latch = new CountDownLatch(1);
		vertx.eventBus().send(SEARCH_QUEUE_ENTRY_ADDRESS, true, new DeliveryOptions().setSendTimeout(100000L), rh -> {
			latch.countDown();
		});
		latch.await();

		future = getClient().searchNodes(qb.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("The node with name {" + oldName + "} should no longer be found since we updated the node and updated the index.", 0,
				response.getData().size());

		qb = QueryBuilders.queryStringQuery(newString);
		future = getClient().searchNodes(qb.toString(), new PagingInfo().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("There should be one item in the resultset since we updated the node and invoked the index update.", 1,
				response.getData().size());

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
