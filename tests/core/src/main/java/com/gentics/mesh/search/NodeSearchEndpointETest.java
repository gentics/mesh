package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleTermQuery;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class NodeSearchEndpointETest extends AbstractNodeSearchEndpointTest {

	public NodeSearchEndpointETest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testDocumentDeletion() throws Exception {
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "Concorde"),
			new PagingParametersImpl().setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals(1, response.getData().size());
		deleteNode(PROJECT_NAME, db().tx(() -> content("concorde").getUuid()));

		waitForSearchIdleEvent();

		response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "Concorde"), new PagingParametersImpl().setPage(1)
			.setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals("We added the delete action and therefore the document should no longer be part of the index.", 0, response.getData().size());

	}

	@Test
	public void testBogusQuery() {
		call(() -> client().searchNodes(PROJECT_NAME, "bogus}J}son"), BAD_REQUEST, "search_query_not_parsable");
	}

	@Test
	public void testCustomQuery() throws Exception {
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("schema.name.raw", "content"),
			new VersioningParametersImpl().draft()));
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

	}

	@Test
	public void testSearchForChildNodes() throws Exception {
		recreateIndices();

		String parentNodeUuid = db().tx(() -> folder("news").getUuid());

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleTermQuery("parentNode.uuid", parentNodeUuid),
			new VersioningParametersImpl().draft()));
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());
		// TODO verify the found nodes are correct
		// for (NodeResponse childNode : response.getData()) {
		// System.out.println(childNode.getUuid());
		// System.out.println(((StringField)childNode.getField("name")).getString());
		// }
	}

	@Test
	public void testDocumentCreation() throws Exception {
		recreateIndices();

		// Invoke a dummy search on an empty index
		String json = "{";
		json += "				\"sort\" : {";
		json += "			      \"created\" : {\"order\" : \"asc\"}";
		json += "			    },";
		json += "			    \"query\":{";
		json += "			        \"bool\" : {";
		json += "			            \"must\" : {";
		json += "			                \"term\" : { \"fields.name\" : \"bla\" }";
		json += "			            }";
		json += "			        }";
		json += "			    }";
		json += "			}";

		String search = json;
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, search, new PagingParametersImpl().setPage(1).setPerPage(2L),
			new VersioningParametersImpl().draft()));
		assertEquals(0, response.getData().size());

		// create a new folder named "bla"
		NodeCreateRequest create = new NodeCreateRequest();
		create.setSchema(new SchemaReferenceImpl().setName("folder").setUuid(db().tx(() -> schemaContainer("folder").getUuid())));
		create.setLanguage("en");
		create.getFields().put("name", FieldUtil.createStringField("bla"));
		create.setParentNodeUuid(db().tx(() -> folder("2015").getUuid()));

		call(() -> client().createNode(PROJECT_NAME, create));

		waitForSearchIdleEvent();

		// Search again and make sure we found our document
		response = call(() -> client().searchNodes(PROJECT_NAME, search, new PagingParametersImpl().setPage(1).setPerPage(2L),
			new VersioningParametersImpl().draft()));
		assertEquals("Check search result after document creation", 1, response.getData().size());
	}
}
