package com.gentics.mesh.core.field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.impl.NodeFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.DateField;
import com.gentics.mesh.core.data.node.field.basic.HtmlField;
import com.gentics.mesh.core.data.node.field.list.BooleanFieldList;
import com.gentics.mesh.core.data.node.field.list.DateFieldList;
import com.gentics.mesh.core.data.node.field.list.HtmlFieldList;
import com.gentics.mesh.core.data.node.field.list.MicroschemaFieldList;
import com.gentics.mesh.core.data.node.field.list.NodeFieldList;
import com.gentics.mesh.core.data.node.field.list.NumberFieldList;
import com.gentics.mesh.core.data.node.field.list.StringFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.test.AbstractDBTest;

public class ListFieldTest extends AbstractDBTest {

	@Test
	public void testStringList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		StringFieldList list = container.createStringList("dummyList");

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

		StringFieldList loadedList = container.getStringList("dummyList");
		assertNotNull(loadedList);
		assertEquals(3, loadedList.getSize());
	}

	@Test
	public void testNodeList() {
		Node node = fg.addFramedVertex(NodeImpl.class);
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		NodeFieldList list = container.createNodeList("dummyList");
		assertEquals(0, list.getList().size());
		list.createNode("1", node);
		assertEquals(1, list.getList().size());

		NodeField foundNodeField = list.getList().get(0);
		assertNotNull(foundNodeField.getNode());
		assertEquals(node.getUuid(), foundNodeField.getNode().getUuid());

		NodeFieldList loadedList = container.getNodeList("dummyList");
		assertNotNull(loadedList);
		assertEquals(1, loadedList.getSize());
		assertEquals(node.getUuid(), loadedList.getList().get(0).getNode().getUuid());
	}

	@Test
	public void testNumberList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		NumberFieldList list = container.createNumberList("dummyList");

		list.createNumber("1");
		assertEquals(1, list.getList().size());

		list.createNumber("2");
		assertEquals(2, list.getList().size());
	}

	@Test
	public void testDateList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		DateFieldList list = container.createDateList("dummyList");
		assertNotNull(list);
		DateField dateField = list.createDate("Date One");
		assertNotNull(dateField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
	}

	@Test
	public void testHTMLList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		HtmlFieldList list = container.createHTMLList("dummyList");
		assertNotNull(list);
		HtmlField htmlField = list.createHTML("HTML 1");
		assertNotNull(htmlField);
		assertEquals(1, list.getSize());
		assertEquals(1, list.getList().size());
	}

	@Test
	public void testMicroschemaList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		MicroschemaFieldList list = container.createMicroschemaFieldList("dummyList");
		assertNotNull(list);
	}

	@Test
	public void testBooleanList() {
		NodeFieldContainer container = fg.addFramedVertex(NodeFieldContainerImpl.class);
		BooleanFieldList list = container.createBooleanList("dummyList");
		list.createBoolean(true);
		list.createBoolean(false);
		list.createBoolean(null);
		assertEquals(3, list.getList().size());
		assertEquals(3, list.getSize());
		assertNotNull(list.getBoolean(1));
		assertTrue(list.getBoolean(1).getBoolean());
	}

}
