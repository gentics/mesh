package com.gentics.mesh.core.field.html;

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
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.HtmlField;
import com.gentics.mesh.core.rest.node.field.impl.HtmlFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class HtmlFieldTest extends AbstractFieldTest<HtmlFieldSchema> {

	private static final String HTML_FIELD = "htmlField";

	@Override
	protected HtmlFieldSchema createFieldSchema(boolean isRequired) {
		HtmlFieldSchemaImpl schema = new HtmlFieldSchemaImpl();
		schema.setLabel("Some html field");
		schema.setRequired(isRequired);
		schema.setName(HTML_FIELD);
		return schema;
	}

	@Test
	@Override
	public void testFieldUpdate() {
		// Create field
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField htmlField = container.createHTML(HTML_FIELD);

		// Check field key
		assertEquals(HTML_FIELD, htmlField.getFieldKey());

		// Check field value
		htmlField.setHtml("dummyHTML");
		assertEquals("dummyHTML", htmlField.getHTML());

		// Check bogus key
		HtmlGraphField bogusField1 = container.getHtml("bogus");
		assertNull(bogusField1);

		// Test field loading
		HtmlGraphField reloadedHTMLField = container.getHtml(HTML_FIELD);
		assertNotNull(reloadedHTMLField);
		assertEquals(HTML_FIELD, reloadedHTMLField.getFieldKey());
		assertEquals("dummyHTML", reloadedHTMLField.getHTML());
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		setupData();
		Node node = folder("2015");

		// Add html field schema to the schema
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		HtmlFieldSchemaImpl htmlFieldSchema = new HtmlFieldSchemaImpl();
		htmlFieldSchema.setName(HTML_FIELD);
		htmlFieldSchema.setLabel("Some html field");
		htmlFieldSchema.setRequired(true);
		schema.addField(htmlFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		HtmlGraphField field = container.createHTML(HTML_FIELD);
		field.setHtml("Some<b>htmlABCDE");

		String json = getJson(node);
		assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("ABCDE") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		com.gentics.mesh.core.rest.node.field.HtmlField deserializedNodeField = response.getFields().getHtmlField("htmlField");
		assertNotNull(deserializedNodeField);
		assertEquals("Some<b>htmlABCDE", deserializedNodeField.getHTML());

	}

	@Override
	public void testRemoveFieldViaNullValue() {
		InternalActionContext ac = getMockedInternalActionContext("");
		invokeRemoveFieldViaNullValueTestcase(HTML_FIELD, (container, fieldName) -> {
			return container.getHtml(fieldName);
		} , (container) -> {
			return container.createHTML(HTML_FIELD);
		} , (node) -> {
			HtmlField field = new HtmlFieldImpl();
			field.setHTML(null);
			updateNode(ac, node, HTML_FIELD, field);
		});
	}

	@Test
	@Override
	public void testEquals() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField fieldA = container.createHTML("htmlField1");
		HtmlGraphField fieldB = container.createHTML("htmlField2");
		assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
		fieldA.setHtml("someText");
		assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

		assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
		assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
		fieldB.setHtml("someText");
		assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));

	}

	@Test
	@Override
	public void testEqualsNull() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField fieldA = container.createHTML("htmlField1");
		assertFalse(fieldA.equals((Field) null));
		assertFalse(fieldA.equals((GraphField) null));
	}

	@Test
	@Override
	public void testEqualsRestField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField fieldA = container.createHTML("htmlField1");
		assertFalse("The html field should not be equal to a string rest field. Even if it has the same value",
				fieldA.equals(new StringFieldImpl().setString("someText")));

		assertTrue("The html field should be equal to the html rest field since both fields have no value.", fieldA.equals(new HtmlFieldImpl()));

		fieldA.setHtml("someText");
		assertFalse("The html field should not be equal to the html rest field since the rest field has a different value.",
				fieldA.equals(new HtmlFieldImpl().setHTML("someText2")));
		assertTrue("The html field should be equal to a html rest field with the same value", fieldA.equals(new HtmlFieldImpl().setHTML("someText")));
	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphField htmlField = container.createHTML(HTML_FIELD);
		htmlField.setHtml("<i>HTML</i>");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		htmlField.cloneTo(otherContainer);

		assertThat(otherContainer.getHtml(HTML_FIELD)).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(htmlField, "parentContainer");
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
	public void testDeleteRequiredFieldViaNullValue() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void testUpdateFromRestValidSimpleValue() {
		// TODO Auto-generated method stub
		
	}

}
