package com.gentics.mesh.core.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.etc.MeshSearchQueueProcessor;
import com.gentics.mesh.search.AbstractSearchVerticleTest;

import io.vertx.core.Future;

public class SearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private MeshSearchQueueProcessor processor;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
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
		user().addGroup(groups().get("admin"));

		Future<GenericMessageResponse> future = getClient().invokeReindex();
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("search_admin_reindex_invoked", future);

		Future<SearchStatusResponse> statusFuture = getClient().loadSearchStatus();
		latchFor(statusFuture);
		assertSuccess(statusFuture);
		SearchStatusResponse status = statusFuture.result();
		assertNotNull(status);
		assertEquals(0, status.getBatchCount());
	}

	@Test
	public void testAsyncSearchQueueUpdates() throws Exception {
		String uuid = folder("2015").getUuid();
		for (int i = 0; i < 10; i++) {
			meshRoot().getSearchQueue().createBatch("" + i).addEntry(uuid, "node", SearchQueueEntryAction.CREATE_ACTION);
		}
		searchProvider.deleteDocument("node", "node-en", uuid).toBlocking().first();
		processor.process();
		assertNotNull("The document with uuid {" + uuid + "} still be found within the search index.",searchProvider.getDocument("node", "node-en", uuid).toBlocking().first());
	}

}
