package com.gentics.mesh.core.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.HTMLField;
import com.gentics.mesh.core.data.node.field.impl.basic.HTMLFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class HTMLFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleHTML() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		HTMLFieldImpl field = new HTMLFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
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

	@Test
	public void testHTMLField() {

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		HTMLField htmlField = container.createHTML("htmlField");
		assertEquals("htmlField", htmlField.getFieldKey());
		htmlField.setHTML("dummyHTML");
		assertEquals("dummyHTML", htmlField.getHTML());
		HTMLField bogusField1 = container.getHTML("bogus");
		assertNull(bogusField1);
		HTMLField reloadedHTMLField = container.getHTML("htmlField");
		assertNotNull(reloadedHTMLField);
		assertEquals("htmlField", reloadedHTMLField.getFieldKey());

	}
}
