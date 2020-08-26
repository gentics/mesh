package com.gentics.mesh.core.field.node;

import static com.gentics.mesh.core.field.node.NodeFieldTestHelper.CREATE_EMPTY;
import static com.gentics.mesh.core.field.node.NodeFieldTestHelper.FETCH;
import static com.gentics.mesh.core.field.node.NodeFieldTestHelper.FILL;
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
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(testSize = FULL, startServer = false)
public class NodeFieldTest extends AbstractFieldTest<NodeFieldSchema> {

	final String NODE_FIELD = "nodeField";

	@Override
	protected NodeFieldSchema createFieldSchema(boolean isRequired) {
		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setName(NODE_FIELD);
		nodeFieldSchema.setAllowedSchemas("folder", "content");
		nodeFieldSchema.setRequired(isRequired);
		return nodeFieldSchema;
	}

	@Test
	@Override
	public void testClone() {
		try (Tx tx = tx()) {
			Node node = tx.getGraph().addFramedVertex(NodeImpl.class);

			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphField testField = container.createNode("testField", node);

			NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			testField.cloneTo(otherContainer);

			assertThat(otherContainer.getNode("testField")).as("cloned field").isNotNull();
			assertThat(otherContainer.getNode("testField").getNode()).as("cloned target node").isNotNull().isEqualToComparingFieldByField(node);
		}
	}

	@Test
	@Override
	public void testFieldUpdate() throws Exception {
		try (Tx tx = tx()) {
			HibNode node = tx.getGraph().addFramedVertex(NodeImpl.class);

			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphField field = container.createNode("testNodeField", node);
			assertNotNull(field);
			assertEquals("testNodeField", field.getFieldKey());
			HibNode loadedNode = field.getNode();
			assertNotNull(loadedNode);
			assertEquals(node.getUuid(), loadedNode.getUuid());

			NodeGraphField loadedField = container.getNode("testNodeField");
			assertNotNull(loadedField);
			assertNotNull(loadedField.getNode());
			assertEquals(node.getUuid(), loadedField.getNode().getUuid());
		}
	}

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		HibNode newsNode = folder("news");
		HibNode node = folder("2015");

		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.data().contentDao();
			SchemaVersionModel schema = node.getSchemaContainer().getLatestVersion().getSchema();

			// 1. Create the node field schema and add it to the schema of the node
			NodeFieldSchema nodeFieldSchema = createFieldSchema(true);
			schema.addField(nodeFieldSchema);
			node.getSchemaContainer().getLatestVersion().setSchema(schema);

			// 2. Add the node reference to the node fields
			NodeGraphFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			container.createNode(NODE_FIELD, newsNode);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 3. Transform the node to json and examine the data
			String json = getJson(node);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
			assertNotNull(response);

			NodeField deserializedNodeField = response.getFields().getNodeField(NODE_FIELD);
			assertNotNull("The field {" + NODE_FIELD + "} should not be null. Json: {" + json + "}", deserializedNodeField);
			assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
		}
	}

	@Test
	@Override
	public void testEquals() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphField fieldA = container.createNode("fieldA", folder("2015"));
			NodeGraphField fieldB = container.createNode("fieldB", folder("2014"));
			NodeGraphField fieldC = container.createNode("fieldC", folder("2015"));
			assertTrue("The field should  be equal to itself", fieldA.equals(fieldA));

			assertFalse("The field should not be equal to a non-string field", fieldA.equals("bogus"));
			assertFalse("The field should not be equal since fieldB has no value", fieldA.equals(fieldB));
			assertTrue("Both fields have the same value and should be equal", fieldA.equals(fieldC));
		}
	}

	@Test
	@Override
	public void testEqualsNull() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphField fieldA = container.createNode("field1", content());
			assertFalse(fieldA.equals((Field) null));
			assertFalse(fieldA.equals((GraphField) null));
		}
	}

	@Test
	@Override
	public void testEqualsRestField() {
		try (Tx tx = tx()) {
			NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
			NodeGraphField fieldA = container.createNode("field1", content());

			// graph set - rest set - same value - different type
			assertFalse("The field should not be equal to a string rest field. Even if it has the same value",
					fieldA.equals(new StringFieldImpl().setString("someText")));
			// graph set - rest set - different value
			assertFalse("The field should not be equal to the rest field since the rest field has a different value.",
					fieldA.equals(new NodeFieldImpl().setUuid(folder("2015").getUuid())));

			// graph set - rest set - same value
			assertTrue("The field should be equal to a rest field with the same value",
					fieldA.equals(new NodeFieldImpl().setUuid(content().getUuid())));
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreate() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestTestcase(NODE_FIELD, FETCH, CREATE_EMPTY);
		}
	}

	@Test
	@Override
	public void testUpdateFromRestNullOnCreateRequired() {
		try (Tx tx = tx()) {
			invokeUpdateFromRestNullOnCreateRequiredTestcase(NODE_FIELD, FETCH);
		}
	}

	@Test
	@Override
	public void testRemoveFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveFieldViaNullTestcase(NODE_FIELD, FETCH, FILL, (node) -> {
				updateContainer(ac, node, NODE_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testRemoveRequiredFieldViaNull() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeRemoveRequiredFieldViaNullTestcase(NODE_FIELD, FETCH, FILL, (container) -> {
				updateContainer(ac, container, NODE_FIELD, null);
			});
		}
	}

	@Test
	@Override
	public void testUpdateFromRestValidSimpleValue() {
		try (Tx tx = tx()) {
			InternalActionContext ac = mockActionContext();
			invokeUpdateFromRestValidSimpleValueTestcase(NODE_FIELD, FILL, (container) -> {
				NodeFieldImpl field = new NodeFieldImpl();
				field.setUuid(content().getUuid());
				updateContainer(ac, container, NODE_FIELD, field);
			}, (container) -> {
				NodeGraphField field = container.getNode(NODE_FIELD);
				assertNotNull("The graph field {" + NODE_FIELD + "} could not be found.", field);
				assertEquals("The node reference of the field was not updated.", content().getUuid(), field.getNode().getUuid());
			});
		}
	}
}
