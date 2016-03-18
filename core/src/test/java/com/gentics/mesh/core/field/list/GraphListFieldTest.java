package com.gentics.mesh.core.field.list;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.data.node.field.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.BooleanGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.DateGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.StringGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.rest.common.FieldTypes;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.FieldList;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class GraphListFieldTest extends AbstractEmptyDBTest {

	@Test
	public void testNodeListTransformation() throws Exception {
		setupData();
		Node node = folder("2015");
		Node newsNode = folder("news");

		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		ListFieldSchema nodeListFieldSchema = new ListFieldSchemaImpl();
		nodeListFieldSchema.setName("nodeList");
		nodeListFieldSchema.setListType("node");
		schema.addField(nodeListFieldSchema);

		ListFieldSchema stringListFieldSchema = new ListFieldSchemaImpl();
		stringListFieldSchema.setName("stringList");
		stringListFieldSchema.setListType("string");
		schema.addField(stringListFieldSchema);

		ListFieldSchema htmlListFieldSchema = new ListFieldSchemaImpl();
		htmlListFieldSchema.setName("htmlList");
		htmlListFieldSchema.setListType("html");
		schema.addField(htmlListFieldSchema);

		ListFieldSchema numberListFieldSchema = new ListFieldSchemaImpl();
		numberListFieldSchema.setName("numberList");
		numberListFieldSchema.setListType("number");
		schema.addField(numberListFieldSchema);

		ListFieldSchema booleanListFieldSchema = new ListFieldSchemaImpl();
		booleanListFieldSchema.setName("booleanList");
		booleanListFieldSchema.setListType("boolean");
		schema.addField(booleanListFieldSchema);

		ListFieldSchema dateListFieldSchema = new ListFieldSchemaImpl();
		dateListFieldSchema.setName("dateList");
		dateListFieldSchema.setListType("date");
		schema.addField(dateListFieldSchema);

		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());

		NodeGraphFieldList nodeList = container.createNodeList("nodeList");
		nodeList.createNode("1", newsNode);
		nodeList.createNode("2", newsNode);

		BooleanGraphFieldList booleanList = container.createBooleanList("booleanList");
		booleanList.createBoolean(true);
		booleanList.createBoolean(null);
		booleanList.createBoolean(false);

		NumberGraphFieldList numberList = container.createNumberList("numberList");
		numberList.createNumber(1);
		numberList.createNumber(1.11);

		DateGraphFieldList dateList = container.createDateList("dateList");
		dateList.createDate(1L);
		dateList.createDate(2L);

		StringGraphFieldList stringList = container.createStringList("stringList");
		stringList.createString("dummyString1");
		stringList.createString("dummyString2");

		HtmlGraphFieldList htmlList = container.createHTMLList("htmlList");
		htmlList.createHTML("some<b>html</b>");
		htmlList.createHTML("some<b>more html</b>");

		MicronodeGraphFieldList micronodeList = container.createMicronodeFieldList("micronodeList");
		micronodeList.addItem(null);
		micronodeList.addItem(null);

		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		assertList(2, "stringList", "string", response);
		assertList(2, "htmlList", "html", response);
		assertList(2, "dateList", "date", response);
		assertList(2, "numberList", "number", response);
		assertList(2, "nodeList", "node", response);
		assertList(2, "booleanList", "boolean", response);
		//TODO Add micronode assertion
		//		assertList(2, "micronodeList", MicronodeFieldListImpl.class, response);

	}

	private void assertList(int expectedItems, String fieldKey, String listType, NodeResponse response) {
		Field deserializedList = response.getFields().getField(fieldKey, FieldTypes.LIST, listType, false);
		assertNotNull(deserializedList);
		FieldList<?> listField = (FieldList<?>) deserializedList;
		assertEquals("The list of type {" + listType + "} did not contain the expected amount of items.", expectedItems, listField.getItems().size());
	}

	@Test
	public void testStringList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphFieldList list = container.createStringList("dummyList");

		list.createString("1");
		assertEquals("dummyList", list.getFieldKey());
		assertNotNull(list.getList());

		assertEquals(1, list.getList().size());
		assertEquals(list.getSize(), list.getList().size());
		list.createString("2");
		assertEquals(2, list.getList().size());
		list.createString("3").setString("Some string 3");
		assertEquals(3, list.getList().size());
		assertEquals("Some string 3", list.getList().get(2).getString());

		StringGraphFieldList loadedList = container.getStringList("dummyList");
		assertNotNull(loadedList);
		assertEquals(3, loadedList.getSize());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	public void testNodeList() {
		// Create node field
		Node node = tx.getGraph().addFramedVertex(NodeImpl.class);
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NodeGraphFieldList list = container.createNodeList("dummyList");

		// Add item
		assertEquals(0, list.getList().size());
		list.createNode("1", node);
		assertEquals(1, list.getList().size());

		// Retrieve item
		NodeGraphField foundNodeField = list.getList().get(0);
		assertNotNull(foundNodeField.getNode());
		assertEquals(node.getUuid(), foundNodeField.getNode().getUuid());

		// Load list
		NodeGraphFieldList loadedList = container.getNodeList("dummyList");
		assertNotNull(loadedList);
		assertEquals(1, loadedList.getSize());
		assertEquals(node.getUuid(), loadedList.getList().get(0).getNode().getUuid());

		// Add another item
		assertEquals(1, list.getList().size());
		list.createNode("2", node);
		assertEquals(2, list.getList().size());

		// Remove items
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	public void testNumberList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldList list = container.createNumberList("dummyList");

		list.createNumber(1);
		assertEquals(1, list.getList().size());

		list.createNumber(2);
		assertEquals(2, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	public void testDateList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList list = container.createDateList("dummyList");
		assertNotNull(list);
		DateGraphField dateField = list.createDate(1L);
		assertNotNull(dateField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	public void testHTMLList() {
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
	public void testMicronodeList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		MicronodeGraphFieldList list = container.createMicronodeFieldList("dummyList");
		assertNotNull(list);
	}

	@Test
	public void testBooleanList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphFieldList list = container.createBooleanList("dummyList");
		list.createBoolean(true);
		list.createBoolean(false);
		list.createBoolean(null);
		assertEquals("Only non-null values are persisted.", 2, list.getList().size());
		assertEquals(2, list.getSize());
		assertNotNull(list.getBoolean(1));
		assertTrue(list.getBoolean(1).getBoolean());
		list.removeAll();
		assertEquals(0, list.getSize());
		assertEquals(0, list.getList().size());
	}

	@Test
	public void testCloneStringList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		StringGraphFieldList testField = container.createStringList("testField");
		testField.createString("one");
		testField.createString("two");
		testField.createString("three");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getStringList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}

	@Test
	public void testCloneNodeList() {
		Node node = tx.getGraph().addFramedVertex(NodeImpl.class);

		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NodeGraphFieldList testField = container.createNodeList("testField");
		testField.createNode("1", node);
		testField.createNode("2", node);
		testField.createNode("3", node);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getNodeList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}

	@Test
	public void testCloneNumberList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NumberGraphFieldList testField = container.createNumberList("testField");
		testField.createNumber(47);
		testField.createNumber(11);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getNumberList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}

	@Test
	public void testCloneDateList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		DateGraphFieldList testField = container.createDateList("testField");
		testField.createDate(47L);
		testField.createDate(11L);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getDateList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}

	@Test
	public void testCloneHtmlList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		HtmlGraphFieldList testField = container.createHTMLList("testField");
		testField.createHTML("<b>One</b>");
		testField.createHTML("<i>Two</i>");
		testField.createHTML("<u>Three</u>");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getHTMLList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}

	@Test
	public void testCloneMicronodeList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		MicronodeGraphFieldList testField = container.createMicronodeFieldList("testField");
		
		Micronode micronode = testField.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
		micronode.createString("firstName").setString("Donald");
		micronode.createString("lastName").setString("Duck");

		micronode = testField.createMicronode(new MicronodeResponse());
		micronode.setMicroschemaContainerVersion(microschemaContainers().get("vcard").getLatestVersion());
		micronode.createString("firstName").setString("Mickey");
		micronode.createString("lastName").setString("Mouse");

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getMicronodeList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}

	@Test
	public void testCloneBooleanList() {
		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphFieldList testField = container.createBooleanList("testField");
		testField.createBoolean(true);
		testField.createBoolean(false);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getBooleanList("testField")).as("cloned field")
				.isEqualToComparingFieldByField(testField);
	}
}
