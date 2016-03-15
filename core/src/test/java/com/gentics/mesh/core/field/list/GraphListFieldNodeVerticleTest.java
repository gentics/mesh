package com.gentics.mesh.core.field.list;

import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
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
import com.gentics.mesh.core.rest.node.field.list.NodeFieldList;
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

import io.vertx.core.Future;

public class GraphListFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		setSchema("node");
	}

	private void setSchema(String listType) throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName("listField");
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType(listType);
		schema.removeField("listField");
		schema.addField(listFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNodeAndCheck(null, (Field) null);
		NodeFieldListImpl nodeField = response.getFields().getNodeListField("listField");
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		setSchema("string");
		NodeResponse response = createNodeAndCheck("listField", (Field) null);
		StringFieldListImpl nodeField = response.getFields().getStringFieldList("listField");
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateEmptyStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		NodeResponse response = createNodeAndCheck("listField", listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList("listField");
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateNullStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.setItems(null);
		NodeResponse response = createNodeAndCheck("listField", listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList("listField");
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateWithOmittedStringListValue() throws IOException {
		setSchema("string");
		NodeResponse response = createNodeAndCheck(null, (Field) null);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList("listField");
		assertNotNull(listFromResponse);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testBogusNodeList() throws IOException {
		setSchema("node");

		NodeFieldListImpl listField = new NodeFieldListImpl();
		listField.add(new NodeFieldListItemImpl("bogus"));

		Future<NodeResponse> future = createNode("listField", listField);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_list_item_not_found", "bogus");
	}

	@Test
	public void testValidNodeList() throws IOException {
		setSchema("node");

		NodeFieldListImpl listField = new NodeFieldListImpl();
		listField.add(new NodeFieldListItemImpl(content().getUuid()));
		listField.add(new NodeFieldListItemImpl(folder("news").getUuid()));

		NodeResponse response = createNodeAndCheck("listField", listField);

		NodeFieldList listFromResponse = response.getFields().getNodeFieldList("listField");
		assertEquals(2, listFromResponse.getItems().size());
		assertEquals(content().getUuid(), listFromResponse.getItems().get(0).getUuid());
		assertEquals(folder("news").getUuid(), listFromResponse.getItems().get(1).getUuid());

	}

	@Test
	public void testNullNodeList() throws IOException {
		setSchema("node");
		NodeResponse response = createNodeAndCheck("listField", (Field) null);
		//TODO see CL-359
	}

	@Test
	public void testNullNodeList2() throws IOException {
		setSchema("node");

		NodeFieldListImpl listField = new NodeFieldListImpl();
		listField.add(new NodeFieldListItemImpl(null));

	}

	@Test
	public void testStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNodeAndCheck("listField", listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList("listField");
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

		NodeResponse response = createNodeAndCheck("listField", listField);
		HtmlFieldListImpl listFromResponse = response.getFields().getHtmlFieldList("listField");
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testBooleanList() throws IOException {
		setSchema("boolean");
		BooleanFieldListImpl listField = new BooleanFieldListImpl();
		listField.add(true);
		listField.add(false);
		listField.add(null);

		NodeResponse response = createNodeAndCheck("listField", listField);
		BooleanFieldListImpl listFromResponse = response.getFields().getBooleanListField("listField");
		assertEquals("Only valid values (true,false) should be stored.", 2, listFromResponse.getItems().size());
	}

	@Test
	public void testDateList() throws IOException {
		setSchema("date");
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add((System.currentTimeMillis() / 1000) + 1);
		listField.add((System.currentTimeMillis() / 1000) + 2);
		listField.add((System.currentTimeMillis() / 1000) + 3);

		NodeResponse response = createNodeAndCheck("listField", listField);
		DateFieldListImpl listFromResponse = response.getFields().getDateFieldList("listField");
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testNumberList() throws IOException {
		setSchema("number");
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(0.1);
		listField.add(1337);
		listField.add(42);

		NodeResponse response = createNodeAndCheck("listField", listField);
		NumberFieldListImpl listFromResponse = response.getFields().getNumberFieldList("listField");
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
		NodeFieldListImpl field = response.getFields().getNodeListField("listField");
		assertEquals(1, field.getItems().size());

		/// Add another item to the list and update the node
		list.add(new NodeFieldListItemImpl(node2.getUuid()));
		response = updateNode("listField", list);
		field = response.getFields().getNodeListField("listField");
		assertEquals(2, field.getItems().size());
	}

	@Test
	public void testUpdateNodeWithStringField() throws IOException {
		setSchema("string");

		StringFieldListImpl listField = new StringFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNodeAndCheck("listField", listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList("listField");
		assertEquals(3, listFromResponse.getItems().size());
		for (int i = 0; i < listField.getItems().size(); i++) {
			assertEquals("Check item #" + (i + 1), listField.getItems().get(i), listFromResponse.getItems().get(i));
		}

		// Add another item to the list and update the node
		listField.add("D");
		response = updateNode("listField", listField);
		listFromResponse = response.getFields().getStringFieldList("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithHtmlField() throws IOException {
		setSchema("html");

		HtmlFieldListImpl listField = new HtmlFieldListImpl();
		listField.add("A");
		listField.add("B");
		listField.add("C");

		NodeResponse response = createNodeAndCheck("listField", listField);
		HtmlFieldListImpl listFromResponse = response.getFields().getHtmlFieldList("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add("D");
		response = updateNode("listField", listField);
		listFromResponse = response.getFields().getHtmlFieldList("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithDateField() throws IOException {
		setSchema("date");

		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add(1L);
		listField.add(2L);
		listField.add(3L);

		NodeResponse response = createNodeAndCheck("listField", listField);
		DateFieldListImpl listFromResponse = response.getFields().getDateFieldList("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add(4L);
		response = updateNode("listField", listField);
		listFromResponse = response.getFields().getDateFieldList("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	public void testUpdateNodeWithNumberField() throws IOException {
		setSchema("number");

		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(1.1);
		listField.add(1.2);
		listField.add(1.4);

		NodeResponse response = createNodeAndCheck("listField", listField);
		NumberFieldListImpl listFromResponse = response.getFields().getNumberFieldList("listField");
		assertEquals(3, listFromResponse.getItems().size());

		// Add another item to the list and update the node
		listField.add(1.6);
		response = updateNode("listField", listField);
		listFromResponse = response.getFields().getNumberFieldList("listField");
		assertEquals(4, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeFieldListImpl listField = new NodeFieldListImpl();
		NodeFieldListItemImpl item = new NodeFieldListItemImpl().setUuid(folder("news").getUuid());
		listField.add(item);
		NodeResponse response = createNodeAndCheck("listField", listField);
		NodeFieldListImpl listFromResponse = response.getFields().getNodeListField("listField");
		assertEquals(1, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList("listField");
		nodeList.createNode("1", folder("news"));
		NodeResponse response = readNode(node);
		NodeFieldListImpl deserializedListField = response.getFields().getNodeListField("listField");
		assertNotNull(deserializedListField);
		assertEquals(1, deserializedListField.getItems().size());
	}

	@Test
	public void testReadExpandedNodeListWithExistingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		// Create node list
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList("listField");
		nodeList.createNode("1", newsNode);

		// 1. Read node with collapsed fields and check that the collapsed node list item can be read
		NodeResponse responseCollapsed = readNode(node);
		NodeFieldList deserializedNodeListField = responseCollapsed.getFields().getNodeFieldList("listField");
		assertNotNull(deserializedNodeListField);
		assertEquals("The newsNode should be the first item in the list.", newsNode.getUuid(), deserializedNodeListField.getItems().get(0).getUuid());

		// Check whether it is possible to read the field in an expanded form.
		NodeResponse nodeListItem = (NodeResponse) deserializedNodeListField.getItems().get(0);
		assertNotNull(nodeListItem);

		// 2. Read node with expanded fields
		NodeResponse responseExpanded = readNode(node, "listField", "bogus");

		// Check collapsed node field
		deserializedNodeListField = responseExpanded.getFields().getNodeListField("listField");
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
