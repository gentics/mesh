package com.gentics.mesh.search;

import static com.gentics.mesh.core.data.ContainerType.DRAFT;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.madl.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class SearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testAsyncSearchQueueUpdates() throws Exception {
		try (Tx tx = tx()) {

			Node node = folder("2015");
			String uuid = node.getUuid();
			SearchQueueBatch batch = MeshInternal.get().searchQueue().create();
			for (int i = 0; i < 10; i++) {
				String branchUuid = project().getLatestBranch().getUuid();
				batch.store(node, branchUuid, DRAFT, true);
			}

			String documentId = NodeGraphFieldContainer.composeDocumentId(node.getUuid(), "en");

			searchProvider().deleteDocument(Node.TYPE, documentId).blockingAwait();
			assertTrue("The document with uuid {" + uuid + "} could still be found within the search index. Used document id {" + documentId + "}",
					searchProvider().getDocument(Node.TYPE, documentId).blockingGet().isEmpty());
		}
	}

}
