package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Objects;

import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.shaded.com.google.common.collect.Iterators;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.ElasticsearchTestMode;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.category.FailingTests;
import com.gentics.mesh.test.util.TestUtils;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = FULL, startServer = true)
public class SchemaChangesEndpointTest extends AbstractNodeSearchEndpointTest {

	public SchemaChangesEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Test
	public void testUpdateName() throws GenericRestException, Exception {
		String name = "new_name";
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		HibSchemaVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		SchemaUpdateRequest request = JsonUtil.readValue(tx(() -> schemaContainer.getLatestVersion().getJson()), SchemaUpdateRequest.class);
		request.setName(name);

		mesh().serverSchemaStorage().clear();

		waitForJobs(() -> {
			// Invoke the update of the schema which will trigger the node migration
			GenericMessageResponse message = call(() -> client().updateSchema(schemaUuid, request));
			assertThat(message).matches("schema_updated_migration_invoked", "content", "2.0");
		}, COMPLETED, 1);

		try (Tx tx = tx()) {
			SchemaDaoWrapper schemaDao = tx.schemaDao();
			assertEquals("The name of the old version should not be updated", "content", currentVersion.getName());
			assertEquals("The name of the schema was not updated", name, currentVersion.getNextVersion().getName());
			HibSchema reloaded = schemaDao.findByUuid(schemaUuid);
			assertEquals("The name should have been updated", name, reloaded.getName());
		}

	}

	@Test
	public void testApplyChanges() throws IOException {

		String json = TestUtils.getJson("schema-changes.json");
		String uuid = tx(() -> schemaContainer("folder").getUuid());
		SchemaChangesListModel changes = JsonUtil.readValue(json, SchemaChangesListModel.class);

		expect(SCHEMA_UPDATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertEquals("folder", event.getName());
			assertEquals(uuid, event.getUuid());
		}).one();
		expect(SCHEMA_MIGRATION_START).none();
		call(() -> client().applyChangesToSchema(uuid, changes));
		awaitEvents();

