package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.core.field.node.NodeListFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.node.NodeListFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.node.NodeListFieldTestHelper.FILL;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.GraphField;
import com.gentics.mesh.core.data.node.field.list.NodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.GraphDBTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.list.impl.HtmlFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListImpl;
import com.gentics.mesh.core.rest.node.field.list.impl.NodeFieldListItemImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.util.UUIDUtil;

@MeshTestSetting(testSize = FULL, startServer = false)
public class NodeListFieldTest extends AbstractFieldTest<ListFieldSchema> {

	private static final String NODE_LIST = "nodeList";

	@Override
	protected ListFieldSchema createFieldSchema(boolean isRequired) {
		ListFieldSchema schema = new ListFieldSchemaImpl();
		schema.setListType("node");
		schema.setName(NODE_LIST);
		schema.setRequired(isRequired);
		return schema;
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		HibNode node = folder("2015");
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = (ContentDaoWrapper) tx.contentDao();
			HibNode newsNode = folder("news");
			prepareNode(node, NODE_LIST, "node");
			NodeGraphFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			NodeGraphFieldList nodeList = container.createNodeList(NODE_LIST);
			nodeList.createNode("1", newsNode);
			nodeList.createNode("2", newsNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = transform(node);
			assertList(2, NODE_LIST, "node", response);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			// Create node field
			Node node = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeImpl.class);
			NodeGraphFieldContainer container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
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
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			Node node = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeImpl.class);

			NodeGraphFieldContainer container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldList testField = container.createNodeList("testField");
			testField.createNode("1", node);
			testField.createNode("2", node);
			testField.createNode("3", node);

			NodeGraphFieldContainerImpl otherContainer = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getNodeList("testField")).as("cloned field").isEqualToComparingFieldByField(testField);
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldList fieldA = container.createNodeList("fieldA");
			NodeGraphFieldList fieldB = container.createNodeList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.addItem(fieldA.createNode("testNode", content()));
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.addItem(fieldB.createNode("testNode", content()));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphFieldList fieldA = container.createNodeList("fieldA");
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainer container = ((GraphDBTx) tx).getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			String dummyKey = "test123";

			// rest null - graph null
			NodeGraphFieldList fieldA = container.createNodeList(NODE_LIST);

			NodeFieldListImpl restField = new NodeFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.addItem(fieldA.createNode("1", content()));
			restField.add(new NodeFieldListItemImpl(UUIDUtil.randomUUID()));
			assertFalse("Both fields should be different since both values are not equal", fieldA.equals(restField));

			// rest set - graph set - same value
			restField.getItems().clear();
			restField.add(new NodeFieldListItemImpl(content().getUuid()));
			assertTrue("Both fields should be equal since values are equal", fieldA.equals(restField));

			HtmlFieldListImpl otherTypeRestField = new HtmlFieldListImpl();
			otherTypeRestField.add(dummyKey);
			// rest set - graph set - same value different type
			assertFalse("Fields should not be equal since the type does not match.", fieldA.equals(otherTypeRestField));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(NODE_LIST, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(NODE_LIST, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(NODE_LIST, FETCH, FILL, (node) -> {
				updateContainer(ac, node, NODE_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(NODE_LIST, FETCH, FILL, (container) -> {
				updateContainer(ac, container, NODE_LIST, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(NODE_LIST, FILL, (container) -> {
				NodeFieldListImpl field = new NodeFieldListImpl();
				field.getItems().add(new NodeFieldListItemImpl(content().getUuid()));
				field.getItems().add(new NodeFieldListItemImpl(folder("2015").getUuid()));
				updateContainer(ac, container, NODE_LIST, field);
			}, (container) -> {
				NodeGraphFieldList field = container.getNodeList(NODE_LIST);
				assertNotNull("The graph field {" + NODE_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", content().getUuid(), field.getList().get(0).getNode().getUuid());
				assertEquals("The list item of the field was not updated.", folder("2015").getUuid(), field.getList().get(1).getNode().getUuid());
			});
		}
	}
}
