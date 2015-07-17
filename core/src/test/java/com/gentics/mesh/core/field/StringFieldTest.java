package com.gentics.mesh.core.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.StringField;
import com.gentics.mesh.core.data.node.field.impl.basic.StringFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class StringFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleString() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		StringFieldImpl field = new StringFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setString("dummyString");
		assertEquals("dummyString", field.getString());
	}

	@Test
	public void testStringField() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		StringField stringField = container.createString("stringField");
		assertEquals("stringField", stringField.getFieldKey());
		stringField.setString("dummyString");
		assertEquals("dummyString", stringField.getString());
		StringField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		StringField reloadedStringField = container.getString("stringField");
		assertNotNull(reloadedStringField);
		assertEquals("stringField", reloadedStringField.getFieldKey());
	}

}