		SchemaResponse response = call(() -> client().findSchemaByUuid(uuid));
		assertEquals("string", response.getField("pub_dir").getType());
		assertNull("Slug should have been removed.", response.getField("slug"));
		assertEquals("pub_dir", response.getDisplayField());
		assertEquals("name", response.getSegmentField());
		assertEquals(2, response.getFields().size());
	}

	@Test
	public void testUpdateWithConflictingName() {
		try (Tx tx = tx()) {
			String name = "folder";
			String originalSchemaName = "content";
			HibSchema schema = schemaContainer(originalSchemaName);
			SchemaUpdateRequest request = JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaUpdateRequest.class);

			mesh().serverSchemaStorage().clear();

			// Update name to folder to create a conflict
			request.setName(name);

			call(() -> client().updateSchema(schema.getUuid(), request), CONFLICT, "schema_conflicting_name", name);
			assertEquals("The name of the schema was updated", originalSchemaName, schema.getLatestVersion().getName());
		}
	}

	private SchemaUpdateRequest buildListFieldUpdateRequest(SchemaResponse schema) {
		SchemaUpdateRequest req = new SchemaUpdateRequest();
		req.setName(schema.getName());
		req.setFields(schema.getFields());
		return req;
	}

	@Test
	public void testListTypeChange() throws Exception {
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		SchemaResponse schema = call(() -> client().findSchemaByUuid(schemaUuid));

		ListFieldSchema listField = new ListFieldSchemaImpl();
		listField.setListType("string");
		listField.setName("testListType");
		listField.setLabel("testListTypeLabel");
		schema.getFields().add(listField);
		// Update schema and wait for migration
		waitForJobs(() -> call(() -> client().updateSchema(schemaUuid, this.buildListFieldUpdateRequest(schema))), COMPLETED, 1);

		SchemaResponse updated = call(() -> client().findSchemaByUuid(schemaUuid));
		assertNotNull("The new field should have been added to the schema.", updated.getField("testListType"));
		listField = (ListFieldSchema) updated.getField("testListType");
		assertEquals("The list type should be string.", "string", listField.getListType());
		listField.setListType("micronode");
		// Update schema and wait for migration
		waitForJobs(() -> call(() -> client().updateSchema(schemaUuid, this.buildListFieldUpdateRequest(updated))), COMPLETED, 1);

		SchemaResponse changed = call(() -> client().findSchemaByUuid(schemaUuid));
		assertNotNull("The new field should still be in the schema.", changed.getField("testListType"));
		listField = (ListFieldSchema) changed.getField("testListType");
		assertEquals("The list type should have changed to micronode.", "micronode", listField.getListType());
	}

	@Test
	public void testFieldTypeChange() throws Exception {
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		HibSchemaVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		assertNull("The schema should not yet have any changes", tx(() -> currentVersion.getNextChange()));

		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createChangeFieldTypeChange("content", "boolean");
		listOfChanges.getChanges().add(change);

		// Trigger migration
		GenericMessageResponse status = call(() -> client().applyChangesToSchema(schemaUuid, listOfChanges));
		assertThat(status).matches("schema_changes_applied", "content");
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));

		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);

		try (Tx tx = tx()) {
			assertNotNull("The change should have been added to the schema.", currentVersion.getNextChange());
			assertNotNull("The container should now have a new version", currentVersion.getNextVersion());

			// Assert that migration worked
			HibNode node = content();
			assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				!Objects.equals(currentVersion.getVersion(),
					boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getVersion()));
			assertNull("There should no longer be a content field of type html",
				boot().contentDao().getGraphFieldContainer(node, "en").getHtml("content"));
		}
	}

	@Test
	public void testRemoveAddFieldTypeWithSameKey() throws Exception {
		SchemaUpdateRequest request;
		HibNode content = content();
		HibSchema schemaContainer = schemaContainer("content");

		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.contentDao();
			contentDao.getLatestDraftFieldContainer(content, english()).getHtml("content").setHtml("42.1");

			// 1. Create update request by removing the content field from schema and adding a new content with different type
			request = JsonUtil.readValue(schemaContainer.getLatestVersion().getJson(), SchemaUpdateRequest.class);
			request.removeField("content");
			request.addField(FieldUtil.createNumberFieldSchema("content"));
			tx.success();
		}

		mesh().serverSchemaStorage().clear();

		// 4. Update the schema server side -> 2.0
		try (Tx tx = tx()) {
			GenericMessageResponse status = call(() -> client().updateSchema(schemaContainer.getUuid(), request,
				new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)));
			assertThat(status).matches("schema_updated_migration_deferred", request.getName(), "2.0");
			// 5. assign the new schema version to the branch (which will start the migration)
			SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaContainer.getUuid()));

			waitForJobs(() -> {
				call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, project().getLatestBranch().getUuid(),
					new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
			}, COMPLETED, 1);
		}

		// Add the updated schema to the client store
		try (Tx tx = tx()) {
			request.setVersion(request.getVersion() + 1);

			// 6. Read node and check additional field
			NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new VersioningParametersImpl().draft()));
			assertNotNull("The response should contain the content field.", response.getFields().hasField("content"));
			assertEquals("The type of the content field was not changed to a number field.", NumberFieldImpl.class,
				response.getFields().getNumberField("content").getClass());
			assertEquals("2.0", response.getVersion());

			// 7. Update the node and set the new field
			NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
			nodeUpdateRequest.setLanguage("en");
			nodeUpdateRequest.getFields().put("content", new NumberFieldImpl().setNumber(42.01));
			nodeUpdateRequest.setVersion("2.0");
			response = call(() -> client().updateNode(PROJECT_NAME, contentUuid(), nodeUpdateRequest));
			assertNotNull(response);
			assertNotNull(response.getFields().hasField("content"));
			assertEquals(42.01, response.getFields().getNumberField("content").getNumber());
		}

	}

	@Test
	public void testApplyWithEmptyChangesList() {
		try (Tx tx = tx()) {
			HibSchema container = schemaContainer("content");
			SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
			call(() -> client().applyChangesToSchema(container.getUuid(), listOfChanges), BAD_REQUEST, "schema_migration_no_changes_specified");
		}
	}

	@Test
	public void testUnsetSegmentField() throws JsonParseException, JsonMappingException, IOException {
		// 1. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createUpdateSchemaChange();
		change.setProperty(SchemaChangeModel.SEGMENT_FIELD_KEY, null);
		listOfChanges.getChanges().add(change);

		String uuid = db().tx(() -> schemaContainer("content").getUuid());

		HibSchemaVersion currentVersion = db().tx(() -> {
			HibSchema container = schemaContainer("content");
			HibSchemaVersion version = container.getLatestVersion();
			assertNull("The schema should not yet have any changes", version.getNextChange());
			return version;
		});

		// 2. Invoke migration
		call(() -> client().applyChangesToSchema(uuid, listOfChanges));

		try (Tx tx = tx()) {
			// 3. Assert updated schema
			HibSchema container = schemaContainer("content");
			assertNull("The segment field reference should have been set to null", currentVersion.getNextVersion().getSchema().getSegmentField());
		}
	}

	@Test
	public void testRemoveSegmentField() throws Exception {
		HibNode node = content();
		HibSchema container = schemaContainer("content");
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();

		try (Tx tx = tx()) {
			assertNotNull("The node should have a filename string graph field",
				boot().contentDao().getGraphFieldContainer(node, "en").getString("slug"));

			// 1. Create changes
			SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("slug");
			listOfChanges.getChanges().add(change);
			tx.success();
		}

		try (Tx tx = tx()) {
			// 2. Invoke migration
			assertNull("The schema should not yet have any changes", container.getLatestVersion().getNextChange());
			call(() -> client().applyChangesToSchema(container.getUuid(), listOfChanges), BAD_REQUEST, "schema_error_segmentfield_invalid", "slug");

			// 3. Assert migrated node
			NodeGraphFieldContainer fieldContainer = boot().contentDao().getGraphFieldContainer(node, "en");
			assertNull("The node should still have a filename string graph field", fieldContainer.getHtml("slug"));
		}
	}

	@Test
	public void testRemoveField() throws Exception {
		// 1. Verify test data
		HibNode node = content();
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		HibSchemaVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		assertNotNull("The node should have a html graph field", tx(() -> boot().contentDao().getGraphFieldContainer(node, "en").getHtml("content")));

		// 2. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("content");
		listOfChanges.getChanges().add(change);

		// 3. Invoke migration
		try (Tx tx = tx()) {
			assertNull("The schema should not yet have any changes", schemaContainer.getLatestVersion().getNextChange());
		}
		call(() -> client().applyChangesToSchema(schemaUuid, listOfChanges));

		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);

		try (Tx tx = tx()) {
			assertNotNull("The change should have been added to the schema.", currentVersion.getNextChange());

			// 6. Assert migrated node
			NodeGraphFieldContainer fieldContainer = boot().contentDao().getGraphFieldContainer(node, "en");
			assertNull("The node should no longer have a content html graph field", fieldContainer.getHtml("content"));
		}
	}

	@Test
	public void testAddField() throws Exception {
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		HibSchemaVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		assertNull("The schema should not yet have any changes", tx(() -> currentVersion.getNextChange()));

		// 1. Setup changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField", "html", "label1234");
		listOfChanges.getChanges().add(change);

		// 3. Invoke migration
		GenericMessageResponse status = call(() -> client().applyChangesToSchema(schemaUuid, listOfChanges));
		assertThat(status).matches("schema_changes_applied", "content");
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));

		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);

		try (Tx tx = tx()) {
			assertNotNull("The change should have been added to the schema.", currentVersion.getNextChange());
			assertNotEquals("The container should now have a new version", currentVersion.getUuid(), schemaContainer.getLatestVersion().getUuid());

			// Assert that migration worked
			HibNode node = content();
			assertNotNull("The schema of the node should contain the new field schema",
				boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getSchema().getField("newField"));
			assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				!Objects.equals(currentVersion.getVersion(),
					boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getVersion()));
			assertEquals("label1234",
				boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getSchema().getField("newField").getLabel());

		}
	}

	@Test
	public void testUpdateMultipleTimes() throws Exception {
		HibSchemaVersion currentVersion;
		HibSchema container;
		try (Tx tx = tx()) {
			// Assert start condition
			container = schemaContainer("content");
			currentVersion = container.getLatestVersion();
			assertNull("The schema should not yet have any changes", currentVersion.getNextChange());
		}

		String containerUuid = db().tx(() -> schemaContainer("content").getUuid());
		String branchUuid = db().tx(() -> project().getLatestBranch().getUuid());

		for (int i = 0; i < 10; i++) {

			// 1. Setup changes
			SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
			SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField_" + i, "html", null);
			listOfChanges.getChanges().add(change);

			GenericMessageResponse status = call(() -> client().applyChangesToSchema(containerUuid, listOfChanges));
			assertThat(status).matches("schema_changes_applied", "content");
			SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(containerUuid));

			// 2. Invoke migration
			waitForJobs(() -> {
				call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, branchUuid,
					new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
			}, COMPLETED, 1);

			try (Tx tx = tx()) {
				assertNotNull("The change should have been added to the schema.", currentVersion.getNextChange());
				assertNotEquals("The container should now have a new version", currentVersion.getUuid(), container.getLatestVersion().getUuid());

				// Assert that migration worked
				HibNode node = content();
				assertNotNull("The schema of the node should contain the new field schema",
					boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getSchema().getField("newField_" + i));
				assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
					!Objects.equals(currentVersion.getVersion(),
						boot().contentDao().getGraphFieldContainer(node, "en").getSchemaContainerVersion().getVersion()));
			}

		}

		// Validate schema changes and versions
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			SchemaDaoWrapper schemaDao = tx.schemaDao();

			assertEquals("We invoked 10 migration. Thus we expect 11 versions.", 11, Iterators.size(schemaDao.findAllVersions(container).iterator()));
			assertNull("The last version should not have any changes", container.getLatestVersion().getNextChange());
			assertNull("The last version should not have any futher versions", container.getLatestVersion().getNextVersion());

			HibSchemaVersion version = container.getLatestVersion();
			int nVersions = 0;
			while (true) {
				version = version.getPreviousVersion();
				if (version == null) {
					break;
				}
				assertNotNull("The schema version {" + version.getUuid() + "-" + version.getVersion() + "} should have a next change",
					version.getNextChange());
				assertEquals("The version is not referencing the correct parent container.", container.getUuid(),
					version.getSchemaContainer().getUuid());
				nVersions++;
			}

			assertEquals("The latest version should have exactly 10 previous versions.", nVersions, 10);
			assertTrue("The user should still have update permissions on the schema", userDao.hasPermission(user(), container, UPDATE_PERM));
		}

	}

	/**
	 * Update the schema without applying any changes.
	 */
	@Test
	public void testNoChangesUpdate() {
		try (Tx tx = tx()) {
			HibSchema container = schemaContainer("content");
			SchemaUpdateRequest schema = JsonUtil.readValue(container.getLatestVersion().getSchema().toJson(), SchemaUpdateRequest.class);

			// Update the schema server side
			GenericMessageResponse status = call(() -> client().updateSchema(container.getUuid(), schema));
			assertThat(status).matches("schema_update_no_difference_detected");
		}
	}

	@Test
	public void testUpdateAddField() throws Exception {
		SchemaUpdateRequest schema;
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());

		// 1. Setup schema
		try (Tx tx = tx()) {
			HibSchema container = schemaContainer("content");
			schema = JsonUtil.readValue(container.getLatestVersion().getJson(), SchemaUpdateRequest.class);
			assertEquals("The segment field slug should be set", "slug", schema.getSegmentField());
			schema.getFields().add(FieldUtil.createStringFieldSchema("extraname").setLabel("someLabel"));
			mesh().serverSchemaStorage().clear();
			tx.success();
		}

		// 3. Update the schema server side -> 2.0
		GenericMessageResponse status = call(
			() -> client().updateSchema(schemaUuid, schema, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)));
		assertThat(status).matches("schema_updated_migration_deferred", "content", "2.0");

		// 4. Assign the new schema version to the branch
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));

		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);

		SchemaModel reloadedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		assertEquals("The segment field slug should be set", "slug", reloadedSchema.getSegmentField());
		assertEquals("someLabel", reloadedSchema.getField("extraname").getLabel());

		schema.setVersion(schema.getVersion() + 1);

		// Read node and check additional field
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new VersioningParametersImpl().draft()));
		assertNotNull(response);

		// Update the node and set the new field
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("2.0");
		nodeUpdateRequest.getFields().put("extraname", new StringFieldImpl().setString("sometext"));
		response = call(() -> client().updateNode(PROJECT_NAME, contentUuid(), nodeUpdateRequest));
		assertNotNull(response);
		assertNotNull(response.getFields().getStringField("extraname"));
		assertEquals("sometext", response.getFields().getStringField("extraname").getString());

		// Read node and check additional field
		response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new VersioningParametersImpl().draft()));
		assertNotNull(response);
		assertNotNull(response.getFields().hasField("extraname"));

	}

	@Test
	public void testRemoveField2() throws Exception {
		String containerUuid = db().tx(() -> schemaContainer("content").getUuid());
		String branchUuid = db().tx(() -> project().getLatestBranch().getUuid());
		SchemaUpdateRequest schema;
		String nodeUuid;
		try (Tx tx = tx()) {
			HibNode content = content();
			nodeUuid = content.getUuid();
			// 1. Prepare the update request in which we remove the content field
			HibSchema container = schemaContainer("content");
			schema = JsonUtil.readValue(container.getLatestVersion().getJson(), SchemaUpdateRequest.class);
			schema.removeField("content");
			mesh().serverSchemaStorage().clear();
		}

		call(() -> client().updateSchema(containerUuid, schema, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)));

		// 3. Load the new schema and assign it to the branch
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(containerUuid));
		assertEquals("2.0", updatedSchema.getVersion());
		assertNull("The content field should have been removed", updatedSchema.getField("content"));
		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, branchUuid,
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);
		schema.setVersion(schema.getVersion() + 1);

		// 4. Read node and check additional field
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().draft()));
		assertNull(response.getFields().getStringField("content"));

	}

	@Test
	public void testMigrationForBranch() throws Exception {
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		HibNode content = content();
		SchemaUpdateRequest request;

		HibBranch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			request = JsonUtil.readValue(schemaContainer.getLatestVersion().getSchema().toJson(), SchemaUpdateRequest.class);
			request.getFields().add(FieldUtil.createStringFieldSchema("extraname"));
			mesh().serverSchemaStorage().clear();
			tx.success();
		}

		// 2. Update the schema server side
		call(() -> client().updateSchema(schemaUuid, request, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)));

		// 3. assign the new schema version to the initial branch
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		waitForJobs(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		}, COMPLETED, 1);

		// node must be migrated for initial branch
		try (Tx tx = tx()) {
			ContentDaoWrapper contentDao = tx.contentDao();

			assertThat(contentDao.getGraphFieldContainer(content, "en", initialBranchUuid(), ContainerType.DRAFT))
				.isOf(schemaContainer.getLatestVersion());

			// node must not be migrated for new branch
			assertThat(contentDao.getGraphFieldContainer(content, "en", newBranch.getUuid(), ContainerType.DRAFT))
				.isOf(schemaContainer.getLatestVersion().getPreviousVersion());
		}
	}

	@Test
	@Category({ FailingTests.class })
	public void testUpdateFieldName() throws Exception {
		// 1. Verify test data
		HibNode node = content();
		HibSchema schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		String contentFieldValue;
		try (Tx tx = tx()) {
			assertNotNull("The node should have a html graph field", boot().contentDao().getGraphFieldContainer(node, "en").getHtml("content"));
			contentFieldValue = boot().contentDao().getGraphFieldContainer(node, "en").getHtml("content").getHTML();
		}
		assertEquals("1.0", tx(() -> boot().contentDao().getGraphFieldContainer(node, "en").getVersion().toString()));

		// 2. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createRenameFieldChange("content", "newcontent");
		listOfChanges.getChanges().add(change);

		// 3. Invoke migration
		try (Tx tx = tx()) {
			assertNull("The schema should not yet have any changes", schemaContainer.getLatestVersion().getNextChange());
		}
		call(() -> client().applyChangesToSchema(schemaUuid, listOfChanges));

		// 4. Assign new schema version to branch
		SchemaResponse updatedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		waitForJob(() -> {
			call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(),
				new SchemaReferenceImpl().setName("content").setVersion(updatedSchema.getVersion())));
		});

		// Note : testing against content() directly (orientdb model) doesnt work since old field is not deleted
		assertEquals("2.0", tx(() -> boot().contentDao().getGraphFieldContainer(node, "en").getVersion().toString()));
		assertNull("We would expect the new version to not include the old field value.",
			tx(() -> boot().contentDao().getGraphFieldContainer(node, "en").getHtml("content")));

		// Read node and check that content field has been migrated to newcontent
		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid(), new VersioningParametersImpl().draft()));
		assertTrue(response.getFields().hasField("newcontent"));
		assertFalse(response.getFields().hasField("content"));
		assertEquals("The field value was not preserved.", contentFieldValue, response.getFields().getHtmlField("newcontent").getHTML());
	}

}
