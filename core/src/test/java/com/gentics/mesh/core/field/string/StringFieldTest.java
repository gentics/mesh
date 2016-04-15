package com.gentics.mesh.core.field.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class StringFieldTest extends AbstractFieldTest<StringFieldSchema> {

	private static final String STRING_FIELD = "stringField";

	@Override
	protected StringFieldSchema createFieldSchema(boolean isRequired) {
		StringFieldSchema schema = new StringFieldSchemaImpl();
		schema.setLabel("Some string field");
		schema.setRequired(isRequired);
		schema.setName(STRING_FIELD);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node node = folder("2015");

		// Add a new string field to the schema
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		StringFieldSchemaImpl stringFieldSchema = new StringFieldSchemaImpl();
		stringFieldSchema.setName("stringField");
		stringFieldSchema.setLabel("Some string field");
		stringFieldSchema.setRequired(true);
		schema.addField(stringFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		StringGraphField field = container.createString("stringField");
		field.setString("someString");

		String json = getJson(node);
		assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("someString") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		com.gentics.mesh.core.rest.node.field.StringField deserializedNodeField = response.getFields().getStringField("stringField");
		assertNotNull(deserializedNodeField);
		assertEquals("someString", deserializedNodeField.getString());
	}

	@Test
	public void testSimpleString() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphFieldImpl field = new StringGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		field.setString("dummyString");
		assertEquals("dummyString", field.getString());
	}

	@Test
	public void testStringField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
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

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphField testField = container.createString("testField");
		testField.setString("this is the string");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getString("testField")).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(testField, "parentContainer");
	}

	@Override
	public void testFieldUpdate() throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testEquals() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testEqualsNull() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testEqualsRestField() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestNullOnCreate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testRemoveFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testDeleteRequiredFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		// TODO Auto-generated method stub
		
	}
}
