package com.gentics.mesh.core.field.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class NumberGraphFieldTest extends AbstractEmptyDBTest {

	@Test
	public void testSimpleNumber() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldImpl field = new NumberGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-number"));
		assertEquals(2, container.getPropertyKeys().size());
		field.setNumber(42);
		assertEquals(42, field.getNumber());
		assertEquals(Integer.valueOf(42), container.getProperty("test-number"));
		assertEquals(3, container.getPropertyKeys().size());
		field.setNumber(null);
		assertNull(field.getNumber());
		assertNull(container.getProperty("test-number"));
	}

	@Test
	public void testNumberField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphField numberField = container.createNumber("numberField");
		assertEquals("numberField", numberField.getFieldKey());
		numberField.setNumber(42);
		assertEquals(42, numberField.getNumber());
		StringGraphField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		NumberGraphField reloadedNumberField = container.getNumber("numberField");
		assertNotNull(reloadedNumberField);
		assertEquals("numberField", reloadedNumberField.getFieldKey());
	}
}
