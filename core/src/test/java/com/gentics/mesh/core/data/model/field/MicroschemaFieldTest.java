package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.MicroschemaField;
import com.gentics.mesh.test.AbstractDBTest;

public class MicroschemaFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleMicroschema() {
		MeshNodeFieldContainerImpl container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);
		MicroschemaField gallery = container.createMicroschema("gallery");
		assertNotNull(gallery);
		gallery.setFieldLabel("dummyLabel");
		gallery.setFieldName("dummyName");

		assertEquals("gallery", gallery.getFieldKey());
		assertEquals("dummyName", gallery.getFieldName());
		assertEquals("dummyLabel", gallery.getFieldLabel());
		assertEquals(0, gallery.getFields().size());
	}
}
