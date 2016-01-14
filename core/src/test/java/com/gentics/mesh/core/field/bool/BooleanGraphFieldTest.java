package com.gentics.mesh.core.field.bool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class BooleanGraphFieldTest extends AbstractBasicDBTest {

	@Autowired
	private ServerSchemaStorage schemaStorage;

	@Test
	public void testBooleanFieldTransformation() throws Exception {
		Node node = folder("2015");
		Schema schema = node.getSchema();
		BooleanFieldSchemaImpl booleanFieldSchema = new BooleanFieldSchemaImpl();
		booleanFieldSchema.setName("booleanField");
		booleanFieldSchema.setLabel("Some boolean field");
		booleanFieldSchema.setRequired(true);
		schema.addField(booleanFieldSchema);
		node.getSchemaContainer().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
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

	@Test
	public void testSimpleBoolean() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphFieldImpl field = new BooleanGraphFieldImpl("test", container);
		assertEquals(2, container.getPropertyKeys().size());
		assertNull(container.getProperty("test-boolean"));
		field.setBoolean(new Boolean(true));

		assertEquals("true", container.getProperty("test-boolean"));
		// assertEquals(5, container.getPropertyKeys().size());
		field.setBoolean(new Boolean(false));
		assertEquals("false", container.getProperty("test-boolean"));
		field.setBoolean(null);
		assertEquals("null", container.getProperty("test-boolean"));
		assertNull(field.getBoolean());
	}

	@Test
	public void testBooleanField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphField booleanField = container.createBoolean("booleanField");
		assertEquals("booleanField", booleanField.getFieldKey());
		booleanField.setBoolean(true);
		assertTrue(booleanField.getBoolean());
		booleanField.setBoolean(false);
		assertFalse(booleanField.getBoolean());
		booleanField.setBoolean(null);
		assertNull(booleanField.getBoolean());
		BooleanGraphField bogusField2 = container.getBoolean("bogus");
		assertNull(bogusField2);
		BooleanGraphField reloadedBooleanField = container.getBoolean("booleanField");
		assertNotNull(reloadedBooleanField);
		assertEquals("booleanField", reloadedBooleanField.getFieldKey());
	}
}
