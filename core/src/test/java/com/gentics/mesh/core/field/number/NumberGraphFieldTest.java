package com.gentics.mesh.core.field.number;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.NumberGraphField;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.NumberGraphFieldImpl;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.schema.NumberFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NumberFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class NumberGraphFieldTest extends AbstractBasicDBTest {

	@Test
	public void testNumberFieldTransformation() throws Exception {
		Node node = folder("2015");

		// Update the schema
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		NumberFieldSchema numberFieldSchema = new NumberFieldSchemaImpl();
		numberFieldSchema.setName("numberField");
		//		numberFieldSchema.setMin(10);
		//		numberFieldSchema.setMax(1000);
		numberFieldSchema.setRequired(true);
		schema.addField(numberFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NumberGraphField numberField = container.createNumber("numberField");
		numberField.setNumber(100.9f);

		String json = getJson(node);
		assertTrue("Could not find number within json. Json {" + json + "}", json.indexOf("100.9") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);
		NumberFieldImpl deserializedNumberField = response.getFields().getNumberField("numberField");
		assertEquals(100.9, deserializedNumberField.getNumber());
	}

	@Test
	public void testSimpleNumber() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldImpl field = new NumberGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-number"));
		assertEquals(2, container.getPropertyKeys().size());
		field.setNumber(42);
		assertEquals(42L, field.getNumber());
		assertEquals("42", container.getProperty("test-number"));
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
		assertEquals(42L, numberField.getNumber());
		StringGraphField bogusField1 = container.getString("bogus");
		assertNull(bogusField1);
		NumberGraphField reloadedNumberField = container.getNumber("numberField");
		assertNotNull(reloadedNumberField);
		assertEquals("numberField", reloadedNumberField.getFieldKey());
	}
}
