package com.gentics.mesh.core.field.microschema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.nesting.GraphMicroschemaField;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractDBTest;

public class MicroschemaGraphFieldTest extends AbstractDBTest {

	@Test
	@Ignore("Not yet implemented")
	public void testSimpleMicroschema() {
		try (Trx tx = new Trx(database)) {
			NodeFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			GraphMicroschemaField gallery = container.createMicroschema("gallery");
			assertNotNull(gallery);

			assertEquals("gallery", gallery.getFieldKey());
			assertEquals(0, gallery.getFields().size());

			gallery.createString("galleryName");
		}
	}
}
