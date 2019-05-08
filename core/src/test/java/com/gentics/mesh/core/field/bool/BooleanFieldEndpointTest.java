package com.gentics.mesh.core.field.bool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.BooleanGraphField;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.impl.BooleanFieldImpl;
import com.gentics.mesh.core.rest.schema.BooleanFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.impl.BooleanFieldSchemaImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class BooleanFieldEndpointTest extends AbstractFieldEndpointTest {

	private static final String FIELD_NAME = "booleanField";

	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {
			SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			BooleanFieldSchema booleanFieldSchema = new BooleanFieldSchemaImpl();
			booleanFieldSchema.setName(FIELD_NAME);
			booleanFieldSchema.setLabel("Some label");
			schema.addField(booleanFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			NodeGraphFieldContainer container = node.getLatestDraftFieldContainer(english());
			container.createBoolean(FIELD_NAME).setBoolean(true);
			tx.success();
		}
		try (Tx tx = tx()) {
			Node node = folder("2015");
			NodeResponse response = readNode(node);
			BooleanFieldImpl deserializedBooleanField = response.getFields().getBooleanField(FIELD_NAME);
			assertNotNull(deserializedBooleanField);
			assertTrue(deserializedBooleanField.getValue());
		}
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		Node node = folder("2015");
		for (int i = 0; i < 20; i++) {
			boolean flag = false;
			NodeGraphFieldContainer container = tx(() -> node.getGraphFieldContainer("en"));
			final NodeGraphFieldContainer currentContainer = container;
			Boolean oldValue = tx(() -> getBooleanValue(currentContainer, FIELD_NAME));
			String expectedVersion = tx(() -> currentContainer.getVersion().nextDraft().toString());

			NodeResponse response = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(flag));
			BooleanFieldImpl field = response.getFields().getBooleanField(FIELD_NAME);
			assertEquals(flag, field.getValue());
			assertEquals("The version within the response should be bumped by one minor version.", expectedVersion, response.getVersion());

			try (Tx tx = tx()) {
				assertEquals("Check old value", oldValue, getBooleanValue(container, FIELD_NAME));
				container = node.getGraphFieldContainer("en");
				oldValue = getBooleanValue(container, FIELD_NAME);
				response = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(!flag));
				field = response.getFields().getBooleanField(FIELD_NAME);
				assertEquals(!flag, field.getValue());
				assertEquals("Check version number", container.getVersion().nextDraft().toString(), response.getVersion());
				assertEquals("Check old value", oldValue, getBooleanValue(container, FIELD_NAME));
			}
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		try (Tx tx = tx()) {
			NodeResponse firstResponse = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
			String oldVersion = firstResponse.getVersion();

			NodeResponse secondResponse = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
			assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldVersion);
		}
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new BooleanFieldImpl());
		assertThat(secondResponse.getFields().getBooleanField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			Node node = folder("2015");
			NodeGraphFieldContainer latest = node.getLatestDraftFieldContainer(english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getBoolean(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getBoolean(FIELD_NAME)).isNotNull();
			Boolean oldValue = latest.getPreviousVersion().getBoolean(FIELD_NAME).getBoolean();
			assertThat(oldValue).isEqualTo(true);

			NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
			assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
				secondResponse.getVersion());
		}
	}

	@Test
	@Override
	@Ignore
	public void testUpdateSetEmpty() {
		// Boolean fields can be set to empty - thus this is covered by the set null test
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
		try (Tx tx = tx()) {
			NodeResponse response = createNode(FIELD_NAME, (Field) null);
			BooleanFieldImpl field = response.getFields().getBooleanField(FIELD_NAME);
			assertNull(field);
		}
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		try (Tx tx = tx()) {
			NodeResponse response = createNodeWithField();
			BooleanFieldImpl field = response.getFields().getBooleanField(FIELD_NAME);
			assertTrue(field.getValue());
		}
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new BooleanFieldImpl().setValue(true));
	}
}
