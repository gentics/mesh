package com.gentics.mesh.core.field.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeFieldListItem;
import com.gentics.mesh.core.rest.node.field.list.impl.BooleanFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.DateFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NumberFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.StringFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class GraphListFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		setSchema("node");
	}

	private void setSchema(String listType) throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName("listField");
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType(listType);
		schema.addField(listFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(null, (Field) null);
		NodeFieldListImpl nodeField = response.getField("listField");
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		setSchema("string");
		NodeResponse response = createNode("listField", (Field) null);
		StringFieldListImpl nodeField = response.getField("listField");
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateEmptyStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		NodeResponse response = createNode("listField", listField);
		StringFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateNullStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.setItems(null);
		NodeResponse response = createNode("listField", listField);
		StringFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateWithOmittedStringListValue() throws IOException {
		setSchema("string");
		NodeResponse response = createNode(null, (Field) null);
		StringFieldListImpl listFromResponse = response.getField("listField");
		assertNotNull(listFromResponse);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNode("listField", listField);
		StringFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());
		assertEquals(Arrays.asList("A", "B", "C").toString(), listFromResponse.getItems().toString());
	}

	@Test
	public void testHtmlList() throws IOException {
		setSchema("html");
		HtmlFieldListImpl listField = new HtmlFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNode("listField", listField);
		HtmlFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testBooleanList() throws IOException {
		setSchema("boolean");
		BooleanFieldListImpl listField = new BooleanFieldListImpl();
		listField.add(true);
		listField.add(false);
		listField.add(null);

		NodeResponse response = createNode("listField", listField);
		BooleanFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testDateList() throws IOException {
		setSchema("date");
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add((System.currentTimeMillis() / 1000) + 1);
		listField.add((System.currentTimeMillis() / 1000) + 2);
		listField.add((System.currentTimeMillis() / 1000) + 3);

		NodeResponse response = createNode("listField", listField);
		DateFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testNumberList() throws IOException {
		setSchema("number");
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(0.1);
		listField.add(1337);
		listField.add(42);

		NodeResponse response = createNode("listField", listField);
		NumberFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("news");
		Node node2 = folder("deals");

		NodeFieldListImpl list = new NodeFieldListImpl();
		list.add(new NodeFieldListItemImpl(node.getUuid()));
		NodeResponse response = updateNode("listField", list);
		NodeFieldListImpl field = response.getField("listField");
		assertEquals(1, field.getItems().size());

		/// Add another item to the list and update the node
		list.add(new NodeFieldListItemImpl(node2.getUuid()));
		response = updateNode("listField", list);
		field = response.getField("listField");
		assertEquals(2, field.getItems().size());
	}

	@Test
	public void testUpdateNodeWithStringField() throws IOException {
		setSchema("string");

		StringFieldListImpl listField = new StringFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNode("listField", listField);
		StringFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add("D");
		response = updateNode("listField", listField);
		listFromResponse = response.getField("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithHtmlField() throws IOException {
		setSchema("html");

		HtmlFieldListImpl listField = new HtmlFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNode("listField", listField);
		HtmlFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add("D");
		response = updateNode("listField", listField);
		listFromResponse = response.getField("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithDateField() throws IOException {
		setSchema("date");

		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(1L);
		listField.add(2L);
		listField.add(3L);

		NodeResponse response = createNode("listField", listField);
		DateFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add(4L);
		response = updateNode("listField", listField);
		listFromResponse = response.getField("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithNumberField() throws IOException {
		setSchema("number");

		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(1.1);
		listField.add(1.2);
		listField.add(1.4);

		NodeResponse response = createNode("listField", listField);
		NumberFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add(1.6);
		response = updateNode("listField", listField);
		listFromResponse = response.getField("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeFieldListImpl listField = new NodeFieldListImpl();
		NodeFieldListItemImpl item = new NodeFieldListItemImpl().setUuid(folder("news").getUuid());
		listField.add(item);
		NodeResponse response = createNode("listField", listField);
		NodeFieldListImpl listFromResponse = response.getField("listField");
		assertEquals(1, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList("listField");
		nodeList.createNode("1", folder("news"));
		NodeResponse response = readNode(node);
		NodeFieldListImpl deserializedListField = response.getField("listField", NodeFieldListImpl.class);
		assertNotNull(deserializedListField);
		assertEquals(1, deserializedListField.getItems().size());
	}

	@Test
	public void testReadExpandedNodeListWithExistingField() throws IOException {
		resetClientSchemaStorage();
		Node newsNode = folder("news");
		Node node = folder("2015");

		// Create node list
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList("listField");
		nodeList.createNode("1", newsNode);

		// 1. Read node with collapsed fields and check that the collapsed node list item can be read
		NodeResponse responseCollapsed = readNode(node);
		com.gentics.mesh.core.rest.node.field.list.NodeFieldList deserializedNodeListField = responseCollapsed.getField("listField",
				NodeFieldListImpl.class);
		assertNotNull(deserializedNodeListField);
		assertEquals("The newsNode should be the first item in the list.", newsNode.getUuid(), deserializedNodeListField.getItems().get(0).getUuid());

		// Check whether it is possible to read the field in an expanded form.
		NodeResponse nodeListItem = (NodeResponse) deserializedNodeListField.getItems().get(0);
		assertNotNull(nodeListItem);

		// 2. Read node with expanded fields
		NodeResponse responseExpanded = readNode(node, "listField", "bogus");

		// Check collapsed node field
		deserializedNodeListField = responseExpanded.getField("listField", NodeFieldListImpl.class);
		assertNotNull(deserializedNodeListField);
		assertEquals(newsNode.getUuid(), deserializedNodeListField.getItems().get(0).getUuid());

		// Check expanded node field
		NodeFieldListItem deserializedExpandedItem = deserializedNodeListField.getItems().get(0);
		if (deserializedExpandedItem instanceof NodeResponse) {
			NodeResponse expandedField = (NodeResponse) deserializedExpandedItem;
			assertNotNull(expandedField);
			assertEquals(newsNode.getUuid(), expandedField.getUuid());
			assertNotNull(expandedField.getCreator());
		} else {
			fail("The returned item should be a NodeResponse object");
		}
	}

}
