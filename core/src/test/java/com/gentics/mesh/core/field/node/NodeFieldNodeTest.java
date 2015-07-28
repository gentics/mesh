package com.gentics.mesh.core.field.node;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.NodeField;
import com.gentics.mesh.core.rest.node.field.impl.NodeFieldImpl;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.NodeFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;

public class NodeFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws JsonParseException, JsonMappingException, IOException {
		setupData();
	}

	@Test
	public void testNodeFieldTransformation() throws IOException, InterruptedException {
		Node newsNode = data().getFolder("news");

		Node node = data().getFolder("2015");
		Schema schema = node.getSchema();
		NodeFieldSchema nodeFieldSchema = new NodeFieldSchemaImpl();
		nodeFieldSchema.setName("nodeField");
		nodeFieldSchema.setAllowedSchemas("folder");
		schema.addField(nodeFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeFieldContainer container = node.getFieldContainer(data().getEnglish());
		container.createNode("nodeField", newsNode);

		String json = getJson(node);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);

		NodeField deserializedNodeField = response.getField("nodeField", NodeFieldImpl.class);
		assertNotNull(deserializedNodeField);
		assertEquals(newsNode.getUuid(), deserializedNodeField.getUuid());

	}

	private String getJson(Node node) throws InterruptedException {
		RoutingContext rc = getMockedRoutingContext("lang=en");
		CountDownLatch latch = new CountDownLatch(1);
		AtomicReference<String> reference = new AtomicReference<>();
		node.transformToRest(rc, rh -> {
			NodeResponse response = rh.result();
			reference.set(JsonUtil.toJson(response));
			assertNotNull(response);
			latch.countDown();
		});
		latch.await();
		return reference.get();
	}
}
