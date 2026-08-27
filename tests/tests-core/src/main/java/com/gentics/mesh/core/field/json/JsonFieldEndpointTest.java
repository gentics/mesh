package com.gentics.mesh.core.field.json;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.HibJsonField;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.field.AbstractFieldEndpointTest;
import com.gentics.mesh.core.rest.JsonSchema;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.Field;
import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.core.rest.node.field.JsonField;
import com.gentics.mesh.core.rest.node.field.impl.JsonFieldImpl;
import com.gentics.mesh.core.rest.schema.JsonFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.impl.JsonFieldSchemaImpl;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.util.VersionNumber;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;


@MeshTestSetting(testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class JsonFieldEndpointTest extends AbstractFieldEndpointTest {

	private static final String FIELD_NAME = "jsonField";

	/**
	 * Update the schema and add a json field.
	 * 
	 * @throws IOException
	 */
	@Before
	public void updateSchema() throws IOException {
		try (Tx tx = tx()) {

			// add non restricted json field
			JsonFieldSchema jsonFieldSchema = new JsonFieldSchemaImpl();
			jsonFieldSchema.setName(FIELD_NAME);
			jsonFieldSchema.setLabel("Some label");

			// add restricted json field
			JsonFieldSchema restrictedJsonFieldSchema = new JsonFieldSchemaImpl();
			restrictedJsonFieldSchema.setName("restrictedjsonField");
			restrictedJsonFieldSchema.setLabel("Some label");
			restrictedJsonFieldSchema.setAllowedSchemas(new JsonSchema("{\"type\":\"object\",\"properties\":{\"firstName\":{\"type\":\"string\"},\"lastName\":{\"type\":\"string\"}},\"required\":[\"firstName\",\"lastName\"]}"));

			prepareTypedSchema(schemaContainer("folder"), List.of(jsonFieldSchema, restrictedJsonFieldSchema), Optional.empty());
			tx.success();
		}
	}

	@Test
	@Override
	public void testCreateNodeWithNoField() {
		NodeResponse response = createNode(null, (Field) null);
		JsonFieldImpl jsonField = response.getFields().getJsonField(FIELD_NAME);
		assertNull(jsonField);
	}

	@Test
	@Override
	public void testUpdateNodeFieldWithField() {
		for (int i = 0; i < 20; i++) {
			VersionNumber oldVersion = tx(tx -> { return tx.contentDao().getFieldContainer(folder("2015"), "en").getVersion(); });

			JsonContent newValue = JsonFieldTestHelper.make("content " + i);

			NodeResponse response = updateNode(FIELD_NAME, new JsonFieldImpl().setJson(newValue));
			JsonFieldImpl field = response.getFields().getJsonField(FIELD_NAME);
			assertEquals(newValue, field.getJson());
			assertEquals("Check version number", oldVersion.nextDraft().toString(), response.getVersion());
		}
	}

	@Test
	@Override
	public void testUpdateSameValue() {
		NodeResponse firstResponse = updateNode(FIELD_NAME, new JsonFieldImpl().setJson(JsonFieldTestHelper.make("bla")));
		String oldNumber = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, new JsonFieldImpl().setJson(JsonFieldTestHelper.make("bla")));
		assertThat(secondResponse.getVersion()).as("New version number").isEqualTo(oldNumber);
	}

	@Test
	@Override
	public void testUpdateSetNull() {
		disableAutoPurge();

		JsonContent old = JsonFieldTestHelper.make("bla");
		NodeResponse firstResponse = updateNode(FIELD_NAME, new JsonFieldImpl().setJson(old));
		String oldVersion = firstResponse.getVersion();

		NodeResponse secondResponse = updateNode(FIELD_NAME, null);
		assertThat(secondResponse.getFields().getJsonField(FIELD_NAME)).as("Updated Field").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);

		// Assert that the old version was not modified
		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			HibNode node = folder("2015");
			HibNodeFieldContainer latest = contentDao.getLatestDraftFieldContainer(node, english());
			assertThat(latest.getVersion().toString()).isEqualTo(secondResponse.getVersion());
			assertThat(latest.getJson(FIELD_NAME)).isNull();
			assertThat(latest.getPreviousVersion().getJson(FIELD_NAME)).isNotNull();
			JsonContent oldValue = latest.getPreviousVersion().getJson(FIELD_NAME).getJson();
			assertThat(oldValue).isEqualTo(old);
		}
		NodeResponse thirdResponse = updateNode(FIELD_NAME, null);
		assertEquals("The field does not change and thus the version should not be bumped.", thirdResponse.getVersion(),
			secondResponse.getVersion());
	}

	@Test
	@Override
	public void testUpdateSetEmpty() {
		JsonContent content = JsonFieldTestHelper.make("bla");
		NodeResponse firstResponse = updateNode(FIELD_NAME, new JsonFieldImpl().setJson(content));
		JsonField emptyField = new JsonFieldImpl();
		emptyField.setJson(null);
		String oldVersion = firstResponse.getVersion();
		NodeResponse secondResponse = updateNode(FIELD_NAME, emptyField);
		assertThat(secondResponse.getFields().getDateField(FIELD_NAME)).as("Field Value").isNull();
		assertThat(secondResponse.getVersion()).as("New version number").isNotEqualTo(oldVersion);
	}

	/**
	 * Get the json value
	 * 
	 * @param container
	 *            container
	 * @param fieldName
	 *            field name
	 * @return json value (may be null)
	 */
	protected JsonContent getJsonValue(HibNodeFieldContainer container, String fieldName) {
		HibJsonField field = container.getJson(fieldName);
		return field != null ? field.getJson() : null;
	}

	@Test
	@Override
	public void testCreateNodeWithField() {
		NodeResponse response = createNodeWithField();
		JsonFieldImpl field = response.getFields().getJsonField(FIELD_NAME);
		assertEquals(JsonFieldTestHelper.make("someJson"), field.getJson());
	}

	@Test
	@Override
	public void testReadNodeWithExistingField() {
		JsonContent someJson = JsonFieldTestHelper.make("someJson");
		try (Tx tx = tx()) {
			HibNode node = folder("2015");
			ContentDao contentDao = tx.contentDao();

			HibNodeFieldContainer container = contentDao.createFieldContainer(node, english(),
					node.getProject().getLatestBranch(), user(),
					contentDao.getLatestDraftFieldContainer(node, english()), true);
			HibJsonField jsonField = container.createJson(FIELD_NAME);
			jsonField.setJson(someJson);
			tx.success();
		}
		NodeResponse response = readNode(folder("2015"));
		JsonFieldImpl deserializedJsonField = response.getFields().getJsonField(FIELD_NAME);
		assertNotNull(deserializedJsonField);
		assertEquals(someJson, deserializedJsonField.getJson());
	}

	@Test
	public void testValueRestrictionValidValue() {
		JsonContent valid = JsonContent.fromObject(new JsonObject().put("firstName", "Mickey").put("lastName", "Mouse"));
		NodeResponse response = updateNode("restrictedjsonField", new JsonFieldImpl().setJson(valid));
		JsonFieldImpl field = response.getFields().getJsonField("restrictedjsonField");
		assertEquals(valid, field.getJson());
	}

	@Test
	public void testValueRestrictionInvalidValue() {
		JsonContent invalid = JsonFieldTestHelper.make("whatever");
		updateNodeFailure("restrictedjsonField", new JsonFieldImpl().setJson(invalid), HttpResponseStatus.BAD_REQUEST,
				"node_error_invalid_json_field_value", "restrictedjsonField", JsonUtil.toJson(invalid));
	}
	
	@Test
	public void testValueRemoveValueRestrictions() {
		try (Tx tx = tx()) {
			SchemaVersionModel schema = schemaContainer("folder").getLatestVersion().getSchema();
			
			// unrestrict json field
			JsonFieldSchema restrictedJsonFieldSchema = schema.getField("restrictedjsonField", JsonFieldSchema.class);
			restrictedJsonFieldSchema.setAllowedSchemas();

			schemaContainer("folder").getLatestVersion().setSchema(schema);
			tx.success();
		}
		JsonContent valid = JsonFieldTestHelper.make("whatever");
		NodeResponse response = updateNode("restrictedjsonField", new JsonFieldImpl().setJson(valid));
		JsonFieldImpl field = response.getFields().getJsonField("restrictedjsonField");
		assertEquals(valid, field.getJson());
	}

	@Override
	public NodeResponse createNodeWithField() {
		return createNode(FIELD_NAME, new JsonFieldImpl().setJson(JsonFieldTestHelper.make("someJson")));
	}
}
