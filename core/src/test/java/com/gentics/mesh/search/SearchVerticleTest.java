package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.search.SearchQueueEntryAction.STORE_ACTION;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.search.index.IndexHandler;
import com.gentics.mesh.search.index.node.NodeIndexHandler;

import io.vertx.core.AbstractVerticle;

public class SearchVerticleTest extends AbstractSearchVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.searchVerticle());
		return list;
	}

	@Test
	public void testLoadSearchStatus() {
		MeshResponse<SearchStatusResponse> future = getClient().loadSearchStatus().invoke();
		latchFor(future);
		assertSuccess(future);
		SearchStatusResponse status = future.result();
		assertNotNull(status);
		assertEquals(0, status.getBatchCount());
	}

	@Test
	public void testNoPermReIndex() {
		MeshResponse<GenericMessageResponse> future = getClient().invokeReindex().invoke();
		latchFor(future);
		expectException(future, FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReindex() {
		// Add the user to the admin group - this way the user is in fact an admin.
		try (NoTx noTrx = db.noTx()) {
			user().addGroup(groups().get("admin"));
			searchProvider.refreshIndex();
		}

		GenericMessageResponse message = call(() -> getClient().invokeReindex());
		expectResponseMessage(message, "search_admin_reindex_invoked");

		SearchStatusResponse status = call(() -> getClient().loadSearchStatus());
		assertNotNull(status);
		assertEquals(0, status.getBatchCount());
	}

	@Test
	public void testClearIndex() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			fullIndex();
		}

		// Make sure the document was added to the index.
		Map<String, Object> map = searchProvider.getDocument("user", "user", db.noTx(() -> user().getUuid())).toBlocking().single();
		assertNotNull("The user document should be stored within the index since we invoked a full index but it could not be found.", map);
		assertEquals(db.noTx(() -> user().getUuid()), map.get("uuid"));

		for (IndexHandler handler : meshDagger.indexHandlerRegistry().getHandlers()) {
			handler.clearIndex().await();
		}

		// Make sure the document is no longer stored within the search index.
		map = searchProvider.getDocument("user", "user", db.noTx(() -> user().getUuid())).toBlocking().single();
		assertNull("The user document should no longer be part of the search index.", map);

	}

	@Test
	public void testAsyncSearchQueueUpdates() throws Exception {
		try (NoTx noTrx = db.noTx()) {

			Node node = folder("2015");
			String uuid = node.getUuid();
			String indexType = NodeIndexHandler.getDocumentType();
			for (int i = 0; i < 10; i++) {
				meshRoot().getSearchQueue().createBatch().addEntry(uuid, Node.TYPE, STORE_ACTION);
			}

			String documentId = NodeIndexHandler.composeDocumentId(node, "en");
			searchProvider.deleteDocument(Node.TYPE, indexType, documentId).await();
			meshRoot().getSearchQueue().processAll();
			assertNull(
					"The document with uuid {" + uuid + "} could still be found within the search index. Used index type {" + indexType
							+ "} document id {" + documentId + "}",
					searchProvider.getDocument(Node.TYPE, indexType, documentId).toBlocking().first());
		}
	}

}
