package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.core.field.node.NodeListFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.node.NodeListFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.node.NodeListFieldTestHelper.FILL;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibNodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibNodeField;
import com.gentics.mesh.core.db.CommonTx;
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
import com.gentics.mesh.util.CoreTestUtils;
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
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();
			HibNode newsNode = folder("news");
			prepareNode(node, NODE_LIST, "node");
			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibNodeFieldList nodeList = container.createNodeList(NODE_LIST);
			nodeList.createNode(0, newsNode);
			nodeList.createNode(1, newsNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			NodeResponse response = transform(node);
			assertList(2, NODE_LIST, "node", response);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			// Create node field
			HibNode node = tx.<CommonTx>unwrap().nodeDao().createPersisted(project(), null);
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibNodeFieldList list = container.createNodeList("dummyList");

			// Add item
			assertEquals(0, list.getList().size());
			list.createNode(0, node);
			assertEquals(1, list.getList().size());

			// Retrieve item
			HibNodeField foundNodeField = list.getList().get(0);
			assertNotNull(foundNodeField.getNode());
			assertEquals(node.getUuid(), foundNodeField.getNode().getUuid());

			// Load list
			HibNodeFieldList loadedList = container.getNodeList("dummyList");
			assertNotNull(loadedList);
			assertEquals(1, loadedList.getSize());
			assertEquals(node.getUuid(), loadedList.getList().get(0).getNode().getUuid());

			// Add another item
			assertEquals(1, list.getList().size());
			list.createNode(1, node);
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
			HibNode node = folder("2015");

			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibNodeFieldList testField = container.createNodeList("testField");
			testField.createNode(0, node);
			testField.createNode(1, node);
			testField.createNode(2, node);

			HibNodeFieldContainer otherContainer = CoreTestUtils.createContainer();
			testField.cloneTo(otherContainer);

			assertTrue(otherContainer.getNodeList("testField").equals(testField));
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibNodeFieldList fieldA = container.createNodeList("fieldA");
			HibNodeFieldList fieldB = container.createNodeList("fieldB");
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));
			fieldA.addItem(fieldA.createNode(0, content()));
			assertTrue("The field should  still be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			fieldB.addItem(fieldB.createNode(0, content()));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldB));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			HibNodeFieldList fieldA = container.createNodeList("fieldA");
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((HibNodeFieldList) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			HibNodeFieldContainer container = CoreTestUtils.createContainer();
			String dummyKey = "test123";

			// rest null - graph null
			HibNodeFieldList fieldA = container.createNodeList(NODE_LIST);

			NodeFieldListImpl restField = new NodeFieldListImpl();
			assertTrue("Both fields should be equal to eachother since both values are null", fieldA.equals(restField));

			// rest set - graph set - different values
			fieldA.addItem(fieldA.createNode(content()));
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
				HibNodeFieldList field = container.getNodeList(NODE_LIST);
				assertNotNull("The graph field {" + NODE_LIST + "} could not be found.", field);
				assertEquals("The list of the field was not updated.", 2, field.getList().size());
				assertEquals("The list item of the field was not updated.", content().getUuid(), field.getList().get(0).getNode().getUuid());
				assertEquals("The list item of the field was not updated.", folder("2015").getUuid(), field.getList().get(1).getNode().getUuid());
			});
		}
	}
}
