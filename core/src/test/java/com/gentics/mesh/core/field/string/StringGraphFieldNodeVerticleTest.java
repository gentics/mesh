package com.gentics.mesh.core.field.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.basic.StringGraphField;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

public class StringGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getSchema();
		StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		stringFieldSchema.setName("stringField");
		stringFieldSchema.setLabel("Some label");
		schema.addField(stringFieldSchema);
		schemaContainer("folder").setSchema(schema);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("stringField", new StringFieldImpl().setString("addedString"));
		StringFieldImpl field = response.getField("stringField");
		assertEquals("addedString", field.getString());

		response = updateNode("stringField", new StringFieldImpl().setString("updatedString2"));
		field = response.getField("stringField");
		assertEquals("updatedString2", field.getString());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("stringField", new StringFieldImpl().setString("someString"));
		StringFieldImpl field = response.getField("stringField");
		assertEquals("someString", field.getString());
	}

	@Test
	@Override
	public void testReadNodeWithExitingField() {
		Node node = folder("2015");

		NodeFieldContainer container = node.getFieldContainer(english());
		StringGraphField stringField = container.createString("stringField");
		stringField.setString("someString");

		NodeResponse response = readNode(node);

		StringFieldImpl deserializedStringField = response.getField("stringField", StringFieldImpl.class);
		assertNotNull(deserializedStringField);
		assertEquals("someString", deserializedStringField.getString());
	}

}
