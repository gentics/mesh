package com.gentics.mesh.core.field.string;

import static com.gentics.mesh.core.field.string.StringFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.string.StringFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.string.StringFieldTestHelper.FILLTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.data.node.field.impl.StringGraphFieldImpl;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
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
	@Override
	public void testClone() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphField testField = container.createString("testField");
		testField.setString("this is the string");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getString("testField")).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(testField, "parentContainer");
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
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
	public void testEquals() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		String testValue = "test123";
		StringGraphField fieldA = container.createString(STRING_FIELD);
		StringGraphField fieldB = container.createString(STRING_FIELD + "_2");
		fieldA.setString(testValue);
		fieldB.setString(testValue);
		assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
	}

	@Test
	@Override
	public void testEqualsNull() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphField fieldA = container.createString(STRING_FIELD);
		StringGraphField fieldB = container.createString(STRING_FIELD + "_2");
		assertTrue("Both fields should be equal to eachother", fieldA.equals(fieldB));
	}

	@Test
	@Override
	public void testEqualsRestField() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		String dummyValue = "test123";

		// rest null - graph null
		StringGraphField fieldA = container.createString(STRING_FIELD);
		StringFieldImpl restField = new StringFieldImpl();
		assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

		// rest set - graph set - different values
		fieldA.setString(dummyValue);
		restField.setString(dummyValue + 1L);
		assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

		// rest set - graph set - same value
		restField.setString(dummyValue);
		assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

		// rest set - graph set - same value different type
		assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(new HtmlFieldImpl().setHTML(dummyValue)));

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		invokeUpdateFromRestTestcase(STRING_FIELD, FETCH, CREATE_EMPTY);
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		invokeUpdateFromRestNullOnCreateRequiredTestcase(STRING_FIELD, FETCH);
	}

	@Test
	@Override
	public void testRemoveFieldViaNullValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeRemoveFieldViaNullValueTestcase(STRING_FIELD, FETCH, CREATE_EMPTY, (node) -> {
			StringField field = new StringFieldImpl();
			field.setString(null);
			updateContainer(ac, node, STRING_FIELD, field);
		});
	}

	@Test
	@Override
	public void testDeleteRequiredFieldViaNullValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeDeleteRequiredFieldViaNullValueTestcase(STRING_FIELD, FETCH, FILLTEXT, (container) -> {
			StringField field = new StringFieldImpl();
			field.setString(null);
			updateContainer(ac, container, STRING_FIELD, field);
		});
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeUpdateFromRestValidSimpleValueTestcase(STRING_FIELD, FILLTEXT, (container) -> {
			StringField field = new StringFieldImpl();
			field.setString("someValue");
			updateContainer(ac, container, STRING_FIELD, field);
		} , (container) -> {
			StringGraphField field = container.getString(STRING_FIELD);
			assertNotNull("The graph field {" + STRING_FIELD + "} could not be found.", field);
			assertEquals("The string of the field was not updated.", "someValue", field.getString());
		});
	}
}
