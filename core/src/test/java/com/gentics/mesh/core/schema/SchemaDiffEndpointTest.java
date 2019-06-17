package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.AUTO_PURGE_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.CONTAINER_FLAG_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel.DISPLAY_FIELD_NAME_KEY;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.ADDFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.REMOVEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATEFIELD;
import static com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation.UPDATESCHEMA;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.LinkedHashMap;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.schema.BinaryFieldSchema;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.HtmlFieldSchema;
import com.gentics.mesh.core.rest.schema.NodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.StringFieldSchema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.HtmlFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.StringFieldSchemaImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaDiffEndpointTest extends AbstractMeshTest {

	private Schema getSchema() {
		Schema schema = new SchemaModelImpl();
		schema.setName("content");
		schema.setDescription("Content schema for blogposts");
		schema.setDisplayField("title");
		schema.setSegmentField("slug");

		StringFieldSchema slugFieldSchema = new StringFieldSchemaImpl();
		slugFieldSchema.setName("slug");
		slugFieldSchema.setLabel("Slug");
		slugFieldSchema.setRequired(true);
		schema.addField(slugFieldSchema);

		StringFieldSchema titleFieldSchema = new StringFieldSchemaImpl();
		titleFieldSchema.setName("title");
		titleFieldSchema.setLabel("Title");
		schema.addField(titleFieldSchema);

		StringFieldSchema nameFieldSchema = new StringFieldSchemaImpl();
		nameFieldSchema.setName("teaser");
		nameFieldSchema.setLabel("Teaser");
		nameFieldSchema.setRequired(true);
		schema.addField(nameFieldSchema);

		HtmlFieldSchema contentFieldSchema = new HtmlFieldSchemaImpl();
		contentFieldSchema.setName("content");
		contentFieldSchema.setLabel("Content");
		schema.addField(contentFieldSchema);

		schema.setContainer(false);
		return schema;
	}

	@Test
	public void testDiffContainerFlag() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		Schema request = getSchema();

		// Flag not specified
		request.setContainer(null);
		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(0);

		// Set to same value
		request.setContainer(false);
		changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(0);

		// Flag set to different value
		request.setContainer(true);
		changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(1);
		assertThat(changes.getChanges().get(0)).is(UPDATESCHEMA).hasProperty(CONTAINER_FLAG_KEY, true);
	}

	@Test
	public void testDiffAutoPurgeFlag() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		Schema request = getSchema();

		// Flag not specified
		request.setAutoPurge(null);
		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(0);

		// Set to same value
		request.setAutoPurge(false);
		changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges().get(0)).is(UPDATESCHEMA).hasProperty(AUTO_PURGE_FLAG_KEY, false);

		// Flag set to different value
		request.setAutoPurge(true);
		changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(1);
		assertThat(changes.getChanges().get(0)).is(UPDATESCHEMA).hasProperty(AUTO_PURGE_FLAG_KEY, true);

	}

	@Test
	public void testDiffDescription() {
		final String SCHEMA_NAME = "TestSchema";

		// 1. Create empty schema
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName(SCHEMA_NAME);
		SchemaResponse response = call(() -> client().createSchema(request));
		assertNull(response.getDescription());

		// 2. Diff the schema with itself
		SchemaChangesListModel changes = call(() -> client().diffSchema(response.getUuid(), response));
		assertThat(changes.getChanges()).isEmpty();

		// 3. Diff with description set
		response.setDescription("SetToSomething");
		changes = call(() -> client().diffSchema(response.getUuid(), response));
		assertThat(changes.getChanges()).hasSize(1);
		SchemaChangeModel change = changes.getChanges().get(0);
		assertEquals("SetToSomething", change.getProperty("description"));

	}

	@Test
	public void testDiffDisplayField() throws GenericRestException, Exception {
		try (Tx tx = tx()) {
			SchemaContainer container = schemaContainer("content");
			Schema request = getSchema();
			request.setDisplayField("slug");

			SchemaChangesListModel changes = call(() -> client().diffSchema(container.getUuid(), request));
			assertNotNull(changes);
			// We expect one change that indicates that the displayField property has changed.
			assertThat(changes.getChanges()).hasSize(1);
			assertThat(changes.getChanges().get(0)).is(UPDATESCHEMA).hasProperty(DISPLAY_FIELD_NAME_KEY, "slug");
		}
	}

	@Test
	public void testNoDiff() {
		try (Tx tx = tx()) {
			SchemaContainer container = schemaContainer("content");
			Schema request = getSchema();
			SchemaChangesListModel changes = call(() -> client().diffSchema(container.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).isEmpty();
		}
	}

	@Test
	public void testAllowFieldDiff() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add allowed property to slug
		try (Tx tx = tx()) {
			SchemaContainerVersion version = schemaContainer("content").getLatestVersion();
			SchemaModel schema = version.getSchema();
			schema.getField("slug", StringFieldSchema.class).setAllowedValues("A", "B", "C");
			version.setJson(schema.toJson());
			tx.success();
		}

		Schema request = getSchema();
		StringFieldSchema field = request.getField("slug", StringFieldSchema.class);
		field.setAllowedValues("a", "b", "c");

		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(1);
		SchemaChangeModel change = changes.getChanges().get(0);
		assertThat(change).is(UPDATEFIELD).forField("slug").hasProperty("allow", Arrays.asList("a", "b", "c").toArray());
	}

	@Test
	public void testAllowNullFieldDiff() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add allowed property to slug
		try (Tx tx = tx()) {
			SchemaContainerVersion version = schemaContainer("content").getLatestVersion();
			SchemaModel schema = version.getSchema();
			schema.getField("slug", StringFieldSchema.class).setAllowedValues("A", "B", "C");
			version.setJson(schema.toJson());
			tx.success();
		}

		// Get the content schema (The schema does not contain the allow property)
		Schema request = getSchema();

		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(1);
		SchemaChangeModel change = changes.getChanges().get(0);
		assertThat(change).is(UPDATEFIELD).forField("slug").hasProperty("allow", null);
	}

	@Test
	public void testAllowEmptyFieldDiff() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add allowed property to slug
		try (Tx tx = tx()) {
			SchemaContainerVersion version = schemaContainer("content").getLatestVersion();
			SchemaModel schema = version.getSchema();
			schema.getField("slug", StringFieldSchema.class).setAllowedValues("A", "B", "C");
			version.setJson(schema.toJson());
			tx.success();
		}

		// Get the content schema (The schema does not contain the allow property)
		Schema request = getSchema();
		request.getField("slug", StringFieldSchema.class).setAllowedValues();

		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(1);
		SchemaChangeModel change = changes.getChanges().get(0);
		assertThat(change).is(UPDATEFIELD).forField("slug").hasProperty("allow", new String[0]);
	}

	@Test
	public void testESFieldDiff() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		Schema request = getSchema();
		FieldSchema field = request.getField("slug");
		JsonObject setting = new JsonObject().put("test", "123");
		field.setElasticsearch(setting);

		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));
		assertThat(changes.getChanges()).hasSize(1);
		SchemaChangeModel change = changes.getChanges().get(0);
		assertThat(change).is(UPDATEFIELD).forField("slug").hasProperty("elasticsearch", setting);
	}

	/**
	 * Diff the schema field with a schema field which sets the elasticsearch setting to null. Internally that should be transformed to set the setting to empty
	 * json object.
	 */
	@Test
	public void testESFieldNullDiff() {
		assertESHandlingForValue(null);
	}

	@Test
	public void testESFieldNullDiff2() {
		assertESHandlingForValue(new JsonObject());
	}

	private void assertESHandlingForValue(JsonObject newValueForSetting) {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		// Add elasticsearch setting to content field
		try (Tx tx = tx()) {
			JsonObject setting = new JsonObject().put("test", "123");
			SchemaContainerVersion version = schemaContainer("content").getLatestVersion();
			SchemaModel schema = version.getSchema();
			schema.getField("slug").setElasticsearch(setting);
			version.setJson(schema.toJson());
			tx.success();
		}

		// Diff with es setting in field set to null
		Schema request = getSchema();
		FieldSchema field = request.getField("slug");
		field.setElasticsearch(newValueForSetting);
		SchemaChangesListModel changes = call(() -> client().diffSchema(schemaUuid, request));

		assertThat(changes.getChanges()).hasSize(1);
		SchemaChangeModel change = changes.getChanges().get(0);
		System.out.println(change.toJson());
		assertThat(change).is(UPDATEFIELD).forField("slug").hasProperty("elasticsearch", new LinkedHashMap<>());

	}

	@Test
	public void testAddField() {
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			Schema request = getSchema();
			BinaryFieldSchema binaryField = FieldUtil.createBinaryFieldSchema("binary");
			binaryField.setAllowedMimeTypes("one", "two");
			request.addField(binaryField);

			SchemaChangesListModel changes = call(() -> client().diffSchema(schema.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(ADDFIELD).forField("binary");
			assertThat(changes.getChanges().get(1)).is(UPDATESCHEMA).hasProperty("order",
				new String[] { "slug", "title", "teaser", "content", "binary" });
		}
	}

	@Test
	public void testAddNodeField() {
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			Schema request = getSchema();
			NodeFieldSchema nodeField = FieldUtil.createNodeFieldSchema("node");
			nodeField.setAllowedSchemas("content");
			request.addField(nodeField);

			SchemaChangesListModel changes = call(() -> client().diffSchema(schema.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(ADDFIELD).forField("node").hasProperty("allow", new String[] { "content" });
			assertThat(changes.getChanges().get(1)).is(UPDATESCHEMA).hasProperty("order",
				new String[] { "slug", "title", "teaser", "content", "node" });
		}
	}

	@Test
	public void testDefaultMigration() {
		try (Tx tx = tx()) {
			SchemaContainer schema = schemaContainer("content");
			Schema request = getSchema();
			request.removeField("content");

			SchemaChangesListModel changes = call(() -> client().diffSchema(schema.getUuid(), request));
			assertNotNull(changes);
			assertThat(changes.getChanges()).hasSize(2);
			assertThat(changes.getChanges().get(0)).is(REMOVEFIELD).forField("content");
			assertThat(changes.getChanges().get(1)).is(UPDATESCHEMA).hasProperty("order", new String[] { "slug", "title", "teaser" });
		}
	}

}
