package com.gentics.mesh.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.search.impl.DummySearchProvider;
import com.gentics.mesh.search.index.NodeIndexHandler;

public class NodeIndexHandlerTest extends AbstractBasicDBTest {

	@Autowired
	protected DummySearchProvider searchProvider;

	@Autowired
	private NodeIndexHandler handler;

	@Test
	public void testReindexAll() throws Exception {
		searchProvider.reset();
		assertEquals("Initially no store event should have been recorded.", 0, searchProvider.getStoreEvents().size());
		handler.reindexAll();
		assertTrue("We expected to see more than one store event.", searchProvider.getStoreEvents().size() > 1);

		for (String key : searchProvider.getStoreEvents().keySet()) {
			if (!key.startsWith("node")) {
				fail("We found a document which was does not represent a node. Only nodes should have been reindexed. {" + key + "}");
			}
		}
	}

}
