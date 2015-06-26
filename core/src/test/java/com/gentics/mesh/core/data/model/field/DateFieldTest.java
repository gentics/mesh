package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.node.MeshNodeFieldContainer;
import com.gentics.mesh.core.data.model.node.field.DateField;
import com.gentics.mesh.test.AbstractDBTest;

public class DateFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleDate() {
		MeshNodeFieldContainer container = fg.addFramedVertex(MeshNodeFieldContainer.class);
		DateField field = new DateField("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setFieldLabel("dummyLabel");
		field.setFieldName("dummyName");
		assertEquals(null, container.getProperty("test-date"));
		assertEquals(4, container.getPropertyKeys().size());
		field.setDate("dummyDate");
		assertEquals("dummyDate", container.getProperty("test-date"));
		assertEquals(5, container.getPropertyKeys().size());
		field.setDate(null);
		assertNull(container.getProperty("test-date"));
	}
}
