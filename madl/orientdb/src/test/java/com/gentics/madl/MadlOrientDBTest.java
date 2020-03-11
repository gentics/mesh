package com.gentics.madl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.gentics.madl.ext.orientdb.OrientDBTypeResolver;
import com.gentics.madl.test.model.TestVertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraphFactory;

public class MadlOrientDBTest {

	@Test
	public void testSetup() {
		OrientDBTypeResolver resolver = new OrientDBTypeResolver("com.gentics.madl.test.model");
		OrientGraphFactory factory = new OrientGraphFactory("memory:tinkerpop" + System.currentTimeMillis()).setupPool(16, 100);
		try (OrientDBTx tx = new OrientDBTx(factory, resolver)) {
			TestVertex t = tx.getGraph().addFramedVertex(TestVertex.class);
			t.setName("Blub");
			assertEquals("Blub", t.getName());
		}

	}
}
