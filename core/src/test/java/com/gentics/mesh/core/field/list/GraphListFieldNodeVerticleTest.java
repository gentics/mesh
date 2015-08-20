package com.gentics.mesh.core.field.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
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
import com.gentics.mesh.graphdb.Trx;

public class GraphListFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		try (Trx tx = new Trx(db)) {
			setSchema("node");
			tx.success();
		}
	}

	private void setSchema(String listType) throws IOException {
		try (Trx tx = new Trx(db)) {
			Schema schema = schemaContainer("folder").getSchema();
			ListFieldSchema listFieldSchema = new ListFieldSchemaImpl();
			listFieldSchema.setName("listField");
			listFieldSchema.setLabel("Some label");
			listFieldSchema.setListType(listType);
			schema.addField(listFieldSchema);
			schemaContainer("folder").setSchema(schema);
		}
	}

	@Test
	public void testStringList() throws IOException {
		try (Trx tx = new Trx(db)) {
			setSchema("string");
			StringFieldListImpl listField = new StringFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add("C");

			NodeResponse response = createNode("listField", listField);
			StringFieldListImpl listFromResponse = response.getField("listField");
			assertEquals(3, listFromResponse.getList().size());
		}

	}

	@Test
	public void testHtmlList() throws IOException {
		try (Trx tx = new Trx(db)) {
			setSchema("html");
			HtmlFieldListImpl listField = new HtmlFieldListImpl();
			listField.add("A");
			listField.add("B");
			listField.add("C");

			NodeResponse response = createNode("listField", listField);
			HtmlFieldListImpl listFromResponse = response.getField("listField");
			assertEquals(3, listFromResponse.getList().size());
		}
	}

	@Test
	public void testBooleanList() throws IOException {
		try (Trx tx = new Trx(db)) {
			setSchema("boolean");
			BooleanFieldListImpl listField = new BooleanFieldListImpl();
			listField.add(true);
			listField.add(false);
			listField.add(null);

			NodeResponse response = createNode("listField", listField);
			BooleanFieldListImpl listFromResponse = response.getField("listField");
			assertEquals(3, listFromResponse.getList().size());
		}
	}

	@Test
	public void testDateList() throws IOException {
		try (Trx tx = new Trx(db)) {
			setSchema("date");
			DateFieldListImpl listField = new DateFieldListImpl();
			listField.add("01.01.1971");
			listField.add("01.01.1972");
			listField.add("01.01.1973");

			NodeResponse response = createNode("listField", listField);
			DateFieldListImpl listFromResponse = response.getField("listField");
			assertEquals(3, listFromResponse.getList().size());
		}
	}

	@Test
	public void testNumberList() throws IOException {
		try (Trx tx = new Trx(db)) {
			setSchema("number");
			NumberFieldListImpl listField = new NumberFieldListImpl();
			listField.add("0.1");
			listField.add("1337");
			listField.add("42");

			NodeResponse response = createNode("listField", listField);
			NumberFieldListImpl listFromResponse = response.getField("listField");
			assertEquals(3, listFromResponse.getList().size());
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		try (Trx tx = new Trx(db)) {
			Node node = folder("news");
			NodeFieldListImpl list = new NodeFieldListImpl();
			list.add(new NodeFieldListItemImpl(node.getUuid()));
			NodeResponse response = updateNode("listField", list);
			NodeFieldListImpl field = response.getField("listField");
			assertEquals(1, field.getList().size());

			Node node2 = folder("deals");
			list = new NodeFieldListImpl();
			list.add(new NodeFieldListItemImpl(node2.getUuid()));
			response = updateNode("listField", list);
			field = response.getField("listField");
			assertEquals(1, field.getList().size());
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Trx tx = new Trx(db)) {
			NodeFieldListImpl listField = new NodeFieldListImpl();
			NodeFieldListItemImpl item = new NodeFieldListItemImpl().setUuid(folder("news").getUuid());
			listField.add(item);
			NodeResponse response = createNode("listField", listField);
			NodeFieldListImpl listFromResponse = response.getField("listField");
			assertEquals(1, listFromResponse.getList().size());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node;
		try (Trx tx = new Trx(db)) {
			node = folder("2015");

			NodeFieldContainer container = node.getFieldContainer(english());
			GraphNodeFieldList nodeList = container.createNodeList("listField");
			nodeList.createNode("1", folder("news"));
			tx.success();
		}
		try (Trx tx = new Trx(db)) {
			NodeResponse response = readNode(node);
			NodeFieldListImpl deserializedListField = response.getField("listField", NodeFieldListImpl.class);
			assertNotNull(deserializedListField);
			assertEquals(1, deserializedListField.getList().size());
		}
	}

	@Test
	public void testReadExpandedNodeListWithExitingField() throws IOException {
		try (Trx tx = new Trx(db)) {
			resetClientSchemaStorage();
			Node newsNode = folder("news");
			Node node = folder("2015");

			// Create node list
			NodeFieldContainer container = node.getFieldContainer(english());
			GraphNodeFieldList nodeList = container.createNodeList("listField");
			nodeList.createNode("1", newsNode);

			// 1. Read node with collapsed fields and check that the collapsed node list item can be read
			NodeResponse responseCollapsed = readNode(node);
			com.gentics.mesh.core.rest.node.field.list.NodeFieldList deserializedNodeListField = responseCollapsed.getField("listField",
					NodeFieldListImpl.class);
			assertNotNull(deserializedNodeListField);
			assertEquals("The newsNode should be the first item in the list.", newsNode.getUuid(),
					deserializedNodeListField.getList().get(0).getUuid());

			// Check whether it is possible to read the field in an expanded form.
			NodeResponse nodeListItem = (NodeResponse) deserializedNodeListField.getList().get(0);
			assertNotNull(nodeListItem);

			// 2. Read node with expanded fields
			NodeResponse responseExpanded = readNode(node, "listField", "bogus");

			// Check collapsed node field
			deserializedNodeListField = responseExpanded.getField("listField", NodeFieldListImpl.class);
			assertNotNull(deserializedNodeListField);
			assertEquals(newsNode.getUuid(), deserializedNodeListField.getList().get(0).getUuid());

			// Check expanded node field
			NodeFieldListItem deserializedExpandedItem = deserializedNodeListField.getList().get(0);
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

}
