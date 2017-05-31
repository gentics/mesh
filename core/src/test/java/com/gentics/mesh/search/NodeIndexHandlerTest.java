package com.gentics.mesh.search;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = false)
public class NodeIndexHandlerTest extends AbstractMeshTest {

	@Test
	public void testReindexAll() throws Exception {
		try (Tx tx = db().tx()) {
			assertThat(meshRoot().getNodeRoot().findAll()).as("Node list").isNotEmpty();
			searchProvider().reset();
			assertEquals("Initially no store event should have been recorded.", 0,
					dummySearchProvider().getStoreEvents().size());
			meshDagger().nodeContainerIndexHandler().reindexAll().await();
			assertTrue("We expected to see more than one store event.",
					dummySearchProvider().getStoreEvents().size() > 1);
		}

		for (String key : dummySearchProvider().getStoreEvents().keySet()) {
			if (!key.startsWith("node")) {
				fail("We found a document which was does not represent a node. Only nodes should have been reindexed. {"
						+ key + "}");
			}
		}
	}

}
