package com.gentics.mesh.core.field.list;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.DateGraphField;
import com.gentics.mesh.core.data.node.field.basic.HtmlGraphField;
import com.gentics.mesh.core.data.node.field.list.GraphBooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphDateFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphHtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphMicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNodeFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphNumberFieldList;
import com.gentics.mesh.core.data.node.field.list.GraphStringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.GraphNodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class GraphListFieldTest extends AbstractDBTest {

	@Test
	public void testStringList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphStringFieldList list = container.createStringList("dummyList");

		list.createString("1");
		assertEquals("dummyList", list.getFieldKey());
		assertNotNull(list.getList());

		assertEquals(1, list.getList().size());
		assertEquals(list.getSize(), list.getList().size());
		list.createString("2");
		assertEquals(2, list.getList().size());
		list.createString("3").setString("Some string 3");
		assertEquals(3, list.getList().size());
		assertEquals("Some string 3", list.getList().get(0).getString());

		GraphStringFieldList loadedList = container.getStringList("dummyList");
		assertNotNull(loadedList);
		assertEquals(3, loadedList.getSize());
	}

	@Test
	public void testNodeList() {
		Node node = fg.addFramedVertex(NodeImpl.class);
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphNodeFieldList list = container.createNodeList("dummyList");
		assertEquals(0, list.getList().size());
		list.createNode("1", node);
		assertEquals(1, list.getList().size());

		GraphNodeField foundNodeField = list.getList().get(0);
		assertNotNull(foundNodeField.getNode());
		assertEquals(node.getUuid(), foundNodeField.getNode().getUuid());

		GraphNodeFieldList loadedList = container.getNodeList("dummyList");
		assertNotNull(loadedList);
		assertEquals(1, loadedList.getSize());
		assertEquals(node.getUuid(), loadedList.getList().get(0).getNode().getUuid());
	}

	@Test
	public void testNumberList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphNumberFieldList list = container.createNumberList("dummyList");

		list.createNumber("1");
		assertEquals(1, list.getList().size());

		list.createNumber("2");
		assertEquals(2, list.getList().size());
	}

	@Test
	public void testDateList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphDateFieldList list = container.createDateList("dummyList");
		assertNotNull(list);
		DateGraphField dateField = list.createDate("Date One");
		assertNotNull(dateField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
	}

	@Test
	public void testHTMLList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphHtmlFieldList list = container.createHTMLList("dummyList");
		assertNotNull(list);
		HtmlGraphField htmlField = list.createHTML("HTML 1");
		assertNotNull(htmlField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
	}

	@Test
	public void testMicroschemaList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphMicroschemaFieldList list = container.createMicroschemaFieldList("dummyList");
		assertNotNull(list);
	}

	@Test
	public void testBooleanList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeGraphFieldContainerImpl.class);
		GraphBooleanFieldList list = container.createBooleanList("dummyList");
		list.createBoolean(true);
		list.createBoolean(false);
		list.createBoolean(null);
		assertEquals(3, list.getList().size());
		assertEquals(3, list.getSize());
		assertNotNull(list.getBoolean(1));
		assertTrue(list.getBoolean(1).getBoolean());
	}

}
