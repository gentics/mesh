package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class NumberFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNumber() {
		MeshNodeFieldContainerImpl container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);
		NumberFieldImpl field = new NumberFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setFieldLabel("dummyLabel");
		field.setFieldName("dummyName");
		assertEquals(null, container.getProperty("test-number"));
		assertEquals(4, container.getPropertyKeys().size());
		field.setNumber("dummy number");
		assertEquals("dummy number", field.getNumber());
		assertEquals("dummy number", container.getProperty("test-number"));
		assertEquals(5, container.getPropertyKeys().size());
		field.setNumber(null);
		assertNull(field.getNumber());
		assertNull(container.getProperty("test-number"));
	}
}
