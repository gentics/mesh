package com.gentics.mesh.core.field.bool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;

public class BooleanGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	private static final String FIELD_NAME = "booleanField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		BooleanFieldSchema booleanFieldSchema = new BooleanFieldSchemaImpl();
		booleanFieldSchema.setName(FIELD_NAME);
		booleanFieldSchema.setLabel("Some label");
		schema.addField(booleanFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createBoolean(FIELD_NAME).setBoolean(true);
		NodeResponse response = readNode(node);
		BooleanFieldImpl deserializedBooleanField = response.getField(FIELD_NAME);
		assertNotNull(deserializedBooleanField);
		assertTrue(deserializedBooleanField.getValue());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		for (int i = 0; i < 20; i++) {
			boolean flag = Math.random() > 0.5;
			NodeResponse response = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(flag));
			BooleanFieldImpl field = response.getField(FIELD_NAME);
			assertEquals(flag, field.getValue());
			response = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(!flag));
			field = response.getField(FIELD_NAME);
			assertEquals(!flag, field.getValue());
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		BooleanFieldImpl field = response.getField(FIELD_NAME);
		assertNotNull(field);
		assertNull(field.getValue());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
		BooleanFieldImpl field = response.getField(FIELD_NAME);
		assertTrue(field.getValue());
	}

}
