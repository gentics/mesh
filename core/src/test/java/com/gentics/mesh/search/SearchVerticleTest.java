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
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.search.index.IndexHandler;
import com.gentics.mesh.search.index.NodeIndexHandler;

import io.vertx.core.Future;

public class SearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private NodeIndexHandler nodeIndexHandler;

	@Autowired
	private IndexHandlerRegistry registry;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(searchVerticle);
		return list;
	}

	@Test
	public void testLoadSearchStatus() {
		Future<SearchStatusResponse> future = getClient().loadSearchStatus();
		latchFor(future);
		assertSuccess(future);
		SearchStatusResponse status = future.result();
		assertNotNull(status);
		assertEquals(0, status.getBatchCount());
	}

	@Test
	public void testNoPermReIndex() {
		Future<GenericMessageResponse> future = getClient().invokeReindex();
		latchFor(future);
		expectException(future, FORBIDDEN, "error_admin_permission_required");
	}

	@Test
	public void testReindex() {
		// Add the user to the admin group - this way the user is in fact an admin.
		try (NoTrx noTrx = db.noTrx()) {
			user().addGroup(groups().get("admin"));
			searchProvider.refreshIndex();
		}

		Future<GenericMessageResponse> future = getClient().invokeReindex();
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "search_admin_reindex_invoked");

		Future<SearchStatusResponse> statusFuture = getClient().loadSearchStatus();
		latchFor(statusFuture);
		assertSuccess(statusFuture);
		SearchStatusResponse status = statusFuture.result();
		assertNotNull(status);
		assertEquals(0, status.getBatchCount());
	}

	@Test
	public void testClearIndex() throws InterruptedException {
		try (NoTrx noTrx = db.noTrx()) {
			fullIndex();
		}

		// Make sure the document was added to the index.
		Map<String, Object> map = searchProvider.getDocument("user", "user", db.noTrx(() -> user().getUuid())).toBlocking().single();
		assertNotNull("The user document should be stored within the index since we invoked a full index but it could not be found.", map);
		assertEquals(db.noTrx(() -> user().getUuid()), map.get("uuid"));

		for (IndexHandler handler : registry.getHandlers()) {
			handler.clearIndex().await();
		}

		// Make sure the document is no longer stored within the search index.
		map = searchProvider.getDocument("user", "user", db.noTrx(() -> user().getUuid())).toBlocking().single();
		assertNull("The user document should no longer be part of the search index.", map);

	}

	@Test
	public void testAsyncSearchQueueUpdates() throws Exception {
		try (NoTrx noTrx = db.noTrx()) {

			Node node = folder("2015");
			String uuid = node.getUuid();
			String indexType = NodeIndexHandler.getDocumentType(node.getSchemaContainer().getLatestVersion());
			for (int i = 0; i < 10; i++) {
				meshRoot().getSearchQueue().createBatch("" + i).addEntry(uuid, Node.TYPE, STORE_ACTION);
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
