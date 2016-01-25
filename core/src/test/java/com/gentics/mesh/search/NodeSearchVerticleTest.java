package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.codehaus.jettison.json.JSONException;
import org.elasticsearch.common.collect.Tuple;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.search.SearchQueue;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.query.impl.NodeRequestParameter;
import com.gentics.mesh.query.impl.NodeRequestParameter.LinkType;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.search.index.NodeIndexHandler;

import io.vertx.core.Future;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;

public class NodeSearchVerticleTest extends AbstractSearchVerticleTest implements BasicSearchCrudTestcases {

	@Autowired
	private NodeVerticle nodeVerticle;

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
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
		assertEquals(1, response.getData().size());
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
		fullIndex();

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

		Node node = folder("2015");

		StringGraphFieldList list = node.getGraphFieldContainer(english()).createStringList("stringList");
		list.createString("one");
		list.createString("two");
		list.createString("three");
		list.createString("four");

		Schema schema = node.getSchemaContainer().getSchema();
		schema.addField(new ListFieldSchemaImpl().setListType("string").setName("stringList"));
		node.getSchemaContainer().setSchema(schema);

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
	public void testSearchContentResolveLinks() throws InterruptedException, JSONException {
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("the"),
				new PagingParameter().setPage(1).setPerPage(2),
				new NodeRequestParameter().setResolveLinks(LinkType.FULL));
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

	/**
	 * Search in only english language versions of nodes
	 *
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchEnglish() throws InterruptedException, JSONException {
		searchWithLanguages("en");
	}

	/**
	 * Searhc in only german language versions of nodes
	 *
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchGerman() throws InterruptedException, JSONException {
		searchWithLanguages("de");
	}

	/**
	 * Search for string which can be found in two language variants of a single
	 * node. We would expect two nodes in the result which have different
	 * language properties.
	 *
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchMultipleLanguages() throws InterruptedException, JSONException {
		searchWithLanguages("de", "en");
	}

	/**
	 * Do the search with the given set of expected languages and assert correctness of the result
	 *
	 * @param expectedLanguages
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	protected void searchWithLanguages(String...expectedLanguages) throws InterruptedException, JSONException {
		fullIndex();

		Node node = content("concorde");

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("concorde"),
				new PagingParameter().setPage(1).setPerPage(100), new NodeRequestParameter().setLanguages(expectedLanguages));
		latchFor(future);
		assertSuccess(future);
		NodeListResponse response = future.result();
		assertEquals("Check # of returned nodes", expectedLanguages.length, response.getData().size());
		assertEquals("Check total count", expectedLanguages.length, response.getMetainfo().getTotalCount());

		Set<String> foundLanguages = new HashSet<>();
		for (NodeResponse nodeResponse : response.getData()) {
			assertEquals("Check uuid of found node", node.getUuid(), nodeResponse.getUuid());
			foundLanguages.add(nodeResponse.getLanguage());
		}

		Set<String> notFound = new HashSet<>(Arrays.asList(expectedLanguages));
		notFound.removeAll(foundLanguages);
		assertTrue("Did not find nodes in expected languages: " + notFound, notFound.isEmpty());

		Set<String> unexpected = new HashSet<>(foundLanguages);
		unexpected.removeAll(Arrays.asList(expectedLanguages));
		assertTrue("Found nodes in unexpected languages: " + unexpected, unexpected.isEmpty());
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

	@Test
	public void testSearchMicronode() throws Exception {
		addMicronodeField();
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("Mickey"), new PagingParameter().setPage(1).setPerPage(2));
		latchFor(future);
		assertSuccess(future);

		NodeListResponse response = future.result();
		assertEquals("Check returned search results", 1, response.getData().size());
		assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull("Returned node must not be null", nodeResponse);
			assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
		}
	}

	@Test
	public void testSearchMicronodeResolveLinks() throws Exception {
		addMicronodeField();
		fullIndex();

		Future<NodeListResponse> future = getClient().searchNodes(getSimpleQuery("Mickey"),
				new PagingParameter().setPage(1).setPerPage(2),
				new NodeRequestParameter().setResolveLinks(LinkType.FULL));
		latchFor(future);
		assertSuccess(future);

		NodeListResponse response = future.result();
		assertEquals("Check returned search results", 1, response.getData().size());
		assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull("Returned node must not be null", nodeResponse);
			assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
		}
	}

	@Test
	public void testSearchListOfMicronodes() throws Exception {
		addMicronodeListField();
		fullIndex();

		for (String firstName : Arrays.asList("Mickey", "Donald")) {
			for (String lastName : Arrays.asList("Mouse", "Duck")) {
				// valid names always begin with the same character
				boolean expectResult = firstName.substring(0, 1).equals(lastName.substring(0, 1));

				Future<NodeListResponse> future = getClient().searchNodes(getNestedVCardListSearch(firstName, lastName),
						new PagingParameter().setPage(1).setPerPage(2));
				latchFor(future);
				assertSuccess(future);

				NodeListResponse response = future.result();

				if (expectResult) {
					assertEquals("Check returned search results", 1, response.getData().size());
					assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());
					for (NodeResponse nodeResponse : response.getData()) {
						assertNotNull("Returned node must not be null", nodeResponse);
						assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
					}
				} else {
					assertEquals("Check returned search results", 0, response.getData().size());
					assertEquals("Check total search results", 0, response.getMetainfo().getTotalCount());
				}
			}
		}
	}

	@Test
	public void testSearchListOfMicronodesResolveLinks() throws Exception {
		addMicronodeListField();
		fullIndex();

		for (String firstName : Arrays.asList("Mickey", "Donald")) {
			for (String lastName : Arrays.asList("Mouse", "Duck")) {
				// valid names always begin with the same character
				boolean expectResult = firstName.substring(0, 1).equals(lastName.substring(0, 1));

				Future<NodeListResponse> future = getClient().searchNodes(getNestedVCardListSearch(firstName, lastName),
						new PagingParameter().setPage(1).setPerPage(2), new NodeRequestParameter().setResolveLinks(LinkType.FULL));
				latchFor(future);
				assertSuccess(future);

				NodeListResponse response = future.result();

				if (expectResult) {
					assertEquals("Check returned search results", 1, response.getData().size());
					assertEquals("Check total search results", 1, response.getMetainfo().getTotalCount());
					for (NodeResponse nodeResponse : response.getData()) {
						assertNotNull("Returned node must not be null", nodeResponse);
						assertEquals("Check result uuid", content("concorde").getUuid(), nodeResponse.getUuid());
					}
				} else {
					assertEquals("Check returned search results", 0, response.getData().size());
					assertEquals("Check total search results", 0, response.getMetainfo().getTotalCount());
				}
			}
		}
	}

	private void addNumberSpeedField(int number) {
		Node node = content("concorde");

		Schema schema = node.getSchemaContainer().getSchema();
		schema.addField(new NumberFieldSchemaImpl().setName("speed"));
		node.getSchemaContainer().setSchema(schema);

		node.getGraphFieldContainer(english()).createNumber("speed").setNumber(number);
	}

	/**
	 * Add a micronode field to the tested content
	 */
	private void addMicronodeField() {
		Node node = content("concorde");

		Schema schema = node.getSchema();
		MicronodeFieldSchemaImpl vcardFieldSchema = new MicronodeFieldSchemaImpl();
		vcardFieldSchema.setName("vcard");
		vcardFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
		schema.addField(vcardFieldSchema);

		MicronodeGraphField vcardField = node.getGraphFieldContainer(english()).createMicronode("vcard", microschemaContainers().get("vcard"));
		vcardField.getMicronode().createString("firstName").setString("Mickey");
		vcardField.getMicronode().createString("lastName").setString("Mouse");
	}

