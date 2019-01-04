package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class NodeTagSearchEndpointTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchNodeByTag() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESText("tag.es");

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(response.getData()).isNotEmpty();

	}

	@Test
	public void testFailingQuery() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}
		String query = getESText("failing-query.es");
		MeshRestClientMessageException error = call(() -> client().searchNodes(PROJECT_NAME, query), BAD_REQUEST, "search_error_query");
		GenericMessageResponse message = error.getResponseMessage();
		assertNotNull("Detailed info not found", message.getProperties().get("cause-0"));
		System.out.println(message.toJson());

	}

	@Test
	public void testSearchNodaeByMultipleTagsNullPage() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESText("tags.es");
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query, new PagingParametersImpl().setPerPage(0L)));
		assertThat(response.getData()).isEmpty();
		assertEquals(1, response.getMetainfo().getPageCount());
	}

	@Test
	public void testSearchNodeByMultipleTags4() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESText("tags.es");
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(response.getData()).isNotEmpty().hasSize(1);
		String title = response.getData().get(0).getFields().getStringField("title").getString();
		assertEquals("Concorde english title", title);
	}
}
