package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, startServer = true, testSize = FULL)
public class NodeTagSearchEndpointTest extends AbstractNodeSearchEndpointTest {

	@Test
	public void testSearchNodeByTag() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESQuery("tag.es");

		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(response.getData()).isNotEmpty();

	}

	@Test
	public void testFailingQuery() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESQuery("failing-query.es");
		call(() -> client().searchNodes(PROJECT_NAME, query), BAD_REQUEST, "search_error_query");

	}

	@Test
	public void testSearchNodaeByMultipleTagsNullPage() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESQuery("tags.es");
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query, new PagingParametersImpl().setPerPage(0)));
		assertThat(response.getData()).isEmpty();
		assertEquals(1, response.getMetainfo().getPageCount());
	}

	@Test
	public void testSearchNodeByMultipleTags4() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		String query = getESQuery("tags.es");
		NodeListResponse response = call(() -> client().searchNodes(PROJECT_NAME, query));
		assertThat(response.getData()).isNotEmpty().hasSize(1);
		String title = response.getData().get(0).getFields().getStringField("title").getString();
		assertEquals("Concorde english title", title);
	}
}
