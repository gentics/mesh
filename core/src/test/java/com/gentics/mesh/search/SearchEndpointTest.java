package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.assertMessage;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testNoPermReIndex() {
		call(() -> client().invokeReindex(), FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReindex() {
		// Add the user to the admin group - this way the user is in fact an admin.
		try (Tx tx = tx()) {
			user().addGroup(groups().get("admin"));
			tx.success();
		}
		searchProvider().refreshIndex();

		GenericMessageResponse message = call(() -> client().invokeReindex());
		assertMessage(message, "search_admin_reindex_invoked");
	}

	@Test
	@Ignore
	public void testClearIndex() throws Exception {
		try (Tx tx = tx()) {
			recreateIndices();
		}

		// Make sure the document was added to the index.
		Map<String, Object> map = searchProvider().getDocument(User.composeIndexName(), User.composeDocumentId(db().tx(() -> user().getUuid())))
				.toBlocking().value();
		assertNotNull("The user document should be stored within the index since we invoked a full index but it could not be found.", map);
		assertEquals(db().tx(() -> user().getUuid()), map.get("uuid"));

		for (IndexHandler<?> handler : meshDagger().indexHandlerRegistry().getHandlers()) {
			handler.clearIndex().await();
		}

		// Make sure the document is no longer stored within the search index.
		map = searchProvider().getDocument(User.composeIndexName(), User.composeDocumentId(db().tx(() -> user().getUuid()))).toBlocking().value();
		assertNull("The user document should no longer be part of the search index.", map);

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

			searchProvider().deleteDocument(Node.TYPE, documentId).await();
			assertNull("The document with uuid {" + uuid + "} could still be found within the search index. Used document id {" + documentId + "}", searchProvider().getDocument(Node.TYPE, documentId).toBlocking().value());
		}
	}

}
