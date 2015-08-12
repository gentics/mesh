package com.gentics.mesh.core.field.date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.basic.DateGraphFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class DateFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleDate() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		DateGraphFieldImpl field = new DateGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-date"));
		field.setDate("dummyDate");
		assertEquals("dummyDate", container.getProperty("test-date"));
		assertEquals(3, container.getPropertyKeys().size());
		field.setDate(null);
		assertNull(container.getProperty("test-date"));
	}

	@Test
	public void testDateField() {

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		DateGraphField dateField = container.createDate("dateField");
		assertEquals("dateField", dateField.getFieldKey());
		dateField.setDate("dummyDate");
		assertEquals("dummyDate", dateField.getDate());
		StringGraphField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		DateGraphField reloadedDateField = container.getDate("dateField");
		assertNotNull(reloadedDateField);
		assertEquals("dateField", reloadedDateField.getFieldKey());

	}
}
