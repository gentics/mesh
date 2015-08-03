package com.gentics.mesh.core.field.html;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.HtmlField;
import com.gentics.mesh.core.data.node.field.impl.basic.HtmlFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class HtmlFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleHTML() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		HtmlFieldImpl field = new HtmlFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-html"));
		field.setHtml("dummy HTML");
		assertEquals("dummy HTML", field.getHTML());
		assertEquals("dummy HTML", container.getProperty("test-html"));
		assertEquals(3, container.getPropertyKeys().size());
		field.setHtml(null);
		assertNull(field.getHTML());
		assertNull(container.getProperty("test-html"));
	}

	@Test
	public void testHTMLField() {

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		HtmlField htmlField = container.createHTML("htmlField");
		assertEquals("htmlField", htmlField.getFieldKey());
		htmlField.setHtml("dummyHTML");
		assertEquals("dummyHTML", htmlField.getHTML());
		HtmlField bogusField1 = container.getHtml("bogus");
		assertNull(bogusField1);
		HtmlField reloadedHTMLField = container.getHtml("htmlField");
		assertNotNull(reloadedHTMLField);
		assertEquals("htmlField", reloadedHTMLField.getFieldKey());

	}
}
