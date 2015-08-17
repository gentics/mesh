package com.gentics.mesh.core.field.bool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.BooleanGraphField;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;

public class BooleanGraphFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testBooleanFieldTransformation() throws IOException, InterruptedException {
		Node node = folder("2015");
		Schema schema = node.getSchema();
		BooleanFieldSchemaImpl booleanFieldSchema = new BooleanFieldSchemaImpl();
		booleanFieldSchema.setName("booleanField");
		booleanFieldSchema.setLabel("Some boolean field");
		booleanFieldSchema.setRequired(true);
		schema.addField(booleanFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeFieldContainer container = node.getFieldContainer(english());
		BooleanGraphField field = container.createBoolean("booleanField");
		field.setBoolean(true);

		String json = getJson(node);
		assertTrue("The json should contain the boolean field but it did not.{" + json + "}", json.indexOf("booleanField\" : true") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
		assertNotNull(response);

		com.gentics.mesh.core.rest.node.field.BooleanField deserializedNodeField = response.getField("booleanField", BooleanFieldImpl.class);
		assertNotNull(deserializedNodeField);
		assertEquals(true, deserializedNodeField.getValue());

	}

}