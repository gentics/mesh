package com.gentics.mesh.search;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.VersioningParameters;

public class NodeSearchVerticleETest extends AbstractNodeSearchVerticleTest {

	@Test
	public void testDocumentDeletion() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("Concorde"),
				new PagingParameters().setPage(1).setPerPage(2), new VersioningParameters().draft()));
		assertEquals(1, response.getData().size());
		deleteNode(PROJECT_NAME, db.noTx(() -> content("concorde").getUuid()));

		response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleQuery("Concorde"), new PagingParameters().setPage(1).setPerPage(2),
				new VersioningParameters().draft()));
		assertEquals("We added the delete action and therefore the document should no longer be part of the index.", 0, response.getData().size());

	}

	@Test
	public void testBogusQuery() {
		call(() -> getClient().searchNodes(PROJECT_NAME, "bogus}J}son"), BAD_REQUEST, "search_query_not_parsable");
	}

	@Test
	public void testCustomQuery() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		NodeListResponse response = call(
				() -> getClient().searchNodes(PROJECT_NAME, getSimpleTermQuery("schema.name", "content"), new VersioningParameters().draft()));
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

	}

	@Test
	public void testSearchForChildNodes() throws Exception {
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

		String parentNodeUuid = db.noTx(() -> folder("news").getUuid());

		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, getSimpleTermQuery("parentNode.uuid", parentNodeUuid),
				new VersioningParameters().draft()));
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
		try (NoTx noTx = db.noTx()) {
			fullIndex();
		}

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
		NodeListResponse response = call(() -> getClient().searchNodes(PROJECT_NAME, search, new PagingParameters().setPage(1).setPerPage(2),
				new VersioningParameters().draft()));
		assertEquals(0, response.getData().size());

		// create a new folder named "bla"
		NodeCreateRequest create = new NodeCreateRequest();
		create.setSchema(new SchemaReference().setName("folder").setUuid(db.noTx(() -> schemaContainer("folder").getUuid())));
		create.setLanguage("en");
		create.getFields().put("name", FieldUtil.createStringField("bla"));
		create.setParentNodeUuid(db.noTx(() -> folder("2015").getUuid()));

		call(() -> getClient().createNode(PROJECT_NAME, create));

		// Search again and make sure we found our document
		response = call(() -> getClient().searchNodes(PROJECT_NAME, search, new PagingParameters().setPage(1).setPerPage(2),
				new VersioningParameters().draft()));
		assertEquals("Check search result after document creation", 1, response.getData().size());
	}
}