	/**
	 * Add a micronode list field to the tested content
	 */
	private void addMicronodeListField() {
		Node node = content("concorde");

		Schema schema = node.getSchema();
		ListFieldSchema vcardListFieldSchema = new ListFieldSchemaImpl();
		vcardListFieldSchema.setName("vcardlist");
		vcardListFieldSchema.setListType("micronode");
		vcardListFieldSchema.setAllowedSchemas(new String[] { "vcard" });
		schema.addField(vcardListFieldSchema);

		// set the mapping for the schema
		nodeIndexHandler.setNodeIndexMapping("node", schema.getName(), schema).toBlocking().first();

		MicronodeGraphFieldList vcardListField = node.getGraphFieldContainer(english()).createMicronodeFieldList("vcardlist");
		for (Tuple<String, String> testdata : Arrays.asList(Tuple.tuple("Mickey", "Mouse"), Tuple.tuple("Donald", "Duck"))) {
			MicronodeField field = new MicronodeResponse();
			Micronode micronode = vcardListField.createMicronode(field);
			micronode.setMicroschemaContainer(microschemaContainers().get("vcard"));
			micronode.createString("firstName").setString(testdata.v1());
			micronode.createString("lastName").setString(testdata.v2());
		}
	}

	/**
	 * Generate the JSON for a searched in the nested field vcardlist
	 * 
	 * @param firstName
	 *            firstname to search for
	 * @param lastName
	 *            lastname to search for
	 * @return search JSON
	 * @throws IOException
	 */
	private String getNestedVCardListSearch(String firstName, String lastName) throws IOException {
		return XContentFactory.jsonBuilder().startObject().startObject("query").startObject("nested").field("path", "fields.vcardlist")
				.startObject("query").startObject("bool").startArray("must").startObject().startObject("match")
				.field("fields.vcardlist.fields.firstName", firstName).endObject().endObject().startObject().startObject("match")
				.field("fields.vcardlist.fields.lastName", lastName).endObject().endObject().endArray().endObject().endObject().endObject()
				.endObject().endObject().string();
	}
}
