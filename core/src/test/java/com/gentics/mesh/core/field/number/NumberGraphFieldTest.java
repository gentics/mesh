package com.gentics.mesh.core.field.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.NumberGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.NumberGraphFieldImpl;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class NumberGraphFieldTest extends AbstractEmptyDBTest {

	@Test
	public void testSimpleNumber() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldImpl field = new NumberGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-number"));
		assertEquals(2, container.getPropertyKeys().size());
		field.setNumber("dummy number");
		assertEquals("dummy number", field.getNumber());
		assertEquals("dummy number", container.getProperty("test-number"));
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
		numberField.setNumber("dummyNumber");
		assertEquals("dummyNumber", numberField.getNumber());
		StringGraphField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		NumberGraphField reloadedNumberField = container.getNumber("numberField");
		assertNotNull(reloadedNumberField);
		assertEquals("numberField", reloadedNumberField.getFieldKey());
	}
}
