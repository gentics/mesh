package com.gentics.mesh.core.field.node;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.nesting.NodeGraphField;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractEmptyDBTest;

public class NodeGraphFieldTest extends AbstractEmptyDBTest {

	final String NODE_FIELD_NAME = "nodeField";

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	public void testNodeFieldTransformation() throws Exception {
		setupData();
		Node newsNode = folder("news");
		Node node = folder("2015");
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();

		// 1. Create the node field schema and add it to the schema of the node
		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setName(NODE_FIELD_NAME);
		nodeFieldSchema.setAllowedSchemas("folder");
		schema.addField(nodeFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);
		schemaStorage.addSchema(node.getSchemaContainer().getLatestVersion().getSchema());

		// 2. Add the node reference to the node fields
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createNode(NODE_FIELD_NAME, newsNode);

		// 3. Transform the node to json and examine the data
		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		NodeField deserializedNodeField = response.getFields().getNodeField(NODE_FIELD_NAME);
		assertNotNull("The field {" + NODE_FIELD_NAME + "} should not be null. Json: {" + json + "}", deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
	}

	@Test
	public void testSimpleNodeField() {
		Node node = tx.getGraph().addFramedVertex(NodeImpl.class);

		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NodeGraphField field = container.createNode("testNodeField", node);
		assertNotNull(field);
		assertEquals("testNodeField", field.getFieldKey());
		Node loadedNode = field.getNode();
		assertNotNull(loadedNode);
		assertEquals(node.getUuid(), loadedNode.getUuid());

		NodeGraphField loadedField = container.getNode("testNodeField");
		assertNotNull(loadedField);
		assertNotNull(loadedField.getNode());
		assertEquals(node.getUuid(), loadedField.getNode().getUuid());

	}

	@Test
	public void testClone() {
		Node node = tx.getGraph().addFramedVertex(NodeImpl.class);

		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		NodeGraphField testField = container.createNode("testField", node);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		testField.cloneTo(otherContainer);

		assertThat(otherContainer.getNode("testField")).as("cloned field").isNotNull();
		assertThat(otherContainer.getNode("testField").getNode()).as("cloned target node").isNotNull()
				.isEqualToComparingFieldByField(node);
	}
}
