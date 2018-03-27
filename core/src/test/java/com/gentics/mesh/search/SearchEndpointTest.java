package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.test.ClientHelper.assertMessage;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testNoPermReIndex() {
		call(() -> client().invokeIndexSync(), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReindex() {
		// Add the user to the admin group - this way the user is in fact an admin.
		try (Tx tx = tx()) {
			user().addGroup(groups().get("admin"));
			tx.success();
		}
		searchProvider().refreshIndex().blockingAwait();

		GenericMessageResponse message = call(() -> client().invokeIndexSync());
		assertMessage(message, "search_admin_reindex_invoked");
	}

	@Test
	public void testAsyncSearchQueueUpdates() throws Exception {
		try (Tx tx = tx()) {

			Node node = folder("2015");
			String uuid = node.getUuid();
			SearchQueueBatch batch = MeshInternal.get().searchQueue().create();
			for (int i = 0; i < 10; i++) {
				String releaseUuid = project().getLatestRelease().getUuid();
				batch.store(node, releaseUuid, DRAFT, true);
			}

			String documentId = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), "en");

			searchProvider().deleteDocument(Node.TYPE, documentId).blockingAwait();
			assertTrue("The document with uuid {" + uuid + "} could still be found within the search index. Used document id {" + documentId + "}",
					searchProvider().getDocument(Node.TYPE, documentId).blockingGet().isEmpty());
		}
	}

}
