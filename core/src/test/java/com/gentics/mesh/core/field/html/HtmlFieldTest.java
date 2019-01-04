package com.gentics.mesh.core.field.html;

import static com.gentics.mesh.core.field.html.HtmlFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.html.HtmlFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.html.HtmlFieldTestHelper.FILLTEXT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
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
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.PROJECT_AND_NODE, startServer = false)
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
		try (Tx tx = tx()) {
			// Create field
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
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
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node node = folder("2015");
		try (Tx tx = tx()) {

			// Add html field schema to the schema
			SchemaModel schema = node.getSchemaContainer().getLatestVersion().getSchema();
			HtmlFieldSchemaImpl htmlFieldSchema = new HtmlFieldSchemaImpl();
			htmlFieldSchema.setName(HTML_FIELD);
			htmlFieldSchema.setLabel("Some html field");
			htmlFieldSchema.setRequired(true);
			schema.addField(htmlFieldSchema);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			HtmlGraphField field = container.createHTML(HTML_FIELD);
			field.setHtml("Some<b>htmlABCDE");
			tx.success();
		}

		try (Tx tx = tx()) {
			String json = getJson(node);
			assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("ABCDE") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			com.gentics.mesh.core.rest.node.field.HtmlField deserializedNodeField = response.getFields().getHtmlField("htmlField");
			assertNotNull(deserializedNodeField);
			assertEquals("Some<b>htmlABCDE", deserializedNodeField.getHTML());
		}

	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			HtmlGraphField fieldA = container.createHTML("fieldA");
			HtmlGraphField fieldB = container.createHTML("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.setHtml("someText");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.setHtml("someText");
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			HtmlGraphField fieldA = container.createHTML("htmlField1");
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			HtmlGraphField fieldA = container.createHTML("htmlField1");

			// graph empty - rest empty
			assertTrue("The html field should be equal to the html rest field since both fields have no value.", fieldA.equals(new HtmlFieldImpl()));

			// graph set - rest set - same value - different type
			fieldA.setHtml("someText");
			assertFalse("The html field should not be equal to a string rest field. Even if it has the same value",
					fieldA.equals(new StringFieldImpl().setString("someText")));
			// graph set - rest set - different value
			assertFalse("The html field should not be equal to the html rest field since the rest field has a different value.",
					fieldA.equals(new HtmlFieldImpl().setHTML("someText2")));

			// graph set - rest set - same value
			assertTrue("The html field should be equal to a html rest field with the same value",
					fieldA.equals(new HtmlFieldImpl().setHTML("someText")));
		}
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.createVertex(NodeGraphFieldContainerImpl.class);
			HtmlGraphField htmlField = container.createHTML(HTML_FIELD);
			htmlField.setHtml("<i>HTML</i>");

			NodeGraphFieldContainerImpl otherContainer = tx.createVertex(NodeGraphFieldContainerImpl.class);
			htmlField.cloneTo(otherContainer);

			assertThat(otherContainer.getHtml(HTML_FIELD)).as("cloned field").isNotNull().isEqualToIgnoringGivenFields(htmlField, "parentContainer");
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(HTML_FIELD, FETCH, FILLTEXT, (node) -> {
				updateContainer(ac, node, HTML_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(HTML_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(HTML_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(HTML_FIELD, FETCH, FILLTEXT, (container) -> {
				updateContainer(ac, container, HTML_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(HTML_FIELD, FILLTEXT, (container) -> {
				HtmlField field = new HtmlFieldImpl();
				field.setHTML("someValue");
				updateContainer(ac, container, HTML_FIELD, field);
			}, (container) -> {
				HtmlGraphField field = container.getHtml(HTML_FIELD);
				assertNotNull("The graph field {" + HTML_FIELD + "} could not be found.", field);
				assertEquals("The html of the field was not updated.", "someValue", field.getHTML());
			});
		}
	}

}
