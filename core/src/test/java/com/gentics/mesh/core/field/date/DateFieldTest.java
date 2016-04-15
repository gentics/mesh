package com.gentics.mesh.core.field.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.DateGraphFieldImpl;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class DateFieldTest extends AbstractFieldTest {

	private static final String DATE_FIELD = "dateField";
	
	@Test
	public void testDateFieldTransformation() throws Exception {
		Node node = folder("2015");
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
		dateFieldSchema.setName(DATE_FIELD);
		dateFieldSchema.setLabel("Some date field");
		dateFieldSchema.setRequired(true);
		schema.addField(dateFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		DateGraphField field = container.createDate(DATE_FIELD);
		field.setDate(1337L);

		String json = getJson(node);
		assertTrue("The json should contain the date but it did not.{" + json + "}", json.indexOf("1337") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		com.gentics.mesh.core.rest.node.field.DateField deserializedNodeField = response.getFields().getDateField("dateField");
		assertNotNull(deserializedNodeField);
		assertEquals(1337L, deserializedNodeField.getDate().longValue());
	}

	@Test
	public void testSimpleDate() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldImpl field = new DateGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-date"));
		field.setDate(nowEpoch);
		assertEquals(nowEpoch, Long.valueOf(container.getProperty("test-date")));
		assertEquals(3, container.getPropertyKeys().size());
		field.setDate(null);
		assertNull(container.getProperty("test-date"));
	}

	@Test
	public void testDateField() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphField dateField = container.createDate(DATE_FIELD);
		assertEquals(DATE_FIELD, dateField.getFieldKey());
		dateField.setDate(nowEpoch);
		assertEquals(nowEpoch, Long.valueOf(dateField.getDate()));
		StringGraphField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		DateGraphField reloadedDateField = container.getDate(DATE_FIELD);
		assertNotNull(reloadedDateField);
		assertEquals(DATE_FIELD, reloadedDateField.getFieldKey());
	}

	@Test
	@Override
	public void testClone() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphField dateField = container.createDate(DATE_FIELD);
		dateField.setDate(nowEpoch);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		dateField.cloneTo(otherContainer);

		assertThat(otherContainer.getDate(DATE_FIELD)).as("cloned field").isNotNull()
				.isEqualToIgnoringGivenFields(dateField, "parentContainer");
	}
}
