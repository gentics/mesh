package com.gentics.mesh.search.index;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
@MeshTestSetting(elasticsearch = CONTAINER, testSize = TestSize.FULL, startServer = true)
public class IndexClearTest extends AbstractMeshTest {

	@Test
	public void testClear() throws Exception {
		waitForEvent(INDEX_SYNC_FINISHED, () -> SyncEventHandler.invokeSync(vertx()));

		call(() -> client().invokeIndexClear(), FORBIDDEN, "error_admin_permission_required");
		tx(() -> group().addRole(roles().get("admin")));

		GenericMessageResponse message = call(() -> client().invokeIndexClear());
		assertThat(message).matches("search_admin_index_clear");

		try {
			getProvider().getDocument(User.composeIndexName(), userUuid()).blockingGet();
			fail("An error should occur");
		} catch (Exception e) {
			HttpErrorException error = (HttpErrorException) e.getCause();
			assertEquals(404, error.getStatusCode());
		}

	}
}
