package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.impl.HTMLFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class HTMLFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleHTML() {
		MeshNodeFieldContainerImpl container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);
		HTMLFieldImpl field = new HTMLFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setFieldLabel("dummyLabel");
		field.setFieldName("dummyName");
		assertEquals(null, container.getProperty("test-html"));
		assertEquals(4, container.getPropertyKeys().size());
		field.setHTML("dummy HTML");
		assertEquals("dummy HTML", field.getHTML());
		assertEquals("dummy HTML", container.getProperty("test-html"));
		assertEquals(5, container.getPropertyKeys().size());
		field.setHTML(null);
		assertNull(field.getHTML());
		assertNull(container.getProperty("test-html"));
	}
}
