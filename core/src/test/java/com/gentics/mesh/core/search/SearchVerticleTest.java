package com.gentics.mesh.core.search;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.search.SearchStatusResponse;
import com.gentics.mesh.etc.MeshSearchQueueProcessor;
import com.gentics.mesh.search.AbstractSearchVerticleTest;

import io.vertx.core.Future;

public class SearchVerticleTest extends AbstractSearchVerticleTest {

	@Autowired
	private MeshSearchQueueProcessor processor;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
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
	public void testAsyncSearchQueueUpdates() throws Exception {
		String uuid = folder("2015").getUuid();
		for (int i = 0; i < 10; i++) {
			meshRoot().getSearchQueue().createBatch("" + i).addEntry(uuid, "node", SearchQueueEntryAction.CREATE_ACTION);
		}
		CountDownLatch latch = new CountDownLatch(1);
		searchProvider.deleteDocument("node", "node-en", uuid, rh -> {
			if (rh.failed()) {
				fail(rh.cause().getMessage());
			}
			latch.countDown();
		});
		failingLatch(latch);
		processor.process();

		CountDownLatch getLatch = new CountDownLatch(1);
		searchProvider.getDocument("node", "node-en", uuid, rh -> {
			if (rh.failed()) {
				fail(rh.cause().getMessage());
			}
			assertNotNull(rh.result());
			getLatch.countDown();
		});
		failingLatch(getLatch);
	}

}
