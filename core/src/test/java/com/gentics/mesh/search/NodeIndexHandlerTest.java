package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = false)
public class NodeIndexHandlerTest extends AbstractMeshTest {

	@Test
	public void testReindexAll() throws Exception {
		try (Tx tx = tx()) {
			assertTrue(meshRoot().getNodeRoot().findAllIt().iterator().hasNext());
			searchProvider().reset();
			assertEquals("Initially no store event should have been recorded.", 0,
				trackingSearchProvider().getStoreEvents().size());
			meshDagger().nodeContainerIndexHandler().reindexAll().blockingAwait();
			assertTrue("We expected to see more than one store event.",
				trackingSearchProvider().getStoreEvents().size() > 1);
		}

		for (String key : trackingSearchProvider().getStoreEvents().keySet()) {
			if (!key.startsWith("mesh-node")) {
				fail("We found a document which was does not represent a node. Only nodes should have been reindexed. {"
					+ key + "}");
			}
		}
	}

}
