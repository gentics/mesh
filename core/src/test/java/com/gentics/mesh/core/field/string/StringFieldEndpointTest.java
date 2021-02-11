package com.gentics.mesh.core.field.string;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibStringField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.StringField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.netty.handler.codec.http.HttpResponseStatus;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class StringFieldEndpointTest extends AbstractFieldEndpointTest {

	private static final String FIELD_NAME = "stringField";

	/**
	 * Update the schema and add a string field.
	 * 
	 * @throws IOException
	 */
	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaVersionModel schema = schemaContainer("folder").getLatestVersion().getSchema();

			// add non restricted string field
			StringFieldSchema stringFieldSchema = new StringFieldSchemaImpl();
			stringFieldSchema.setName(FIELD_NAME);
			stringFieldSchema.setLabel("Some label");
			schema.addField(stringFieldSchema);

			// add restricted string field
			StringFieldSchema restrictedStringFieldSchema = new StringFieldSchemaImpl();
			restrictedStringFieldSchema.setName("restrictedstringField");
			restrictedStringFieldSchema.setLabel("Some label");
			restrictedStringFieldSchema.setAllowedValues(new String[] { "one", "two", "three" });
			schema.addField(restrictedStringFieldSchema);

			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNode(null, (Field) null);
			StringFieldImpl stringField = response.getFields().getStringField(FIELD_NAME);
			assertNull(stringField);
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		for (int i = 0; i < 20; i++) {
			try (Tx tx = tx()) {
				HibNode node = folder("2015");
				HibNodeFieldContainer container = boot().contentDao().getGraphFieldContainer(node, "en");
				String oldValue = getStringValue(container, FIELD_NAME);

				String newValue = "content " + i;

				NodeResponse response = updateNode(FIELD_NAME, new StringFieldImpl().setString(newValue));
				StringFieldImpl field = response.getFields().getStringField(FIELD_NAME);
				assertEquals(newValue, field.getString());

				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getStringValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
		String oldNumber = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		NodeResponse firstResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getStringField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getString(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getString(FIELD_NAME)).isNotNull();
			String oldValue = latest.getPreviousVersion().getString(FIELD_NAME).getString();
			assertThat(oldValue).isEqualTo("bla");

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new StringFieldImpl().setString("bla"));
		StringField emptyField = new StringFieldImpl();
		emptyField.setString("");
		NodeResponse secondResponse = null;
		try (Tx tx = tx()) {
			String oldVersion = firstResponse.getVersion();
			secondResponse = updateNode(FIELD_NAME, emptyField);
			assertThat(secondResponse.getFields().getStringField(FIELD_NAME)).as("Updated Field").isNotNull();
			assertThat(secondResponse.getFields().getStringField(FIELD_NAME).getString()).as("Updated Field Value").isEqualTo("");
			assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);
		}
		NodeResponse thirdResponse = updateNode(FIELD_NAME, emptyField);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(), secondResponse.getVersion());

	}

	/**
	 * Get the string value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return string value (may be null)
	 */
	protected String getStringValue(HibNodeFieldContainer container, String fieldName) {
		HibStringField field = container.getString(fieldName);
		return field != null ? field.getString() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			StringFieldImpl field = response.getFields().getStringField(FIELD_NAME);
			assertEquals("someString", field.getString());
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		HibNode node = folder("2015");
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNodeFieldContainer container = contentDao.getLatestDraftFieldContainer(node, english());
			HibStringField stringField = container.createString(FIELD_NAME);
			stringField.setString("someString");
			tx.success();
		}

		try (Tx tx = tx()) {
			NodeResponse response = readNode(node);
			StringFieldImpl deserializedStringField = response.getFields().getStringField(FIELD_NAME);
			assertNotNull(deserializedStringField);
			assertEquals("someString", deserializedStringField.getString());
		}
	}

	@Test
	public void testValueRestrictionValidValue() {
		try (Tx tx = tx()) {
			NodeResponse response = updateNode("restrictedstringField", new StringFieldImpl().setString("two"));
			StringFieldImpl field = response.getFields().getStringField("restrictedstringField");
			assertEquals("two", field.getString());
		}
	}

	@Test
	public void testValueRestrictionInvalidValue() {
		try (Tx tx = tx()) {
			updateNodeFailure("restrictedstringField", new StringFieldImpl().setString("invalid"), HttpResponseStatus.BAD_REQUEST,
				"node_error_invalid_string_field_value", "restrictedstringField", "invalid");
		}
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new StringFieldImpl().setString("someString"));
	}
}
