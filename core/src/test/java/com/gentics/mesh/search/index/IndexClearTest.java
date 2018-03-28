package com.gentics.mesh.search.index;

import static com.gentics.mesh.Events.INDEX_SYNC_EVENT;
import static com.gentics.mesh.test.ClientHelper.assertMessage;
import static com.gentics.mesh.test.ClientHelper.call;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.search.impl.ElasticSearchProvider;
import com.gentics.mesh.search.verticle.ElasticsearchSyncVerticle;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = true, testSize = TestSize.FULL, startServer = true)
public class IndexClearTest extends AbstractMeshTest {

	private ElasticSearchProvider getProvider() {
		return ((ElasticSearchProvider) searchProvider());
	}

	@Test
	public void testClear() throws Exception {
		waitForEvent(INDEX_SYNC_EVENT, ElasticsearchSyncVerticle::invokeSync);

		GenericMessageResponse message = call(() -> client().invokeIndexClear());
		assertMessage(message, "search_admin_index_clear");

		try {
			getProvider().getDocument(User.composeIndexName(), userUuid()).blockingGet();
			fail("An error should occur");
		} catch (Exception e) {
			HttpErrorException error = (HttpErrorException) e.getCause();
			assertEquals(404, error.getStatusCode());
		}

	}
}
