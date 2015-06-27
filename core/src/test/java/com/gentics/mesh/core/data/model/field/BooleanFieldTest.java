package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.MeshNodeFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class BooleanFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleBoolean() {
		MeshNodeFieldContainerImpl container = fg.addFramedVertex(MeshNodeFieldContainerImpl.class);
		BooleanFieldImpl field = new BooleanFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setFieldLabel("dummyLabel");
		field.setFieldName("dummyName");
		assertEquals(null, container.getProperty("test-boolean"));
		assertEquals(4, container.getPropertyKeys().size());
		field.setBoolean(new Boolean(true));

		assertEquals("true", container.getProperty("test-boolean"));
		assertEquals(5, container.getPropertyKeys().size());
		field.setBoolean(new Boolean(false));
		assertEquals("false", container.getProperty("test-boolean"));
		field.setBoolean(null);
		assertNull(container.getProperty("test-boolean"));
	}
}
