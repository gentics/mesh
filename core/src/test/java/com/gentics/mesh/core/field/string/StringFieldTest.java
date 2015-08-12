package com.gentics.mesh.core.field.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.StringGraphFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class StringFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleString() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		StringGraphFieldImpl field = new StringGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setString("dummyString");
		assertEquals("dummyString", field.getString());
	}

	@Test
	public void testStringField() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		StringGraphField stringField = container.createString("stringField");
		assertEquals("stringField", stringField.getFieldKey());
		stringField.setString("dummyString");
		assertEquals("dummyString", stringField.getString());
		StringGraphField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		StringGraphField reloadedStringField = container.getString("stringField");
		assertNotNull(reloadedStringField);
		assertEquals("stringField", reloadedStringField.getFieldKey());
	}

}
