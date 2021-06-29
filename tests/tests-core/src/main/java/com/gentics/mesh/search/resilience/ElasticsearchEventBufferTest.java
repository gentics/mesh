package com.gentics.mesh.search.resilience;

import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.junit.Test;

import java.io.IOException;

import static com.gentics.mesh.core.rest.MeshEvent.INDEX_SYNC_REQUEST;
import static com.gentics.mesh.test.ElasticsearchTestMode.CONTAINER_ES6_TOXIC;
import static com.gentics.mesh.test.MeshOptionChanger.SMALL_EVENT_BUFFER;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;

@MeshTestSetting(elasticsearch = CONTAINER_ES6_TOXIC, startServer = true, testSize = FULL, optionChanger = SMALL_EVENT_BUFFER)
public class ElasticsearchEventBufferTest extends AbstractMeshTest {
	private static final Logger log = LoggerFactory.getLogger(ElasticsearchEventBufferTest.class);

	@Test
	public void testRequestBufferOverflow() throws Exception {
		long createdFolderCount = 10;
		recreateIndices();
		long folderCountBefore = getESFolderCount();
		elasticsearch().dropTraffic();

		expect(INDEX_SYNC_REQUEST).one();

		for (int i = 0; i < createdFolderCount; i++) {
			createNode();
		}

		// This will create too many requests to ES which causes the sync event.
		for (int i = 0; i < 200; i++) {
			vertx().eventBus().publish(MeshEvent.SEARCH_FLUSH_REQUEST.address, null);
		}

		awaitEvents();

		elasticsearch().resumeTraffic();

		waitForSearchIdleEvent();

		assertEquals(createdFolderCount, getESFolderCount() - folderCountBefore);
	}

	private long getESFolderCount() throws IOException {
		NodeListResponse response = client().searchNodes(getESText("folders.es")).blockingGet();
		return response.getMetainfo().getTotalCount();
	}
}
