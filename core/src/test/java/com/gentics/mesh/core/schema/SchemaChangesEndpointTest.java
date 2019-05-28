package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.MeshEvent.SCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Objects;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.com.google.common.collect.Iterators;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.SchemaUpdateParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.search.AbstractNodeSearchEndpointTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.util.TestUtils;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class SchemaChangesEndpointTest extends AbstractNodeSearchEndpointTest {

	@Before
	public void addAdminPerms() {
		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));
	}

	@Test
	public void testUpdateName() throws GenericRestException, Exception {
		String name = "new_name";
		SchemaContainer schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		SchemaContainerVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		SchemaUpdateRequest request = JsonUtil.readValue(tx(() -> schemaContainer.getLatestVersion().getJson()), SchemaUpdateRequest.class);
		request.setName(name);

		MeshInternal.get().serverSchemaStorage().clear();

		waitForJobs(() -> {
			// Invoke the update of the schema which will trigger the node migration
			GenericMessageResponse message = call(() -> client().updateSchema(schemaUuid, request));
			assertThat(message).matches("schema_updated_migration_invoked", "content", "2.0");
		}, COMPLETED, 1);

		try (Tx tx = tx()) {
			assertEquals("The name of the old version should not be updated", "content", currentVersion.getName());
			assertEquals("The name of the schema was not updated", name, currentVersion.getNextVersion().getName());
			SchemaContainer reloaded = boot().schemaContainerRoot().findByUuid(schemaUuid);
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
	public void testBlockingMigrationStatus() throws InterruptedException, IOException {
		SchemaContainer container = schemaContainer("content");
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();

		try (Tx tx = tx()) {
			assertNull("The schema should not yet have any changes", container.getLatestVersion().getNextChange());

			SchemaChangeModel change = SchemaChangeModel.createChangeFieldTypeChange("content", "boolean");

			// Update a single node field in order to trigger a single blocking
			// migration script
			content().getLatestDraftFieldContainer(english()).getHtml("content").setHtml("triggerWait");

			String blockingScript = IOUtils.toString(getClass().getResourceAsStream("/testscripts/longMigrate.js"));
			change.setMigrationScript(blockingScript);
			listOfChanges.getChanges().add(change);
			tx.success();
		}

		try (Tx tx = tx()) {
			// Assert that all jobs have been completed
			JobListResponse migrationStatus = call(() -> client().findJobs());
			assertThat(migrationStatus).listsAll(COMPLETED);

			GenericMessageResponse status = call(() -> client().applyChangesToSchema(container.getUuid(), listOfChanges));
			assertThat(status).matches("schema_changes_applied", "content");

			SchemaResponse schema = call(() -> client().findSchemaByUuid(container.getUuid()));
			assertEquals("2.0", schema.getVersion());

			// Trigger migration
			waitForJobs(() -> {
				call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, project().getLatestBranch().getUuid(),
					new SchemaReferenceImpl().setName("content").setVersion(schema.getVersion())));
			}, COMPLETED, 1);
		}

	}

	@Test
	public void testUpdateWithConflictingName() {
		try (Tx tx = tx()) {
			String name = "folder";
			String originalSchemaName = "content";
			SchemaContainer schema = schemaContainer(originalSchemaName);
			SchemaUpdateRequest request = JsonUtil.readValue(schema.getLatestVersion().getJson(), SchemaUpdateRequest.class);

			MeshInternal.get().serverSchemaStorage().clear();

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
		SchemaContainer schemaContainer = schemaContainer("content");
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
		SchemaContainer schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		SchemaContainerVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
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
			Node node = content();
			assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				!Objects.equals(currentVersion.getVersion(), node.getGraphFieldContainer("en").getSchemaContainerVersion().getVersion()));
			assertNull("There should no longer be a content field of type html", node.getGraphFieldContainer("en").getHtml("content"));
		}
	}

	@Test
	public void testRemoveAddFieldTypeWithSameKey() throws Exception {
		SchemaUpdateRequest request;
		Node content = content();
		SchemaContainer schemaContainer = schemaContainer("content");

		try (Tx tx = tx()) {
			content.getLatestDraftFieldContainer(english()).getHtml("content").setHtml("42.1");

			// 1. Create update request by removing the content field from schema and adding a new content with different type
			request = JsonUtil.readValue(schemaContainer.getLatestVersion().getJson(), SchemaUpdateRequest.class);
			request.removeField("content");
			request.addField(FieldUtil.createNumberFieldSchema("content"));
			tx.success();
		}

		MeshInternal.get().serverSchemaStorage().clear();

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
			SchemaContainer container = schemaContainer("content");
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

		SchemaContainerVersion currentVersion = db().tx(() -> {
			SchemaContainer container = schemaContainer("content");
			SchemaContainerVersion version = container.getLatestVersion();
			assertNull("The schema should not yet have any changes", version.getNextChange());
			return version;
		});

		// 2. Invoke migration
		call(() -> client().applyChangesToSchema(uuid, listOfChanges));

		try (Tx tx = tx()) {
			// 3. Assert updated schema
			SchemaContainer container = schemaContainer("content");
			assertNull("The segment field reference should have been set to null", currentVersion.getNextVersion().getSchema().getSegmentField());
		}
	}

	@Test
	public void testRemoveSegmentField() throws Exception {
		SchemaContainer container = schemaContainer("content");
		Node node = content();
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();

		try (Tx tx = tx()) {
			assertNotNull("The node should have a filename string graph field", node.getGraphFieldContainer("en").getString("slug"));

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
			NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
			assertNull("The node should still have a filename string graph field", fieldContainer.getHtml("slug"));
		}
	}

	@Test
	public void testRemoveField() throws Exception {
		// 1. Verify test data
		Node node = content();
		SchemaContainer schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		SchemaContainerVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
		assertNotNull("The node should have a html graph field", tx(() -> node.getGraphFieldContainer("en").getHtml("content")));

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
			NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
			assertNull("The node should no longer have a content html graph field", fieldContainer.getHtml("content"));
		}
	}

	@Test
	public void testAddField() throws Exception {
		SchemaContainer schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		SchemaContainerVersion currentVersion = tx(() -> schemaContainer.getLatestVersion());
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
			Node node = content();
			assertNotNull("The schema of the node should contain the new field schema",
				node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema().getField("newField"));
			assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				!Objects.equals(currentVersion.getVersion(), node.getGraphFieldContainer("en").getSchemaContainerVersion().getVersion()));
			assertEquals("label1234", node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema().getField("newField").getLabel());

		}
	}

	@Test
	public void testUpdateMultipleTimes() throws Exception {
		SchemaContainerVersion currentVersion;
		SchemaContainer container;
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
				Node node = content();
				assertNotNull("The schema of the node should contain the new field schema",
					node.getGraphFieldContainer("en").getSchemaContainerVersion().getSchema().getField("newField_" + i));
				assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
					!Objects.equals(currentVersion.getVersion(), node.getGraphFieldContainer("en").getSchemaContainerVersion().getVersion()));
			}

		}

		// Validate schema changes and versions
		try (Tx tx = tx()) {
			assertEquals("We invoked 10 migration. Thus we expect 11 versions.", 11, Iterators.size(container.findAll().iterator()));
			assertNull("The last version should not have any changes", container.getLatestVersion().getNextChange());
			assertNull("The last version should not have any futher versions", container.getLatestVersion().getNextVersion());

			SchemaContainerVersion version = container.getLatestVersion();
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
			assertTrue("The user should still have update permissions on the schema", user().hasPermission(container, UPDATE_PERM));
		}

	}

	/**
	 * Update the schema without applying any changes.
	 */
	@Test
	public void testNoChangesUpdate() {
		try (Tx tx = tx()) {
			SchemaContainer container = schemaContainer("content");
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
			SchemaContainer container = schemaContainer("content");
			schema = JsonUtil.readValue(container.getLatestVersion().getJson(), SchemaUpdateRequest.class);
			assertEquals("The segment field slug should be set", "slug", schema.getSegmentField());
			schema.getFields().add(FieldUtil.createStringFieldSchema("extraname").setLabel("someLabel"));
			MeshInternal.get().serverSchemaStorage().clear();
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

		Schema reloadedSchema = call(() -> client().findSchemaByUuid(schemaUuid));
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
			Node content = content();
			nodeUuid = content.getUuid();
			// 1. Prepare the update request in which we remove the content field
			SchemaContainer container = schemaContainer("content");
			schema = JsonUtil.readValue(container.getLatestVersion().getJson(), SchemaUpdateRequest.class);
			schema.removeField("content");
			MeshInternal.get().serverSchemaStorage().clear();
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
		SchemaContainer schemaContainer = schemaContainer("content");
		String schemaUuid = tx(() -> schemaContainer.getUuid());
		Node content = content();
		SchemaUpdateRequest request;

		Branch newBranch = createBranch("newbranch", true);

		try (Tx tx = tx()) {
			request = JsonUtil.readValue(schemaContainer.getLatestVersion().getSchema().toJson(), SchemaUpdateRequest.class);
			request.getFields().add(FieldUtil.createStringFieldSchema("extraname"));
			MeshInternal.get().serverSchemaStorage().clear();
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
			assertThat(content.getGraphFieldContainer("en", initialBranchUuid(), ContainerType.DRAFT)).isOf(schemaContainer.getLatestVersion());

			// node must not be migrated for new branch
			assertThat(content.getGraphFieldContainer("en", newBranch.getUuid(), ContainerType.DRAFT))
				.isOf(schemaContainer.getLatestVersion().getPreviousVersion());
		}
	}
}
