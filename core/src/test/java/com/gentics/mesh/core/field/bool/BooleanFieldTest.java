package com.gentics.mesh.core.field.bool;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.AbstractBasicDBTest;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.data.node.field.impl.BooleanGraphFieldImpl;
import com.gentics.mesh.core.field.AbstractFieldTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;

public class BooleanFieldTest extends AbstractFieldTest {

	private static final String BOOLEAN_FIELD = "booleanField";

	@Test
	@Override
	public void testFieldTransformation() throws Exception {
		Node node = folder("2015");

		// Update the schema and add a boolean field
		Schema schema = node.getSchemaContainer().getLatestVersion().getSchema();
		BooleanFieldSchemaImpl booleanFieldSchema = new BooleanFieldSchemaImpl();
		booleanFieldSchema.setName("booleanField");
		booleanFieldSchema.setLabel("Some boolean field");
		booleanFieldSchema.setRequired(true);
		schema.addField(booleanFieldSchema);
		node.getSchemaContainer().getLatestVersion().setSchema(schema);

		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		BooleanGraphField field = container.createBoolean("booleanField");
		field.setBoolean(true);

		String json = getJson(node);
		assertTrue("The json should contain the boolean field but it did not.{" + json + "}", json.indexOf("booleanField\" : true") > 1);
		assertNotNull(json);
		NodeResponse response = JsonUtil.readValue(json, NodeResponse.class);
		assertNotNull(response);

		com.gentics.mesh.core.rest.node.field.BooleanField deserializedNodeField = response.getFields().getBooleanField("booleanField");
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
		assertNull(container.getProperty("test-boolean"));
		assertNull(field.getBoolean());
	}

	@Test
	public void testBooleanField() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphField booleanField = container.createBoolean("booleanField");
		assertEquals("booleanField", booleanField.getFieldKey());
		booleanField.setBoolean(true);
		assertTrue("The boolean field value was not changed to true", booleanField.getBoolean());

		booleanField.setBoolean(false);
		assertFalse("The boolean field value was not changed to false", booleanField.getBoolean());

		booleanField.setBoolean(null);
		assertNull("The boolean field value was not set to null.", booleanField.getBoolean());

		BooleanGraphField bogusField2 = container.getBoolean("bogus");
		assertNull("No field with the name bogus should have been found.", bogusField2);

		BooleanGraphField reloadedBooleanField = container.getBoolean("booleanField");
		assertNull("The boolean field value was set to null and thus the field should have been removed.", reloadedBooleanField);
	}

	@Test
	@Override
	public void testClone() {
		NodeGraphFieldContainerImpl container = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		BooleanGraphField trueBooleanField = container.createBoolean("trueBooleanField");
		trueBooleanField.setBoolean(true);
		BooleanGraphField falseBooleanField = container.createBoolean("falseBooleanField");
		falseBooleanField.setBoolean(false);

		NodeGraphFieldContainerImpl otherContainer = tx.getGraph().addFramedVertex(NodeGraphFieldContainerImpl.class);
		trueBooleanField.cloneTo(otherContainer);
		falseBooleanField.cloneTo(otherContainer);

		assertThat(otherContainer.getBoolean("trueBooleanField")).as("cloned true field").isNotNull().isEqualToIgnoringGivenFields(trueBooleanField,
				"parentContainer");
		assertThat(otherContainer.getBoolean("falseBooleanField")).as("cloned false field").isNotNull()
				.isEqualToIgnoringGivenFields(falseBooleanField, "parentContainer");
	}
}
