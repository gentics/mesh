package com.gentics.mesh.search.index;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_FINISHED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.parameter.client.IndexMaintenanceParametersImpl;
import com.gentics.mesh.search.verticle.eventhandler.SyncEventHandler;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class IndexClearTest extends AbstractMeshTest {
	@Before
	public void setup() throws Exception {
		getProvider().clear().blockingAwait();
		syncIndex();
		revokeAdmin();
	}

	@Test
	public void testClear() throws Exception {
		waitForEvent(INDEX_SYNC_FINISHED, () -> SyncEventHandler.invokeSync(vertx(), null));

		call(() -> client().invokeIndexClear(), FORBIDDEN, "error_admin_permission_required");
		grantAdmin();

		GenericMessageResponse message = call(() -> client().invokeIndexClear());
		assertThat(message).matches("search_admin_index_clear");

		assertDocumentDoesNotExist(User.composeIndexName(), User.composeDocumentId(userUuid()));

	}

	/**
	 * Test that clearing the index restricted by name only clears the specified index
	 * @throws Exception
	 */
	@Test
	public void testClearWithName() throws Exception {
		runClearTest(false);
	}

	/**
	 * Test that clearing indices can also be done with the full name (including the installation prefix)
	 * @throws Exception
	 */
	@Test
	public void testClearWithFullName() throws Exception {
		runClearTest(true);
	}

	/**
	 * Run the clear test
	 * @param prefix true to use the prefixed index name, false to use the bare index name
	 * @throws Exception
	 */
	protected void runClearTest(boolean prefix) throws Exception {
		String index = prefix ? "mesh-user" : "user";
		grantAdmin();

		// check that the project is found in index
		assertDocumentExists(Project.composeIndexName(), Project.composeDocumentId(projectUuid()));
		// check that the user is found in index
		assertDocumentExists(User.composeIndexName(), User.composeDocumentId(userUuid()));

		call(() -> client().invokeIndexClear(new IndexMaintenanceParametersImpl().setIndex(index)));

		// check that the project is still found in index
		assertDocumentExists(Project.composeIndexName(), Project.composeDocumentId(projectUuid()));
		// check that the user is no longer found in index
		assertDocumentDoesNotExist(User.composeIndexName(), User.composeDocumentId(userUuid()));
	}
}
