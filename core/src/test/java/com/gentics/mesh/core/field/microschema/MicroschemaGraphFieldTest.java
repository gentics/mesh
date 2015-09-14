package com.gentics.mesh.core.field.microschema;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.nesting.MicroschemaGraphField;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class MicroschemaGraphFieldTest extends AbstractEmptyDBTest {

	@Test
	@Ignore("Not yet implemented")
	public void testSimpleMicroschema() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		MicroschemaGraphField gallery = container.createMicroschema("gallery");
		assertNotNull(gallery);

		assertEquals("gallery", gallery.getFieldKey());
		assertEquals(0, gallery.getFields().size());

		gallery.createString("galleryName");
	}
}
