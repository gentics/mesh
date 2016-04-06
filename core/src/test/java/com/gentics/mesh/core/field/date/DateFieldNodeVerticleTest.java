package com.gentics.mesh.core.field.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.DateGraphField;
import com.gentics.mesh.core.field.AbstractFieldNodeVerticleTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.DateField;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.DateFieldImpl;
import com.gentics.mesh.core.rest.schema.DateFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.DateFieldSchemaImpl;

public class DateFieldNodeVerticleTest extends AbstractFieldNodeVerticleTest {
	private static final String FIELD_NAME = "dateField";

	@Before
	public void updateSchema() throws IOException {
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		DateFieldSchema dateFieldSchema = new DateFieldSchemaImpl();
		dateFieldSchema.setName(FIELD_NAME);
		dateFieldSchema.setLabel("Some label");
		schema.addField(dateFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(FIELD_NAME, (Field) null);
		DateFieldImpl field = response.getFields().getDateField(FIELD_NAME);
		assertNotNull(field);
		assertNull(field.getDate());
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		for (int i = 0; i < 20; i++) {
			Long nowEpoch = System.currentTimeMillis() / 1000 + i;
			NodeGraphFieldContainer container = node.getGraphFieldContainer("en");
			Long oldValue = getDateValue(container, FIELD_NAME);

			NodeResponse response = updateNode(FIELD_NAME, new DateFieldImpl().setDate(nowEpoch));
			DateFieldImpl field = response.getFields().getDateField(FIELD_NAME);
			assertEquals(nowEpoch, field.getDate());

			node.reload();
			container.reload();
			assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion().getNumber());
			assertEquals("Check old value", oldValue, getDateValue(container, FIELD_NAME));
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(nowEpoch));
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(nowEpoch));
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeResponse firstResponse = updateNode(FIELD_NAME, new DateFieldImpl().setDate(nowEpoch));
		String oldNumber = firstResponse.getVersion().getNumber();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new DateFieldImpl());
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME)).as("Updated Field").isNotNull();
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME).getDate()).as("Updated Field Value").isNull();
		assertThat(secondResponse.getVersion().getNumber()).as("New version number").isNotEqualTo(oldNumber);
	}

	/**
	 * Get the date value
	 * @param container container
	 * @param fieldName field name
	 * @return date value (may be null)
	 */
	protected Long getDateValue(NodeGraphFieldContainer container, String fieldName) {
		DateGraphField field = container.getDate(fieldName);
		return field != null ? field.getDate() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		Long nowEpoch = System.currentTimeMillis() / 1000;
		NodeResponse response = createNode(FIELD_NAME, new DateFieldImpl().setDate(nowEpoch));
		DateField field = response.getFields().getDateField(FIELD_NAME);
		assertEquals(nowEpoch, field.getDate());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		Long nowEpoch = System.currentTimeMillis() / 1000;

		Node node = folder("2015");
		NodeGraphFieldContainer container = node.getGraphFieldContainer(english());
		container.createDate(FIELD_NAME).setDate(nowEpoch);

		NodeResponse response = readNode(node);
		DateField deserializedDateField = response.getFields().getDateField(FIELD_NAME);
		assertNotNull(deserializedDateField);
		assertEquals(nowEpoch, deserializedDateField.getDate());
	}
}
