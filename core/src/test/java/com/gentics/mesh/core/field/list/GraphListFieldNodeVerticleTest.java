package com.gentics.mesh.core.field.list;

import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.impl.BooleanGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.DateGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.HtmlGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NodeGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.NumberGraphFieldListImpl;
import com.gentics.mesh.core.data.node.field.list.impl.StringGraphFieldListImpl;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
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

public class GraphListFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {
	private static final String FIELD_NAME = "listField";

	@Before
	public void updateSchema() throws IOException {
		setSchema("node");
	}

	private void setSchema(String listType) throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
		listFieldSchema.setName(FIELD_NAME);
		listFieldSchema.setLabel("Some label");
		listFieldSchema.setListType(listType);
		schema.removeField(FIELD_NAME);
		schema.addField(listFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(null, (Field) null);
		NodeFieldList nodeField = response.getFields().getNodeFieldList(FIELD_NAME);
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateNodeWithNullFieldValue() throws IOException {
		setSchema("string");
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		StringFieldListImpl nodeField = response.getFields().getStringFieldList(FIELD_NAME);
		assertNotNull(nodeField);
		assertEquals(0, nodeField.getItems().size());
	}

	@Test
	public void testCreateEmptyStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		NodeResponse response = createNode(FIELD_NAME, listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateNullStringList() throws IOException {
		setSchema("string");
		StringFieldListImpl listField = new StringFieldListImpl();
		listField.setItems(null);
		NodeResponse response = createNode(FIELD_NAME, listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testCreateWithOmittedStringListValue() throws IOException {
		setSchema("string");
		NodeResponse response = createNode(null, (Field) null);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
		assertNotNull(listFromResponse);
		assertEquals(0, listFromResponse.getItems().size());
	}

	@Test
	public void testBogusNodeList() throws IOException {
		setSchema("node");

		NodeFieldListImpl listField = new NodeFieldListImpl();
		listField.add(new NodeFieldListItemImpl("bogus"));

		Future<NodeResponse> future = createNodeAsync("listField", listField);
		latchFor(future);
		expectException(future, BAD_REQUEST, "node_list_item_not_found", "bogus");
	}

	@Test
	public void testValidNodeList() throws IOException {
		setSchema("node");

		NodeFieldListImpl listField = new NodeFieldListImpl();
		listField.add(new NodeFieldListItemImpl(content().getUuid()));
		listField.add(new NodeFieldListItemImpl(folder("news").getUuid()));

		NodeResponse response = createNode("listField", listField);

		NodeFieldList listFromResponse = response.getFields().getNodeFieldList("listField");
		assertEquals(2, listFromResponse.getItems().size());
		assertEquals(content().getUuid(), listFromResponse.getItems().get(0).getUuid());
		assertEquals(folder("news").getUuid(), listFromResponse.getItems().get(1).getUuid());

	}

	@Test
	public void testNullNodeList() throws IOException {
		setSchema("node");
		NodeResponse response = createNode("listField", (Field) null);
		// TODO see CL-359
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

		NodeResponse response = createNode(FIELD_NAME, listField);
		StringFieldListImpl listFromResponse = response.getFields().getStringFieldList(FIELD_NAME);
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

		NodeResponse response = createNode(FIELD_NAME, listField);
		HtmlFieldListImpl listFromResponse = response.getFields().getHtmlFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testBooleanList() throws IOException {
		setSchema("boolean");
		BooleanFieldListImpl listField = new BooleanFieldListImpl();
		listField.add(true);
		listField.add(false);
		listField.add(null);

		NodeResponse response = createNode(FIELD_NAME, listField);
		BooleanFieldListImpl listFromResponse = response.getFields().getBooleanFieldList(FIELD_NAME);
		assertEquals("Only valid values (true,false) should be stored.", 2, listFromResponse.getItems().size());
	}

	@Test
	public void testDateList() throws IOException {
		setSchema("date");
		DateFieldListImpl listField = new DateFieldListImpl();
		listField.add((System.currentTimeMillis() / 1000) + 1);
		listField.add((System.currentTimeMillis() / 1000) + 2);
		listField.add((System.currentTimeMillis() / 1000) + 3);

		NodeResponse response = createNode(FIELD_NAME, listField);
		DateFieldListImpl listFromResponse = response.getFields().getDateFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	public void testNumberList() throws IOException {
		setSchema("number");
		NumberFieldListImpl listField = new NumberFieldListImpl();
		listField.add(0.1);
		listField.add(1337);
		listField.add(42);

		NodeResponse response = createNode(FIELD_NAME, listField);
		NumberFieldListImpl listFromResponse = response.getFields().getNumberFieldList(FIELD_NAME);
		assertEquals(3, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		Node targetNode = folder("news");
		Node targetNode2 = folder("deals");

		List<List<Node>> valueCombinations = Arrays.asList(Arrays.asList(targetNode), Arrays.asList(targetNode2, targetNode), Collections.emptyList(),
				Arrays.asList(targetNode, targetNode2), Arrays.asList(targetNode2));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Node> oldValue = getListValues(container, NodeGraphFieldListImpl.class, FIELD_NAME);
			List<Node> newValue = valueCombinations.get(i % valueCombinations.size());

			NodeFieldListImpl list = new NodeFieldListImpl();
			for (Node value : newValue) {
				list.add(new NodeFieldListItemImpl(value.getUuid()));
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			NodeFieldList field = response.getFields().getNodeFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").usingElementComparatorOnFields("uuid").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, NodeGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	public void testUpdateNodeWithStringField() throws IOException {
		setSchema("string");
		Node node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
				Arrays.asList("X", "Y"), Arrays.asList("C"));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<String> oldValue = getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME);
			List<String> newValue = valueCombinations.get(i % valueCombinations.size());

			StringFieldListImpl list = new StringFieldListImpl();
			for (String value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			StringFieldListImpl field = response.getFields().getStringFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, StringGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	public void testUpdateNodeWithHtmlField() throws IOException {
		setSchema("html");
		Node node = folder("2015");

		List<List<String>> valueCombinations = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("C", "B", "A"), Collections.emptyList(),
				Arrays.asList("X", "Y"), Arrays.asList("C"));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<String> oldValue = getListValues(container, HtmlGraphFieldListImpl.class, FIELD_NAME);
			List<String> newValue = valueCombinations.get(i % valueCombinations.size());

			HtmlFieldListImpl list = new HtmlFieldListImpl();
			for (String value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			HtmlFieldListImpl field = response.getFields().getHtmlFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, HtmlGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	public void testUpdateNodeWithDateField() throws IOException {
		setSchema("date");
		Node node = folder("2015");

		List<List<Long>> valueCombinations = Arrays.asList(Arrays.asList(1L, 2L, 3L), Arrays.asList(3L, 2L, 1L), Collections.emptyList(),
				Arrays.asList(4711L, 815L), Arrays.asList(3L));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Long> oldValue = getListValues(container, DateGraphFieldListImpl.class, FIELD_NAME);
			List<Long> newValue = valueCombinations.get(i % valueCombinations.size());

			DateFieldListImpl list = new DateFieldListImpl();
			for (Long value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			DateFieldListImpl field = response.getFields().getDateFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, DateGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	public void testUpdateNodeWithNumberField() throws IOException {
		setSchema("number");
		Node node = folder("2015");

		List<List<Number>> valueCombinations = Arrays.asList(Arrays.asList(1.1, 2, 3), Arrays.asList(3, 2, 1.1), Collections.emptyList(),
				Arrays.asList(47.11, 8.15), Arrays.asList(3));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Number> oldValue = getListValues(container, NumberGraphFieldListImpl.class, FIELD_NAME);
			List<Number> newValue = valueCombinations.get(i % valueCombinations.size());

			NumberFieldListImpl list = new NumberFieldListImpl();
			for (Number value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			NumberFieldListImpl field = response.getFields().getNumberFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, NumberGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	public void testUpdateNodeWithBooleanField() throws IOException {
		setSchema("boolean");
		Node node = folder("2015");

		List<List<Boolean>> valueCombinations = Arrays.asList(Arrays.asList(true, false, false), Arrays.asList(false, false, true),
				Collections.emptyList(), Arrays.asList(true, false), Arrays.asList(false));

		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			List<Boolean> oldValue = getListValues(container, BooleanGraphFieldListImpl.class, FIELD_NAME);
			List<Boolean> newValue = valueCombinations.get(i % valueCombinations.size());

			BooleanFieldListImpl list = new BooleanFieldListImpl();
			for (Boolean value : newValue) {
				list.add(value);
			}
			NodeResponse response = updateNode(FIELD_NAME, list);
			BooleanFieldListImpl field = response.getFields().getBooleanFieldList(FIELD_NAME);
			assertThat(field.getItems()).as("Updated field").containsExactlyElementsOf(list.getItems());
			node.reload();
			container.reload();

			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getListValues(container, BooleanGraphFieldListImpl.class, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		Node targetNode = folder("news");
		Node targetNode2 = folder("deals");

		NodeFieldListImpl list = new NodeFieldListImpl();
		list.add(new NodeFieldListItemImpl(targetNode.getUuid()));
		list.add(new NodeFieldListItemImpl(targetNode2.getUuid()));
		NodeResponse firstResponse = updateNode(FIELD_NAME, list);
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, list);
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		Node targetNode = folder("news");
		Node targetNode2 = folder("deals");

		NodeFieldListImpl list = new NodeFieldListImpl();
		list.add(new NodeFieldListItemImpl(targetNode.getUuid()));
		list.add(new NodeFieldListItemImpl(targetNode2.getUuid()));
		updateNode(FIELD_NAME, list);

		updateNodeFailure(FIELD_NAME, new NodeFieldListImpl(), BAD_REQUEST, "node_error_missing_node_field_uuid", FIELD_NAME);

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getNodeFieldList(FIELD_NAME)).as("Updated Field").isNull();

	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeFieldListImpl listField = new NodeFieldListImpl();
		NodeFieldListItemImpl item = new NodeFieldListItemImpl().setUuid(folder("news").getUuid());
		listField.add(item);
		NodeResponse response = createNode(FIELD_NAME, listField);
		NodeFieldList listFromResponse = response.getFields().getNodeFieldList(FIELD_NAME);
		assertEquals(1, listFromResponse.getItems().size());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");

		NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList(FIELD_NAME);
		nodeList.createNode("1", folder("news"));
		NodeResponse response = readNode(node);
		NodeFieldList deserializedListField = response.getFields().getNodeFieldList(FIELD_NAME);
		assertNotNull(deserializedListField);
		assertEquals(1, deserializedListField.getItems().size());
	}

	@Test
	public void testReadExpandedNodeListWithExistingField() throws IOException {
		Node newsNode = folder("news");
		Node node = folder("2015");

		// Create node list
		NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList(FIELD_NAME);
		nodeList.createNode("1", newsNode);

		// 1. Read node with collapsed fields and check that the collapsed node list item can be read
		NodeResponse responseCollapsed = readNode(node);
		NodeFieldList deserializedNodeListField = responseCollapsed.getFields().getNodeFieldList(FIELD_NAME);
		assertNotNull(deserializedNodeListField);
		assertEquals("The newsNode should be the first item in the list.", newsNode.getUuid(), deserializedNodeListField.getItems().get(0).getUuid());

		// Check whether it is possible to read the field in an expanded form.
		NodeResponse nodeListItem = (NodeResponse) deserializedNodeListField.getItems().get(0);
		assertNotNull(nodeListItem);

		// 2. Read node with expanded fields
		NodeResponse responseExpanded = readNode(node, FIELD_NAME, "bogus");

		// Check collapsed node field
		deserializedNodeListField = responseExpanded.getFields().getNodeFieldList(FIELD_NAME);
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
