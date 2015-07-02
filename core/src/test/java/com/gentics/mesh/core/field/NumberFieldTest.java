package com.gentics.mesh.core.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.NumberField;
import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.impl.basic.NumberFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class NumberFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleNumber() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		NumberFieldImpl field = new NumberFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-number"));
		assertEquals(4, container.getPropertyKeys().size());
		field.setNumber("dummy number");
		assertEquals("dummy number", field.getNumber());
		assertEquals("dummy number", container.getProperty("test-number"));
		assertEquals(5, container.getPropertyKeys().size());
		field.setNumber(null);
		assertNull(field.getNumber());
		assertNull(container.getProperty("test-number"));
	}

	@Test
	public void testNumberField() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		NumberField numberField = container.createNumber("numberField");
		assertEquals("numberField", numberField.getFieldKey());
		numberField.setNumber("dummyNumber");
		assertEquals("dummyNumber", numberField.getNumber());
		StringField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		NumberField reloadedNumberField = container.getNumber("numberField");
		assertNotNull(reloadedNumberField);
		assertEquals("numberField", reloadedNumberField.getFieldKey());

	}
}
