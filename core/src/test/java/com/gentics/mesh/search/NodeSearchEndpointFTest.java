package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.time.Instant;
import java.util.Date;

import org.codehaus.jettison.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestSetting;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class NodeSearchEndpointFTest extends AbstractNodeSearchEndpointTest {

	public NodeSearchEndpointFTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testSearchAndSort() throws Exception {
		recreateIndices();

		String query = getESText("contentSchemaTermQuery.es");
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query, new VersioningParametersImpl().draft()));
		assertNotNull(response);
		assertFalse(response.getData().isEmpty());

		long lastCreated = 0;
		for (NodeResponse nodeResponse : response.getData()) {
			Date date = Date.from(Instant.parse(nodeResponse.getCreated()));
			if (lastCreated > date.getTime()) {
				fail("Found entry that was not sorted by create timestamp. Last entry: {" + lastCreated + "} current entry: {" + nodeResponse
					.getCreated() + "}");
			} else {
				lastCreated = date.getTime();
			}
			assertEquals("content", nodeResponse.getSchema().getName());
		}
	}

	@Test
	public void testSearchContent() throws Exception {
		recreateIndices();

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "the"), new PagingParametersImpl()
			.setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
		assertEquals(1, response.getData().size());
		assertEquals(1, response.getMetainfo().getTotalCount());
		for (NodeResponse nodeResponse : response.getData()) {
			assertNotNull(nodeResponse);
			assertNotNull(nodeResponse.getUuid());
		}

	}

	/**
	 * Test searching for a node which is only persisted in the search index but no longer in the graph.
	 * 
	 * @throws InterruptedException
	 * @throws JSONException
	 */
	@Test
	public void testSearchMissingVertex() throws Exception {
		recreateIndices();
		try (Tx tx = tx()) {
			NodeDaoWrapper nodeDao = tx.data().nodeDao();
			BulkActionContext context = createBulkContext();
			nodeDao.delete(content("honda nr"), context, false, true);
			tx.success();
		}

		waitForSearchIdleEvent();

		NodeListResponse response = call(() -> client().searchNodes(getSimpleQuery("fields.content", "the"), new PagingParametersImpl().setPage(1)
			.setPerPage(2L)));
		assertEquals(0, response.getData().size());
		assertEquals(0, response.getMetainfo().getTotalCount());
	}

	@Test
	public void testSearchWithManySchemas() {
		for (int i = 0; i < 45; i++) {
			SchemaCreateRequest request = new SchemaCreateRequest();
			request.setName("dummy" + i);
			request.addField(FieldUtil.createHtmlFieldSchema("content"));
			SchemaResponse response = call(() -> client().createSchema(request));
			call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));
		}

		waitForSearchIdleEvent();

		call(() -> client().searchNodes(PROJECT_NAME, getSimpleQuery("fields.content", "the"), new PagingParametersImpl()
			.setPage(1).setPerPage(2L), new VersioningParametersImpl().draft()));
	}

}
