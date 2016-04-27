package com.gentics.mesh.core.field.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;

public class NodeListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String MICRONODE_LIST = "micronodeList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("micronode");
		schema.setName(MICRONODE_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node newsNode = folder("news");
		Node node = folder("2015");

		prepareNode(node, "nodeList", "node");

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		NodeGraphFieldList nodeList = container.createNodeList("nodeList");
		nodeList.createNode("1", newsNode);
		nodeList.createNode("2", newsNode);

		NodeResponse response = transform(node);
		assertList(2, "nodeList", "node", response);
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
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
	@Override
	public void testClone() {
		Node node = tx.getGraph().addFramedVertex(NodeImpl.class);

		NodeGraphFieldContainer container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NodeGraphFieldList testField = container.createNodeList("testField");
		testField.createNode("1", node);
		testField.createNode("2", node);
		testField.createNode("3", node);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getNodeList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
	}

	@Test
	@Override
	public void testEquals() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testEqualsNull() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testEqualsRestField() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRemoveFieldViaNullValue() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNullValue() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		// TODO Auto-generated method stub

	}
}
