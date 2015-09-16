package com.gentics.mesh.core.field.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.field.bool.AbstractBasicDBTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class NodeGraphFieldNodeTest extends AbstractBasicDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	public void testNodeFieldTransformation() throws Exception {
		Node newsNode = folder("news");

		Node node = folder("2015");
		Schema schema = node.getSchema();
		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setName("nodeField");
		nodeFieldSchema.setAllowedSchemas("folder");
		schema.addField(nodeFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createNode("nodeField", newsNode);

		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);

		NodeField deserializedNodeField = response.getField("nodeField", NodeFieldImpl.class);
		assertNotNull(deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());
	}

}
