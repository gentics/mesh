package com.gentics.mesh.core.field.html;

import static com.gentics.mesh.core.field.html.HtmlListFieldHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.html.HtmlListFieldHelper.FETCH;
import static com.gentics.mesh.core.field.html.HtmlListFieldHelper.FILLTEXT;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class HtmlListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String HTML_LIST = "htmlList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("html");
		schema.setName(HTML_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		//		setupData();
		Node node = folder("2015");
		Schema schema = prepareNode(node, "nodeList", "node");

		ListFieldSchema htmlListFieldSchema = new ListFieldSchemaImpl();
		htmlListFieldSchema.setName(HTML_LIST);
		htmlListFieldSchema.setListType("html");
		schema.addField(htmlListFieldSchema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		HtmlGraphFieldList htmlList = container.createHTMLList(HTML_LIST);
		htmlList.createHTML("some<b>html</b>");
		htmlList.createHTML("some<b>more html</b>");

		NodeResponse response = transform(node);
		assertList(2, HTML_LIST, "html", response);

	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList list = container.createHTMLList("dummyList");
		assertNotNull(list);
		HtmlGraphField htmlField = list.createHTML("HTML 1");
		assertNotNull(htmlField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList testField = container.createHTMLList("testField");
		testField.createHTML("<b>One</b>");
		testField.createHTML("<i>Two</i>");
		testField.createHTML("<u>Three</u>");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getHTMLList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

	@Test
	@Override
	public void testEquals() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList fieldA = container.createHTMLList("fieldA");
		HtmlGraphFieldList fieldB = container.createHTMLList("fieldB");
		assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
		fieldA.addItem(fieldA.createHTML("testHtml"));
		assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

		assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
		assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
		fieldB.addItem(fieldB.createHTML("testHtml"));
		assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
	}

	@Test
	@Override
	public void testEqualsNull() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList fieldA = container.createHTMLList("fieldA");
		assertFalse(fieldA.equals((Field) null));
		assertFalse(fieldA.equals((GraphField) null));
	}

	@Test
	@Override
	public void testEqualsRestField() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		String dummyValue = "test123";

		// rest null - graph null
		HtmlGraphFieldList fieldA = container.createHTMLList(HTML_LIST);

		HtmlFieldListImpl restField = new HtmlFieldListImpl();
		assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

		// rest set - graph set - different values
		fieldA.addItem(fieldA.createHTML(dummyValue));
		restField.add(dummyValue + 1L);
		assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

		// rest set - graph set - same value
		restField.getItems().clear();
		restField.add(dummyValue);
		assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

		StringFieldListImpl otherTypeRestField = new StringFieldListImpl();
		otherTypeRestField.add(dummyValue);
		// rest set - graph set - same value different type
		assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		invokeUpdateFromRestTestcase(HTML_LIST, FETCH, CREATE_EMPTY);
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		invokeUpdateFromRestNullOnCreateRequiredTestcase(HTML_LIST, FETCH);
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		InternalActionContext ac = getMockedInternalActionContext();
		invokeRemoveFieldViaNullTestcase(HTML_LIST, FETCH, FILLTEXT, (node) -> {
			updateContainer(ac, node, HTML_LIST, null);
		});
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		InternalActionContext ac = getMockedInternalActionContext();
		invokeRemoveRequiredFieldViaNullTestcase(HTML_LIST, FETCH, FILLTEXT, (container) -> {
			updateContainer(ac, container, HTML_LIST, null);
		});
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		InternalActionContext ac = getMockedInternalActionContext();
		invokeUpdateFromRestValidSimpleValueTestcase(HTML_LIST, FILLTEXT, (container) -> {
			HtmlFieldListImpl field = new HtmlFieldListImpl();
			field.getItems().add("someValue");
			field.getItems().add("someValue2");
			updateContainer(ac, container, HTML_LIST, field);
		} , (container) -> {
			HtmlGraphFieldList field = container.getHTMLList(HTML_LIST);
			assertNotNull("The graph field {" + HTML_LIST + "} could not be found.", field);
			assertEquals("The list of the field was not updated.", 2, field.getList().size());
			assertEquals("The list item of the field was not updated.", "someValue", field.getList().get(0).getHTML());
			assertEquals("The list item of the field was not updated.", "someValue2", field.getList().get(1).getHTML());
		});
	}

}
