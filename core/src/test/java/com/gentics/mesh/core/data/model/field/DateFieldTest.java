package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.impl.DateFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class DateFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleDate() {
		MeshNodeFieldContainerImpl container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);
		DateFieldImpl field = new DateFieldImpl("test", container);
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
