package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.query.impl.PagingParameter;

import io.vertx.core.Future;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public class NodeSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Test
	public void testSearchAndSort() throws InterruptedException {
		fullIndex();

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
	@Override
	public void testDocumentDeletion() throws InterruptedException, JSONException {
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("Concorde"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(2, response.getData().size());
		deleteNode(PROJECT_NAME, content("concorde").getUuid());

		future = getClient().searchNodes(getSimpleQuery("Concorde"), new PagingParameter().setPage(1).setPerPage(2));
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
	public void testCustomQuery() throws InterruptedException, JSONException {

		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleTermQuery("schema.name", "content"));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

	}

	@Test
	public void testSearchForChildNodes() throws JSONException, InterruptedException {
		fullIndex();

		Node parentNode = folder("news");

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleTermQuery("parentNode.uuid", parentNode.getUuid()));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());
		// TODO verify the found nodes are correct
		// for (NodeResponse childNode : response.getData()) {
		// System.out.println(childNode.getUuid());
		// System.out.println(((StringField)childNode.getField("name")).getString());
		// }
	}

	@Test
	@Override
	public void testDocumentCreation() throws Exception {
		Node node = folder("2015");

		StringGraphFieldList list = node.getGraphFieldContainer(english()).createStringList("stringList");
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

		Future<NodeListResponse> future = getClient().searchNodes(json, new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(0, response.getData().size());

		// Create the update entry in the search queue
		SearchQueueBatch batch;
		try (Trx tx = db.trx()) {
			SearchQueue searchQueue = boot.meshRoot().getSearchQueue();
			batch = searchQueue.createBatch("0");
			batch.addEntry(node.getUuid(), Node.TYPE, SearchQueueEntryAction.CREATE_ACTION);
			tx.success();
		}
		try (Trx tx = db.trx()) {
			batch.process().toBlocking().last();
		}

		// Search again and make sure we found our document
		future = getClient().searchNodes(json, new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals(
				"There should be at least one item in the resultset since we added the search queue entry and the index should now contain this item.",
				1, response.getData().size());

	}

	@Test
	@Override
	public void testDocumentUpdate() throws Exception {
		fullIndex();

		String newString = "ABCDEFGHI";
		Node node;
		SearchQueueBatch batch;
		try (Trx tx = db.trx()) {
			node = content("concorde");
			assertNotNull(node);
			HtmlGraphField field = node.getGraphFieldContainer(english()).getHtml("content");
			assertNotNull(field);
			field.setHtml(newString);

			// Create the update entry in the search queue
			batch = boot.meshRoot().getSearchQueue().createBatch("0");
			batch.addEntry(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION, Node.TYPE + "-en");
			batch.addEntry(node.getUuid(), Node.TYPE, SearchQueueEntryAction.UPDATE_ACTION, Node.TYPE + "-de");
			tx.success();
		}

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("supersonic"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(1, response.getData().size());

		try (Trx tx = db.trx()) {
			batch.process().toBlocking().last();
		}

		future = getClient().searchNodes(getSimpleQuery("supersonic"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("The node with name {" + "Concorde" + "} should no longer be found since we updated the node and updated the content.", 0,
				response.getData().size());

		future = getClient().searchNodes(getSimpleQuery(newString), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		response = future.result();
		assertEquals("There should be one item in the resultset since we updated the node and invoked the index update.", 1,
				response.getData().size());

	}

	@Test
	public void testSearchContent() throws InterruptedException, JSONException {
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("the"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}

	@Test
	public void testSearchNumberRange() throws Exception {
		int numberValue = 1200;
		addNumberSpeedField(numberValue);
		fullIndex();

		// from 1 to 9
		ObservableFuture<NodeListResponse> obs = RxHelper.observableFuture();
		getClient().searchNodes(getRangeQuery("speed", 100, 9000)).setHandler(obs.toHandler());
		int resultCount = obs.map(l -> l.getData().size()).toBlocking().single();
		assertEquals(1, resultCount);
	}

	@Test
	public void testSearchNumberRange2() throws Exception {
		int numberValue = 1200;
		addNumberSpeedField(numberValue);
		fullIndex();

		// from 9 to 1
		ObservableFuture<NodeListResponse> obs = RxHelper.observableFuture();
		getClient().searchNodes(getRangeQuery("speed", 900, 1500)).setHandler(obs.toHandler());
		int resultCount = obs.map(l -> l.getData().size()).toBlocking().single();
		assertEquals("We could expect to find the node with the given seed number field since the value {" + numberValue
				+ "} is between the search range.", 1, resultCount);
	}

	@Test
	public void testSearchNumberRange3() throws Exception {
		int numberValue = 1200;
		addNumberSpeedField(numberValue);
		fullIndex();

		// out of bounds
		ObservableFuture<NodeListResponse> obs = RxHelper.observableFuture();
		getClient().searchNodes(getRangeQuery("speed", 1000, 90)).setHandler(obs.toHandler());
		int resultCount = obs.map(l -> l.getData().size()).toBlocking().single();
		assertEquals("No node should be found since the range is invalid.", 0, resultCount);
	}

	private void addNumberSpeedField(int number) {
		Node node = content("concorde");

		Schema schema = node.getSchemaContainer().getSchema();
		schema.addField(new NumberFieldSchemaImpl().setName("speed"));
		node.getSchemaContainer().setSchema(schema);

		node.getGraphFieldContainer(english()).createNumber("speed").setNumber(number);
	}
}
