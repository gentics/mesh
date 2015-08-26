package com.gentics.mesh.core.field.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractDBTest;

public class StringGraphFieldNodeTest extends AbstractDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testStringFieldTransformation() throws IOException, InterruptedException {
		try (Trx tx = new Trx(db)) {
			Node node = folder("2015");
			Schema schema = node.getSchema();
			StringFieldSchemaImpl stringFieldSchema = new StringFieldSchemaImpl();
			stringFieldSchema.setName("stringField");
			stringFieldSchema.setLabel("Some string field");
			stringFieldSchema.setRequired(true);
			schema.addField(stringFieldSchema);
			node.getSchemaContainer().setSchema(schema);

			NodeGraphFieldContainer container = node.getFieldContainer(english());
			StringGraphField field = container.createString("stringField");
			field.setString("someString");

			String json = getJson(node);
			assertTrue("The json should contain the string but it did not.{" + json + "}", json.indexOf("someString") > 1);
			assertNotNull(json);
			NodeResponse response = JsonUtil.readNode(json, NodeResponse.class, schemaStorage);
			assertNotNull(response);

			com.gentics.mesh.core.rest.node.field.StringField deserializedNodeField = response.getField("stringField", StringFieldImpl.class);
			assertNotNull(deserializedNodeField);
			assertEquals("someString", deserializedNodeField.getString());
		}
	}

}
