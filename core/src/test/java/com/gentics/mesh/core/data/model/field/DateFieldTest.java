package com.gentics.mesh.core.data.model.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.gentics.mesh.core.data.model.impl.AbstractFieldContainerImpl;
import com.gentics.mesh.core.data.model.node.field.basic.DateField;
import com.gentics.mesh.core.data.model.node.field.basic.StringField;
import com.gentics.mesh.core.data.model.node.field.impl.basic.DateFieldImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class DateFieldTest extends AbstractDBTest {

	@Test
	public void testSimpleDate() {
		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
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

	@Test
	public void testDateField() {

		AbstractFieldContainerImpl container = fg.addFramedVertex(AbstractFieldContainerImpl.class);
		DateField dateField = container.createDate("dateField");
		assertEquals("dateField", dateField.getFieldKey());
		dateField.setFieldLabel("dateLabel");
		assertEquals("dateLabel", dateField.getFieldLabel());
		dateField.setFieldName("dateName");
		assertEquals("dateName", dateField.getFieldName());
		dateField.setDate("dummyDate");
		assertEquals("dummyDate", dateField.getDate());
		StringField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		DateField reloadedDateField = container.getDate("dateField");
		assertNotNull(reloadedDateField);
		assertEquals("dateLabel", reloadedDateField.getFieldLabel());
		assertEquals("dateField", reloadedDateField.getFieldKey());
		assertEquals("dateName", reloadedDateField.getFieldName());

	}
}
