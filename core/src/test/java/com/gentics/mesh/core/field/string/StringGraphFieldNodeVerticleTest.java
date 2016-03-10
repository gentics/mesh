package com.gentics.mesh.core.field.string;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.StringGraphField;
import com.gentics.mesh.core.field.AbstractGraphFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;

import io.netty.handler.codec.http.HttpResponseStatus;

public class StringGraphFieldNodeVerticleTest extends AbstractGraphFieldNodeVerticleTest {

	/**
	 * Update the schema and add a string field.
	 * 
	 * @throws IOException
	 */
	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();

		// add non restricted string field
		StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
		stringFieldSchema.setName("stringField");
		stringFieldSchema.setLabel("Some label");
		schema.addField(stringFieldSchema);

		// add restricted string field
		StringFieldSchema restrictedStringFieldSchema = new StringFieldSchemaImpl();
		restrictedStringFieldSchema.setName("restrictedstringField");
		restrictedStringFieldSchema.setLabel("Some label");
		restrictedStringFieldSchema.setAllowedValues(new String[] { "one", "two", "three" });
		schema.addField(restrictedStringFieldSchema);

		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(null, (Field) null);
		StringFieldImpl stringField = response.getFields().getStringField("stringField");
		assertNotNull(stringField);
		assertNull(stringField.getString());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		NodeResponse response = updateNode("stringField", new StringFieldImpl().setString("addedString"));
		StringFieldImpl field = response.getFields().getStringField("stringField");
		assertEquals("addedString", field.getString());

		response = updateNode("stringField", new StringFieldImpl().setString("updatedString2"));
		field = response.getFields().getStringField("stringField");
		assertEquals("updatedString2", field.getString());
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode("stringField", new StringFieldImpl().setString("someString"));
		StringFieldImpl field = response.getFields().getStringField("stringField");
		assertEquals("someString", field.getString());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		StringGraphField stringField = container.createString("stringField");
		stringField.setString("someString");
		NodeResponse response = readNode(node);
		StringFieldImpl deserializedStringField = response.getFields().getStringField("stringField");
		assertNotNull(deserializedStringField);
		assertEquals("someString", deserializedStringField.getString());
	}

	@Test
	public void testValueRestrictionValidValue() {
		NodeResponse response = updateNode("restrictedstringField", new StringFieldImpl().setString("two"));
		StringFieldImpl field = response.getFields().getStringField("restrictedstringField");
		assertEquals("two", field.getString());
	}

	@Test
	public void testValueRestrictionInvalidValue() {
		updateNodeFailure("restrictedstringField", new StringFieldImpl().setString("invalid"), HttpResponseStatus.BAD_REQUEST,
				"node_error_invalid_string_field_value", "restrictedstringField", "invalid");
	}
}
