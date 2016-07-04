package com.gentics.mesh.core.field.bool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;

public class BooleanFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {

	private static final String FIELD_NAME = "booleanField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		BooleanFieldSchema booleanFieldSchema = new BooleanFieldSchemaImpl();
		booleanFieldSchema.setName(FIELD_NAME);
		booleanFieldSchema.setLabel("Some label");
		schema.addField(booleanFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Node node = folder("2015");
		NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
		container.createBoolean(FIELD_NAME).setBoolean(true);
		NodeResponse response = readNode(node);
		BooleanFieldImpl deserializedBooleanField = response.getFields().getBooleanField(FIELD_NAME);
		assertNotNull(deserializedBooleanField);
		assertTrue(deserializedBooleanField.getValue());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		for (int i = 0; i < 20; i++) {
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			Boolean oldValue = getBooleanValue(container, FIELD_NAME);

			String expectedVersion = container.getVersion().nextDraft().toString();
			boolean flag = false;
			NodeResponse response = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(flag));
			BooleanFieldImpl field = response.getFields().getBooleanField(FIELD_NAME);
			assertEquals(flag, field.getValue());
			node.reload();
			container.reload();
			assertEquals("The version within the response should be bumped by one minor version.", expectedVersion,
					response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getBooleanValue(container, FIELD_NAME));

			container = node.getGraphFieldContainer("en");
			oldValue = getBooleanValue(container, FIELD_NAME);
			response = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(!flag));
			field = response.getFields().getBooleanField(FIELD_NAME);
			assertEquals(!flag, field.getValue());
			node.reload();
			container.reload();
			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getBooleanValue(container, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new BooleanFieldImpl());
		assertThat(secondResponse.getFields().getBooleanField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldNumber);
	}

	@Test
	
	@Override
	public void testUpdateSetEmpty() {
		// TODO Auto-generated method stub
		
	}
	
	/**
	 * Get boolean value
	 * 
	 * @param container
	 *            field container
	 * @param fieldName
	 *            field name
	 * @return value
	 */
	protected Boolean getBooleanValue(NodeGraphFieldContainer container, String fieldName) {
		BooleanGraphField field = container.getBoolean(fieldName);
		return field != null ? field.getBoolean() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		BooleanFieldImpl field = response.getFields().getBooleanField(FIELD_NAME);
		assertNull(field);
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
		BooleanFieldImpl field = response.getFields().getBooleanField(FIELD_NAME);
		assertTrue(field.getValue());
	}
}
