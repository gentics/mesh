package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.common.ContainerType.DRAFT;
import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.core.rest.job.JobStatus.FAILED;
import static com.gentics.mesh.core.rest.job.JobStatus.QUEUED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.UUID;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.branch.HibBranchSchemaVersion;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ContentDao;
import com.gentics.mesh.core.data.dao.JobDao;
import com.gentics.mesh.core.data.dao.NodeDao;
import com.gentics.mesh.core.data.dao.PersistingBranchDao;
import com.gentics.mesh.core.data.dao.PersistingMicroschemaDao;
import com.gentics.mesh.core.data.dao.PersistingSchemaDao;
import com.gentics.mesh.core.data.dao.SchemaDao;
import com.gentics.mesh.core.data.node.HibMicronode;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.field.list.HibMicronodeFieldList;
import com.gentics.mesh.core.data.node.field.nesting.HibMicronodeField;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.data.schema.HibSchema;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.schema.HibUpdateFieldChange;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.job.JobListResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeOperation;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.util.TestUtils;
import com.gentics.mesh.util.IndexOptionHelper;
import com.gentics.mesh.util.Tuple;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true, clusterMode = false)
public class NodeMigrationEndpointTest extends AbstractMeshTest {

	/**
	 * Create a schema model and assign it to the project/branch. Assert that an index has been created.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testInitialAssignment() throws Exception {
		tx(tx -> {
			tx.jobDao().clear();
		});

		/**
		 * 1. Create the initial schema and assign it to the branch. Make sure that an index has been created. No job should be queued.
		 */
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("dummy");
		request.addField(FieldUtil.createStringFieldSchema("text"));
		request.setSegmentField("text");
		request.setDisplayField("text");
		request.validate();
		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		String versionUuid = tx(tx -> {
			return tx.schemaDao().findByName("dummy").getLatestVersion().getUuid();
		});
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), schemaResponse.toReference()));

		// Assert that the index was created and that no job was scheduled. We need no job since no migration is required
		HibBranchSchemaVersion assignment1;
		try (Tx tx = tx()) {
			assignment1 = tx.branchDao().findBranchSchemaEdge(initialBranch(), tx.schemaDao().findByName("dummy").getLatestVersion());
			assertEquals(COMPLETED, assignment1.getMigrationStatus());
			assertNull(assignment1.getJobUuid());
			assertTrue("The assignment should be active.", assignment1.isActive());
		}
		assertThat(adminCall(() -> client().findJobs())).isEmpty();

		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionUuid,
			DRAFT, null, null));
		assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionUuid,
			PUBLISHED, null, null));

		/**
		 * 2. Stop the job worker and update the schema again. The new version should be assigned to the branch and a migration job should be queued. Make sure
		 * both indices exist.
		 */
		// Stop the job worker so that we can inspect the job in detail
		mesh().jobWorkerVerticle().stop();
		// Update the schema again.
		SchemaUpdateRequest updateRequest = new SchemaUpdateRequest();
		updateRequest.setName("dummy");
		updateRequest.addField(FieldUtil.createStringFieldSchema("text"));
		updateRequest.addField(FieldUtil.createStringFieldSchema("text2"));
		updateRequest.setSegmentField("text");
		updateRequest.setDisplayField("text");
		updateRequest.validate();
		call(() -> client().updateSchema(schemaResponse.getUuid(), updateRequest));

		waitForSearchIdleEvent();

		// Assert that the indices have been created and the job has been queued
		HibBranchSchemaVersion assignment2;
		HibSchemaVersion versionB;
		String versionBUuid;
		try (Tx tx = tx()) {
			SchemaDao schemaDao = tx.schemaDao();
			versionB = schemaDao.findByName("dummy").getLatestVersion();
			versionBUuid = versionB.getUuid();
			assertNotEquals(versionUuid, versionBUuid);
			assignment2 = tx.branchDao().findBranchSchemaEdge(initialBranch(), tx.schemaDao().findByName("dummy").getLatestVersion());
			assertNotNull(assignment2.getJobUuid());
			assertEquals("The migration should be queued", QUEUED, assignment2.getMigrationStatus());
			assertTrue("The assignment should be active.", assignment2.isActive());
			assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionBUuid,
				DRAFT, null, null)).hasNoDropEvents();
			assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionBUuid,
				PUBLISHED, null, null)).hasNoDropEvents();
			assertThat(adminCall(() -> client().findJobs())).hasInfos(1);
		}

		/**
		 * 3. Start the job worker and verify that the job has been completed. Make sure that initial index was removed since the new index now takes its place.
		 */
		// Now start the worker again
		mesh().jobWorkerVerticle().start();
		triggerAndWaitForAllJobs(COMPLETED);

		try (Tx tx = tx()) {
			assignment1 = CommonTx.get().load(assignment1.getId(), assignment1.getClass());
			assignment2 = CommonTx.get().load(assignment2.getId(), assignment2.getClass());
			assertNotNull(assignment2.getJobUuid());
			assertEquals(COMPLETED, assignment2.getMigrationStatus());
			assertTrue("The assignment should be active.", assignment2.isActive());
			assertFalse("The previous assignment should be inactive.", assignment1.isActive());
		}

		// The initial index should have been removed
		waitForSearchIdleEvent();
		assertThat(trackingSearchProvider())
			.hasDrop(ContentDao.composeIndexPattern(projectUuid(), initialBranchUuid(), versionUuid));

		/**
		 * 4. Create a node and update the schema again. This node should be migrated. A deleteDocument call must be recorded for the old index. A store event
		 * must be recorded for the new index.
		 */
		trackingSearchProvider().clear().blockingAwait();
		mesh().jobWorkerVerticle().stop();

		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("dummy");
		nodeCreateRequest.setParentNodeUuid(tx(() -> folder("2015").getUuid()));
		nodeCreateRequest.getFields().put("text", new StringFieldImpl().setString("text_value"));
		nodeCreateRequest.getFields().put("text2", new StringFieldImpl().setString("text2_value"));
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		waitForSearchIdleEvent();

		assertThat(trackingSearchProvider()).hasStore(
			ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionBUuid, DRAFT, null, null),
			response.getUuid() + "-en");

		updateRequest.addField(FieldUtil.createStringFieldSchema("text3"));
		call(() -> client().updateSchema(schemaResponse.getUuid(), updateRequest));

		waitForSearchIdleEvent();

		HibBranchSchemaVersion edge3;
		HibSchemaVersion versionC;
		String versionCUuid;
		try (Tx tx = tx()) {
			PersistingSchemaDao schemaDao = tx.<CommonTx>unwrap().schemaDao();

			versionC = tx(() -> schemaDao.findByName("dummy").getLatestVersion());
			versionCUuid = versionC.getUuid();
			assertTrue("There should be editable containers (one draft) which should be linked to the version.",
				schemaDao.findDraftFieldContainers(versionB,
					initialBranchUuid()).hasNext());
			assertNotEquals("A new latest version should have been created.", versionBUuid, versionCUuid);

			edge3 = tx.branchDao().findBranchSchemaEdge(initialBranch(), schemaDao.findByName("dummy").getLatestVersion());
			assertNotNull(edge3.getJobUuid());
			assertEquals(QUEUED, edge3.getMigrationStatus());
			assertFalse("The previous assignment should be inactive.", assignment1.isActive());
			assertTrue("The previous assignment should be active since it has not yet been migrated.", assignment2.isActive());
			assertTrue("The assignment should be active.", edge3.isActive());
			assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionCUuid,
				DRAFT, null, null)).hasNoDropEvents();
			assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionCUuid,
				PUBLISHED, null, null)).hasNoDropEvents();
			assertThat(adminCall(() -> client().findJobs())).hasInfos(2);
		}

		// Now start the worker again
		mesh().jobWorkerVerticle().start();
		triggerAndWaitForAllJobs(COMPLETED);
		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			edge3 = CommonTx.get().load(edge3.getId(), edge3.getClass());
			PersistingSchemaDao schemaDao = tx.<CommonTx>unwrap().schemaDao();
			assertNotNull(edge3.getJobUuid());
			assertEquals(COMPLETED, edge3.getMigrationStatus());
			assertFalse("The previous assignment should be inactive.", assignment1.isActive());
			assertFalse("The previous assignment should be inactive since it has been been migrated.", assignment1.isActive());
			assertTrue("The assignment should be active.", edge3.isActive());
			assertFalse(
				"There should no longer be an editable container (one draft) linked to the version since the migration should have updated the link.",
				schemaDao.findDraftFieldContainers(versionB, initialBranchUuid()).hasNext());
			assertTrue("There should now be versions linked to the new schema version instead.",
				schemaDao.findDraftFieldContainers(versionC, initialBranchUuid()).hasNext());
		}

		// The old index should have been removed
		assertThat(trackingSearchProvider())
			.hasDrop(ContentDao.composeIndexPattern(projectUuid(), initialBranchUuid(), versionBUuid));

		// The node should have been removed from the old index and placed in the new one
		assertThat(trackingSearchProvider()).hasDelete(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionBUuid,
			DRAFT, null, null), response.getUuid() + "-en");
		assertThat(trackingSearchProvider()).hasStore(
			ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionCUuid, DRAFT, null, null),
			response.getUuid() + "-en");

	}

	@Test
	public void testEmptyMigration() throws Throwable {

		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("dummy");
		request.addField(FieldUtil.createStringFieldSchema("text"));
		request.setSegmentField("text");
		request.setDisplayField("text");
		request.validate();
		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		String versionUuid = tx(tx -> {
			return tx.schemaDao().findByName("dummy").getLatestVersion().getUuid();
		});
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), schemaResponse.toReference()));

		try (Tx tx = tx()) {
			JobDao jobDao = tx.jobDao();
			// No job should be scheduled since this is the first time we assign the container to the project/branch
			assertEquals(0, TestUtils.toList(jobDao.findAll()).size());
		}

		assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionUuid,
			DRAFT, null, null));
		assertThat(trackingSearchProvider()).hasCreate(ContentDao.composeIndexName(projectUuid(), initialBranchUuid(), versionUuid,
			PUBLISHED, null, null));
	}

	@Test
	public void testMigrateByESFieldNull() {
		assertFieldEsSettingUpdateForValue(null);
	}

	@Test
	public void testMigrateByESFieldEmpty() {
		assertFieldEsSettingUpdateForValue(new JsonObject());
	}

	private void assertFieldEsSettingUpdateForValue(JsonObject value) {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		SchemaResponse beforeSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		assertEquals("1.0", beforeSchema.getVersion());

		// Add elasticsearch setting to content field
		SchemaVersionModel schemaModel = tx(() -> {
			JsonObject setting = new JsonObject().put("test", "123");
			HibSchemaVersion version = schemaContainer("content").getLatestVersion();
			SchemaVersionModel schema = version.getSchema();
			schema.getField("slug").setElasticsearch(setting);
			version.setJson(schema.toJson(false));
			return schema;
		});
		SchemaUpdateRequest request = JsonUtil.readValue(schemaModel.toJson(), SchemaUpdateRequest.class);
		waitForJobs(() -> {
			request.getField("slug").setElasticsearch(value);
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		SchemaResponse afterSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		assertEquals("2.0", afterSchema.getVersion());
		assertEquals("The ES setting of the slug field should have been set to empty json.", new JsonObject(),
			afterSchema.getField("slug").getElasticsearch());

	}

	@Test
	public void testMigrateByESSchemaNull() {
		String schemaUuid = tx(() -> schemaContainer("content").getUuid());
		SchemaResponse beforeSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		assertEquals("1.0", beforeSchema.getVersion());

		// Add elasticsearch setting to schema
		SchemaVersionModel schemaModel = tx(() -> {
			JsonObject setting = new JsonObject().put("test", "123");
			HibSchemaVersion version = schemaContainer("content").getLatestVersion();
			SchemaVersionModel schema = version.getSchema();
			schema.setElasticsearch(setting);
			version.setJson(schema.toJson(false));
			return schema;
		});
		SchemaUpdateRequest request = JsonUtil.readValue(schemaModel.toJson(), SchemaUpdateRequest.class);
		waitForJobs(() -> {
			request.setElasticsearch(new JsonObject());
			call(() -> client().updateSchema(schemaUuid, request));
		}, COMPLETED, 1);

		SchemaResponse afterSchema = call(() -> client().findSchemaByUuid(schemaUuid));
		assertEquals("2.0", afterSchema.getVersion());
		assertEquals("The ES setting of the schema should have been set to {}.", new JsonObject(), afterSchema.getElasticsearch());

	}

	@Test
	public void testStringHtmlMigration() {
		final String FIELD_NAME_1 = "testField1";
		final String FIELD_NAME_2 = "testField2";
		final String SCHEMA_NAME = "testSchema";
		final String PARENT_NODE_UUID = tx(() -> project().getBaseNode().getUuid());

		// 1. Create the schema
		SchemaCreateRequest schemaCreate = new SchemaCreateRequest();
		schemaCreate.addField(FieldUtil.createStringFieldSchema(FIELD_NAME_1));
		schemaCreate.addField(FieldUtil.createStringFieldSchema(FIELD_NAME_2));
		schemaCreate.setName(SCHEMA_NAME);
		schemaCreate.setDisplayField(FIELD_NAME_1);

		SchemaResponse schema = call(() -> client().createSchema(schemaCreate));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schema.getUuid()));

		// 2. Create a node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchemaName(SCHEMA_NAME);
		nodeCreateRequest.getFields().put(FIELD_NAME_1, FieldUtil.createStringField("test123"));
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setParentNodeUuid(PARENT_NODE_UUID);
		call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

		// 3. Update the schema
		SchemaUpdateRequest schemaUpdate = new SchemaUpdateRequest();
		schemaUpdate.addField(FieldUtil.createStringFieldSchema(FIELD_NAME_1));
		schemaUpdate.addField(FieldUtil.createHtmlFieldSchema(FIELD_NAME_2));
		schemaUpdate.setName(SCHEMA_NAME);
		schemaUpdate.setDisplayField(FIELD_NAME_1);
		waitForJob(() -> {
			call(() -> client().updateSchema(schema.getUuid(), schemaUpdate));
		});
	}

	@Test
	public void testStartSchemaMigration() throws Throwable {
		HibSchema container;
		HibSchemaVersion versionA;
		HibSchemaVersion versionB;
		HibNode firstNode;
		HibNode secondNode;
		String oldFieldName = "oldname";
		String newFieldName = "changedfield";
		String jobUuid;

		try (Tx tx = tx()) {
			PersistingBranchDao branchDao = tx.<CommonTx>unwrap().branchDao();
			NodeDao nodeDao = tx.nodeDao();
			container = createDummySchemaWithChanges(oldFieldName, newFieldName, false);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			HibUser user = user();
			EventQueueBatch batch = createBatch();
			assertNull("No job should be scheduled since this is the first time we assigned the schema to the branch. No need for a migration",
				branchDao.assignSchemaVersion(project().getLatestBranch(), user, versionA, batch));
			batch.dispatch();

			// create a node based on the old schema
			String english = english();
			HibNode parentNode = folder("2015");
			firstNode = nodeDao.create(parentNode, user, versionA, project());
			HibNodeFieldContainer firstEnglishContainer = tx.contentDao().createFieldContainer(firstNode, english,
				firstNode.getProject().getLatestBranch(), user);
			firstEnglishContainer.createString(oldFieldName).setString("first content");

			secondNode = nodeDao.create(parentNode, user, versionA, project());
			HibNodeFieldContainer secondEnglishContainer = tx.contentDao().createFieldContainer(secondNode, english,
				secondNode.getProject().getLatestBranch(), user);
			secondEnglishContainer.createString(oldFieldName).setString("second content");

			HibBranch latestBranch = reloadBranch(project().getLatestBranch());
			jobUuid = branchDao.assignSchemaVersion(latestBranch, user, versionB, batch).getUuid();
			tx.success();
		}

		triggerAndWaitForJob(jobUuid);
		waitForSearchIdleEvent();
		try (Tx tx = tx()) {
			// assert that migration worked
			assertThat(firstNode).as("Migrated Node").isOf(container).hasTranslation("en");
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en")).as("Migrated field container").isOf(versionB).hasVersion("0.2");
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getString(newFieldName).getString()).as("Migrated field value")
				.isEqualTo(
					"first content");
			assertThat(secondNode).as("Migrated Node").isOf(container).hasTranslation("en");
			assertThat(tx.contentDao().getFieldContainer(secondNode, "en")).as("Migrated field container").isOf(versionB).hasVersion("0.2");
			assertThat(tx.contentDao().getFieldContainer(secondNode, "en").getString(newFieldName).getString()).as("Migrated field value")
				.isEqualTo(
					"second content");

			// Two containers are moved from on index to another -> 2 Store / 2 Delete
			// The old indices are dropped -> 2 Deleted
			// The new indices are created -> 2 Creates
			// The mappings of the new indices are created -> 2 Mappings
			int store = 2;
			int update = 0;
			int delete = 2;
			int indexDrop = 1;
			int indexCreate = 2;
			assertThat(trackingSearchProvider()).hasEvents(store, update, delete, indexDrop, indexCreate);
		}

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1).containsJobs(jobUuid);
	}

	@Test
	public void testMigrateAddRawField() throws Throwable {

		HibNode node = content();
		String nodeUuid = contentUuid();
		// Add some really long string value to the content
		try (Tx tx = tx()) {
			HibNode parentNode = content();
			ContentDao contentDao = tx.contentDao();

			HibNodeFieldContainer original = contentDao.getLatestDraftFieldContainer(parentNode, english());
			HibNodeFieldContainer newContainer = contentDao.createFieldContainer(parentNode, english(), project().getLatestBranch(), user(), original, true);
			newContainer.getString("title").setString(TestUtils.getRandomHash(40_000));
			newContainer.getString("teaser").setString(TestUtils.getRandomHash(40_000));
			tx.success();
		}

		String schemaUuid = tx(() -> node.getSchemaContainer().getUuid());

		int nFieldContainers = tx(tx -> {
			ContentDao contentDao = tx.contentDao();
			HibSchemaVersion schemaVersion = node.getSchemaContainer().getLatestVersion();
			return Long.valueOf(tx.<CommonTx>unwrap().schemaDao().getFieldContainers(schemaVersion, initialBranchUuid())
				.filter(contentDao::isPublished)
				.count()).intValue();
		});

		// Update the schema and enable the addRaw field
		waitForJob(() -> {
			SchemaUpdateRequest request = tx(() -> JsonUtil.readValue(node.getSchemaContainer().getLatestVersion().getJson(),
				SchemaUpdateRequest.class));
			request.getField("teaser").setElasticsearch(IndexOptionHelper.getRawFieldOption());
			call(() -> client().updateSchema(schemaUuid, request));
		});

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1);
		waitForSearchIdleEvent();

		int store = nFieldContainers + nFieldContainers + 1;
		int update = 0;
		int delete = nFieldContainers + nFieldContainers;
		int dropIndex = 1;
		int createIndex = 2;
		assertThat(trackingSearchProvider()).hasEvents(store, update, delete, dropIndex, createIndex);
		for (JsonObject mapping : trackingSearchProvider().getCreateIndexEvents().values()) {
			String basePath = "$.mapping.default";
			if (complianceMode() == ComplianceMode.ES_7) {
				basePath = "$.mapping";
			}
			assertThat(mapping).has(basePath + ".properties.fields.properties.teaser.fields.raw.type", "keyword",
				"The mapping should include a raw field for the teaser field");
			assertThat(mapping).hasNot(basePath + ".properties.fields.properties.title.fields.raw",
				"The mapping should not include a raw field for the title field");
		}

		JsonObject doc = trackingSearchProvider().getStoreEvents().entrySet().stream()
				.filter(e -> e.getKey().endsWith(nodeUuid + "-en"))
				.map(e -> e.getValue())
				.sorted(Comparator.comparing((JsonObject json) -> json.getJsonObject("fields").getString("teaser").length()).reversed())
				.findFirst()
				.orElseThrow();

		// Assert that the teaser has been truncated
		String teaser = doc.getJsonObject("fields").getString("teaser");
		assertThat(teaser).hasSize(32_700);
		String content = doc.getJsonObject("fields").getString("title");
		assertThat(content).hasSize(40_000);
	}

	@Test
	public void testMigrateAgain() throws Throwable {
		String oldFieldName = "oldname";
		String newFieldName = "changedfield";
		HibSchema container;
		HibSchemaVersion versionA;
		HibSchemaVersion versionB;
		HibNode firstNode;
		String jobAUuid;
		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			container = createDummySchemaWithChanges(oldFieldName, newFieldName, false);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			HibUser user = user();
			EventQueueBatch batch = createBatch();
			assertNull("No job should be scheduled since this is the first time we assigned the schema to the branch. No need for a migration",
				tx.branchDao().assignSchemaVersion(project().getLatestBranch(), user, versionA, batch));

			// create a node based on the old schema
			String english = english();
			HibNode parentNode = folder("2015");
			firstNode = nodeDao.create(parentNode, user, versionA, project());
			Tx.get().commit();
			HibNodeFieldContainer firstEnglishContainer = tx.contentDao().createFieldContainer(firstNode, english,
				firstNode.getProject().getLatestBranch(), user);
			firstEnglishContainer.createString(oldFieldName).setString("first content");

			// do the schema migration twice
			HibBranch latestBranch = reloadBranch(project().getLatestBranch());
			jobAUuid = tx.branchDao().assignSchemaVersion(latestBranch, user, versionB, batch).getUuid();
			tx.success();
		}
		Thread.sleep(1000);

		triggerAndWaitForJob(jobAUuid);
		doSchemaMigration(versionA, versionB);
		try (Tx tx = tx()) {
			// assert that migration worked, but was only performed once
			assertThat(firstNode).as("Migrated Node").isOf(container).hasTranslation("en");
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en")).as("Migrated field container").isOf(versionB).hasVersion("0.2");
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getString(newFieldName).getString()).as("Migrated field value")
				.isEqualTo(
					"first content");
		}

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(2);
		assertThat(status).containsJobs(jobAUuid);
	}

	@Test
	public void testMigratePublished() throws Throwable {
		String oldFieldName = "oldname";
		String fieldName = "changedfield";
		HibSchema container;
		HibSchemaVersion versionA;
		HibSchemaVersion versionB;
		HibNode node;

		try (Tx tx = tx()) {
			NodeDao nodeDao = tx.nodeDao();
			BranchDao branchDao = tx.branchDao();
			container = createDummySchemaWithChanges(oldFieldName, fieldName, false);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			EventQueueBatch batch = createBatch();
			branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionA, batch);
			Tx.get().commit();

			// create a node and publish
			node = nodeDao.create(folder("2015"), user(), versionA, project());
			HibNodeFieldContainer englishContainer = tx.contentDao().createFieldContainer(node, english(), project().getLatestBranch(),
				user());
			englishContainer.createString(oldFieldName).setString("content");
			englishContainer.createString("name").setString("someName");
			InternalActionContext ac = new InternalRoutingActionContextImpl(mockRoutingContext());
			nodeDao.publish(node, ac, createBulkContext(), "en");

			branchDao.assignSchemaVersion(project().getLatestBranch(), user(), versionB, batch);
			tx.success();
		}

		doSchemaMigration(versionA, versionB);

		try (Tx tx = tx()) {
			ContentDao contentDao = tx.contentDao();
			assertThat(tx.contentDao().getFieldContainer(node, "en")).as("Migrated draft").isOf(versionB).hasVersion("2.0");
			assertThat(contentDao.getFieldContainer(node, "en", project().getLatestBranch().getUuid(), ContainerType.PUBLISHED))
				.as("Migrated published")
				.isOf(versionB).hasVersion("2.0");
		}

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED);
	}

	@Test
	public void testMicronodeListMigration() throws Exception {

		// 1. Prepare the schema and add micronode list to the content schema
		SchemaUpdateRequest schemaUpdate = tx(() -> JsonUtil.readValue(schemaContainer("content").getLatestVersion().getJson(),
			SchemaUpdateRequest.class));
		schemaUpdate.addField(FieldUtil.createListFieldSchema("micronode", "micronode").setAllowedSchemas("vcard"));

		String schemaUuid = db().tx(() -> schemaContainer("content").getUuid());

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		}, COMPLETED, 1);

		String parentNodeUuid = tx(() -> folder("2015").getUuid());
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("content"));
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("test"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("test"));

		MicronodeField micronodeA = FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField(
			"test-updated-firstname")), Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname")));

		MicronodeField micronodeB = FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField("test")), Tuple
			.tuple("lastName", FieldUtil.createStringField("test")));

		nodeCreateRequest.getFields().put("micronode", FieldUtil.createMicronodeListField(micronodeA, micronodeB));
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		nodeCreateRequest.setLanguage("en");

		// 1. Create a node which contains a micronode list and at least two micronodes
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String nodeUuid = response.getUuid();
		assertThat(response.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

		// 2. Update the name and allow of the micronode list of the used schema
		schemaUpdate.setName("someOtherName");
		ListFieldSchema micronodeListFieldSchema = schemaUpdate.getField("micronode", ListFieldSchema.class);
		micronodeListFieldSchema.setAllowedSchemas("vcard", "captionedImage");

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		}, COMPLETED, 1);

		// 3. Assert that the node still contains the micronode list contents
		NodeResponse migratedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion(
			"published")));
		assertThat(migratedNode.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		assertNotEquals("The node should have been migrated due to the schema update.", migratedNode.getVersion(), response.getVersion());

		// 4. Update the allow of the micronode list of the used schema
		ListFieldSchema micronodeListFieldSchema2 = schemaUpdate.getField("micronode", ListFieldSchema.class);
		micronodeListFieldSchema2.setAllowedSchemas("vcard");
		schemaUpdate.addField(FieldUtil.createMicronodeFieldSchema("otherMicronode").setAllowedMicroSchemas("vcard"));
		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		}, COMPLETED, 1);

		// 5. Assert that the node still contains the micronode list contents
		migratedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion("published")));
		assertThat(migratedNode.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		assertNotEquals("The node should have been migrated due to the schema update.", migratedNode.getVersion(), response.getVersion());

		// 6. Now update the name of the microschema
		String microschemaUuid = tx(() -> microschemaContainer("vcard").getUuid());
		MicroschemaUpdateRequest microschemaUpdate = db().tx(() -> JsonUtil.readValue(microschemaContainer("vcard").getLatestVersion().getJson(),
			MicroschemaUpdateRequest.class));
		microschemaUpdate.setName("someOtherName2");
		microschemaUpdate.addField(FieldUtil.createStringFieldSchema("enemenemuh"));
		waitForJobs(() -> {
			call(() -> client().updateMicroschema(microschemaUuid, microschemaUpdate));
		}, COMPLETED, 1);

		// 7. Verify that the node has been migrated again
		NodeResponse migratedNode2 = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion(
			"published")));
		assertThat(migratedNode2.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		assertNotEquals("The node should have been migrated due to the schema update.", migratedNode.getVersion(), migratedNode2.getVersion());

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(4);

	}

	/**
	 * Asserts that the error is handled in the setup of the migration.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigrationFailureInSetup() throws Exception {

		String jobUuid = tx(tx -> {
			JobDao jobDao = tx.jobDao();
			return jobDao.enqueueMicroschemaMigration(user(), initialBranch(), microschemaContainer("vcard").getLatestVersion(),
				microschemaContainer("vcard").getLatestVersion()).getUuid();
		});

		client().deleteProject(projectUuid()).blockingAwait();
		triggerAndWaitForJob(jobUuid, FAILED);

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(FAILED).hasInfos(1);
		assertNotNull("An error should be stored along with the info.", status.getData().get(0).getErrorDetail());
	}

	/**
	 * Assert that the migration info will be limited to a certain amount of information.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testMigrationInfoCleanup() throws Exception {
		// Run 10 migrations which should all fail
		for (int i = 0; i < 10; i++) {
			tx(tx -> {
				JobDao jobDao = tx.jobDao();
				HibSchemaVersion version = schemaContainer("content").getLatestVersion();
				return jobDao.enqueueSchemaMigration(user(), initialBranch(), version, version);
			});
		}
		triggerAndWaitForAllJobs(COMPLETED);

		// Verify that the expected amount of jobs is listed
		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(10);

	}

	@Test
	public void testMigrateDraftAndPublished() throws Throwable {
		disableAutoPurge();

		// Create schema
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("dummy");
		request.addField(FieldUtil.createStringFieldSchema("text"));
		request.setSegmentField("text");
		request.setDisplayField("text");
		request.validate();
		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), schemaResponse.toReference()));

		// Create node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("dummy");
		nodeCreateRequest.setParentNodeUuid(tx(() -> folder("2015").getUuid()));
		nodeCreateRequest.getFields().put("text", new StringFieldImpl().setString("text_value"));
		NodeResponse draftResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		assertThat(draftResponse).hasVersion("0.1");

		// Publish node
		call(() -> client().publishNode(PROJECT_NAME, draftResponse.getUuid()));

		// Update it again so that the draft version is different from the pub version
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setVersion("0.1");
		nodeUpdateRequest.getFields().put("text", new StringFieldImpl().setString("text2_value"));
		assertThat(call(() -> client().updateNode(PROJECT_NAME, draftResponse.getUuid(), nodeUpdateRequest))).hasVersion("1.1");

		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid(), new VersioningParametersImpl().published()))).hasVersion(
			"1.0");

		// Update the schema again.
		SchemaUpdateRequest updateRequest = new SchemaUpdateRequest();
		updateRequest.setName("dummy");
		updateRequest.addField(FieldUtil.createStringFieldSchema("text"));
		updateRequest.addField(FieldUtil.createStringFieldSchema("text2"));
		updateRequest.setSegmentField("text");
		updateRequest.setDisplayField("text");
		updateRequest.validate();

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaResponse.getUuid(), updateRequest));
		}, COMPLETED, 1);
		// Now load the node again and verify that the draft has been migrated from 1.1 to 2.1 (2.1 since 2.0 is the new published version)
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid()))).hasVersion("2.1").hasStringField("text",
			"text2_value");

		triggerAndWaitForAllJobs(COMPLETED);
		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1);

		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid()))).hasStringField("text", "text2_value").hasVersion("2.1")
			.hasSchemaVersion("dummy", "2.0");
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid(), new VersioningParametersImpl().published())))
			.hasStringField("text", "text_value").hasSchemaVersion("dummy", "2.0").hasVersion("2.0");

		// printVersionInfo(draftResponse.getUuid());
	}

//	private void printVersionInfo(String uuid) {
//		try (Tx tx = tx()) {
//			ContentDao contentDao = tx.contentDao();
//			System.out.println();
//			HibNode node = tx.nodeDao().findByUuid(project(), uuid);
//			for (GraphFieldContainerEdgeImpl e : toGraph(node).outE("HAS_FIELD_CONTAINER").frameExplicit(GraphFieldContainerEdgeImpl.class)) {
//				NodeGraphFieldContainer container = e.getNodeContainer();
//				System.out.println("Type: " + e.getType() + " " + container.getUuid() + " version: " + container.getVersion());
//
//				HibNodeFieldContainer prev = container.getPreviousVersion();
//				while (prev != null) {
//					System.out.println("Prev: " + prev.getUuid() + " version:" + prev.getVersion());
//					prev = prev.getPreviousVersion();
//				}
//
//				for (HibNodeFieldContainer next : contentDao.getNextVersions(container)) {
//					System.out.println("Next: " + next.getUuid() + " version:" + next.getVersion());
//				}
//				System.out.println("--");
//			}
//
//			System.out.println();
//		}
//	}

	/**
	 * Run a migration in which the same container to be migrated is used for draft and published version.
	 * 
	 * @throws Throwable
	 */
	@Test
	public void testMigrateDraftAndSamePublished() throws Throwable {

		// Create schema
		SchemaCreateRequest request = new SchemaCreateRequest();
		request.setName("dummy");
		request.addField(FieldUtil.createStringFieldSchema("text"));
		request.setSegmentField("text");
		request.setDisplayField("text");
		request.validate();
		SchemaResponse schemaResponse = call(() -> client().createSchema(request));
		call(() -> client().assignSchemaToProject(PROJECT_NAME, schemaResponse.getUuid()));
		call(() -> client().assignBranchSchemaVersions(PROJECT_NAME, initialBranchUuid(), schemaResponse.toReference()));

		// Create node
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");
		nodeCreateRequest.setSchemaName("dummy");
		nodeCreateRequest.setParentNodeUuid(tx(() -> folder("2015").getUuid()));
		nodeCreateRequest.getFields().put("text", new StringFieldImpl().setString("text_value"));
		NodeResponse draftResponse = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		assertThat(draftResponse).hasVersion("0.1");

		// Publish node
		call(() -> client().publishNode(PROJECT_NAME, draftResponse.getUuid()));
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid(), new VersioningParametersImpl().published()))).hasVersion(
			"1.0");

		// Update the schema again.
		SchemaUpdateRequest updateRequest = new SchemaUpdateRequest();
		updateRequest.setName("dummy");
		updateRequest.addField(FieldUtil.createStringFieldSchema("text"));
		updateRequest.addField(FieldUtil.createStringFieldSchema("text2"));
		updateRequest.setSegmentField("text");
		updateRequest.setDisplayField("text");
		updateRequest.validate();

		waitForJobs(() -> {
			call(() -> client().updateSchema(schemaResponse.getUuid(), updateRequest));
		}, COMPLETED, 1);

		// Assert that the draft version stays in sync with the publish version. Both must have version 1.0 since they are using the same NGFC.
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid()))).hasVersion("2.0").hasStringField("text", "text_value")
			.hasSchemaVersion("dummy", "2.0");

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1);

		// Assert that the draft and publish version both have version 2.0 since they share the same NGFC.
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid()))).hasStringField("text", "text_value").hasVersion("2.0")
			.hasSchemaVersion("dummy", "2.0");
		assertThat(call(() -> client().findNodeByUuid(PROJECT_NAME, draftResponse.getUuid(), new VersioningParametersImpl().published())))
			.hasStringField("text", "text_value").hasSchemaVersion("dummy", "2.0").hasVersion("2.0");

	}

	@Test
	public void testStartMicroschemaMigration() throws Throwable {
		disableAutoPurge();

		String oldFieldName = "field";
		String newFieldName = "changedfield";
		String micronodeFieldName = "micronodefield";
		HibMicroschema container;
		HibMicroschemaVersion versionA;
		HibMicroschemaVersion versionB;
		HibMicronodeField firstMicronodeField;
		HibMicronodeField secondMicronodeField;
		HibNode firstNode;
		HibNode secondNode;

		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();

			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			// create version 1 of the microschema
			container = microschemaDao.createPersisted(UUIDUtil.randomUUID(), m -> {
				m.setCreated(user());
				m.setName(UUID.randomUUID().toString());
			});
			container.generateBucketId();
			versionA = createMicroschemaVersion(tx, container, v-> {
				container.setLatestVersion(v);					
				MicroschemaModelImpl microschemaA = new MicroschemaModelImpl();
				microschemaA.setName("migratedSchema");
				microschemaA.setVersion("1.0");
				FieldSchema oldField = FieldUtil.createStringFieldSchema(oldFieldName);
				microschemaA.addField(oldField);
				v.setName("migratedSchema");
				v.setSchema(microschemaA);
			});

			// create version 2 of the microschema (with the field renamed)
			versionB = createMicroschemaVersion(tx, container, v -> {
				MicroschemaModelImpl microschemaB = new MicroschemaModelImpl();
				microschemaB.setName("migratedSchema");
				microschemaB.setVersion("2.0");
				FieldSchema newField = FieldUtil.createStringFieldSchema(newFieldName);
				microschemaB.addField(newField);
				v.setName("migratedSchema");
				v.setSchema(microschemaB);
			});
			microschemaDao.mergeIntoPersisted(container);

			// link the schemas with the changes in between
			HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
			updateFieldChange.setFieldName(oldFieldName);
			updateFieldChange.setRestProperty(SchemaChangeModel.NAME_KEY, newFieldName);
			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);
			versionA.setNextChange(updateFieldChange);

			// create micronode based on the old schema
			String english = english();
			firstNode = tx.nodeDao().findByUuidGlobal(folder("2015").getUuid());
			SchemaVersionModel schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
			schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(versionA.getName());
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			actions().updateSchemaVersion(firstNode.getSchemaContainer().getLatestVersion());
			Tx.get().commit();
			firstMicronodeField = tx.contentDao().createFieldContainer(firstNode, english, firstNode.getProject().getLatestBranch(), user())
				.createMicronode(
					micronodeFieldName, versionA);
			firstMicronodeField.getMicronode().createString(oldFieldName).setString("first content");

			secondNode = folder("news");
			secondMicronodeField = tx.contentDao()
				.createFieldContainer(secondNode, english, secondNode.getProject().getLatestBranch(), user()).createMicronode(
					micronodeFieldName, versionA);
			secondMicronodeField.getMicronode().createString(oldFieldName).setString("second content");
			tx.success();
		}

		String jobUuid = tx(tx -> { return tx.jobDao().enqueueMicroschemaMigration(user(), initialBranch(), versionA, versionB).getUuid(); });

		triggerAndWaitForJob(jobUuid);
		try (Tx tx = tx()) {

			// assert that migration worked and created a new version
			assertThat(firstMicronodeField.getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(firstMicronodeField.getMicronode().getString(oldFieldName).getString()).as("Old field value").isEqualTo("first content");

			assertThat(tx.contentDao().getFieldContainer(firstNode, "en")).as("Migrated field container").hasVersion("1.2");
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getMicronode(micronodeFieldName).getMicronode())
				.as("Migrated Micronode").isOf(
					versionB);
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getMicronode(micronodeFieldName).getMicronode()
				.getString(newFieldName).getString()).as(
					"Migrated field value").isEqualTo("first content");

			assertThat(secondMicronodeField.getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(secondMicronodeField.getMicronode().getString(oldFieldName).getString()).as("Old field value").isEqualTo("second content");

			assertThat(tx.contentDao().getFieldContainer(secondNode, "en")).as("Migrated field container").hasVersion("1.2");
			assertThat(tx.contentDao().getFieldContainer(secondNode, "en").getMicronode(micronodeFieldName).getMicronode())
				.as("Migrated Micronode").isOf(
					versionB);
			assertThat(tx.contentDao().getFieldContainer(secondNode, "en").getMicronode(micronodeFieldName).getMicronode()
				.getString(newFieldName).getString())
					.as(
						"Migrated field value")
					.isEqualTo("second content");
		}

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1);

	}

	@Test
	public void testMicroschemaMigrationInListField() throws Throwable {
		String oldFieldName = "field";
		String newFieldName = "changedfield";
		String micronodeFieldName = "micronodefield";
		HibMicroschema container;
		HibMicroschemaVersion versionA;
		HibMicroschemaVersion versionB;
		HibMicronodeFieldList firstMicronodeListField;
		HibMicronodeFieldList secondMicronodeListField;
		HibNode firstNode;
		HibNode secondNode;

		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();
			ContentDao contentDao = tx.contentDao();
			// create version 1 of the microschema
			container = microschemaDao.createPersisted(UUIDUtil.randomUUID(), m -> {
				m.setCreated(user());
				m.setName(UUID.randomUUID().toString());
			});
			container.generateBucketId();
			versionA = createMicroschemaVersion(tx, container, v -> {
				container.setLatestVersion(v);
				MicroschemaModelImpl microschemaA = new MicroschemaModelImpl();
				microschemaA.setName("migratedSchema");
				microschemaA.setVersion("1.0");
				FieldSchema oldField = FieldUtil.createStringFieldSchema(oldFieldName);
				microschemaA.addField(oldField);
				v.setName("migratedSchema");
				v.setSchema(microschemaA);
			});

			// create version 2 of the microschema (with the field renamed)
			versionB = createMicroschemaVersion(tx, container, v -> {
				MicroschemaModelImpl microschemaB = new MicroschemaModelImpl();
				microschemaB.setName("migratedSchema");
				microschemaB.setVersion("2.0");
				FieldSchema newField = FieldUtil.createStringFieldSchema(newFieldName);
				microschemaB.addField(newField);
				v.setName("migratedSchema");
				v.setSchema(microschemaB);
			});
			microschemaDao.mergeIntoPersisted(container);
			Tx.get().commit();
			actions().updateSchemaVersion(versionA);
			actions().updateSchemaVersion(versionB);
			// link the schemas with the changes in between
			HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
			updateFieldChange.setFieldName(oldFieldName);
			updateFieldChange.setRestProperty(SchemaChangeModel.NAME_KEY, newFieldName);
			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);
			versionA.setNextChange(updateFieldChange);

			// create micronode based on the old schema
			String en = english();
			firstNode = folder("2015");
			SchemaVersionModel schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(versionA.getName()).setName(micronodeFieldName)
				.setLabel("Micronode List Field"));
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);
			actions().updateSchemaVersion(firstNode.getSchemaContainer().getLatestVersion());

			// Create the new container version with the specified content which will be migrated
			HibBranch branch = firstNode.getProject().getLatestBranch();
			HibNodeFieldContainer oldContainer = contentDao.getFieldContainer(firstNode, en, branch, DRAFT);
			HibNodeFieldContainer newContainer = contentDao.createFieldContainer(firstNode, en, branch, user(),
				oldContainer,
				true);
			firstMicronodeListField = newContainer.createMicronodeList(micronodeFieldName);
			HibMicronode micronode = firstMicronodeListField.createMicronode(versionA);
			micronode.createString(oldFieldName).setString("first content");

			secondNode = folder("news");
			HibBranch branch2 = secondNode.getProject().getLatestBranch();
			HibNodeFieldContainer oldContainer2 = contentDao.getFieldContainer(secondNode, en, branch2, DRAFT);
			secondMicronodeListField = contentDao.createFieldContainer(secondNode, en, branch2, user(),
				oldContainer2,
				true)
				.createMicronodeList(micronodeFieldName);

			micronode = secondMicronodeListField.createMicronode(versionA);
			micronode.createString(oldFieldName).setString("second content");
			micronode = secondMicronodeListField.createMicronode(versionA);
			micronode.createString(oldFieldName).setString("third content");
			tx.success();
		}

		String jobUuid = tx(tx -> {
			return tx.jobDao().enqueueMicroschemaMigration(user(), project().getLatestBranch(), versionA, versionB).getUuid();
		});
		triggerAndWaitForJob(jobUuid);

		try (Tx tx = tx()) {
			// assert that migration worked and created a new version
			assertThat(firstMicronodeListField.getList().get(0).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(firstMicronodeListField.getList().get(0).getMicronode().getString(oldFieldName).getString()).as("Old field value").isEqualTo(
				"first content");

			assertThat(tx.contentDao().getFieldContainer(firstNode, "en")).as("Migrated field container").hasVersion("2.1");
			assertThat(
				tx.contentDao().getFieldContainer(firstNode, "en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode()).as(
					"Migrated Micronode").isOf(versionB);
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getMicronodeList(micronodeFieldName).getList().get(0)
				.getMicronode().getString(
					newFieldName)
				.getString()).as("Migrated field value").isEqualTo("first content");

			assertThat(secondMicronodeListField.getList().get(0).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(secondMicronodeListField.getList().get(0).getMicronode().getString(oldFieldName).getString()).as("Old field value").isEqualTo(
				"second content");
			assertThat(secondMicronodeListField.getList().get(1).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(secondMicronodeListField.getList().get(1).getMicronode().getString(oldFieldName).getString()).as("Old field value").isEqualTo(
				"third content");

			assertThat(tx.contentDao().getFieldContainer(secondNode, "en")).as("Migrated field container").hasVersion("2.1");
			assertThat(
				tx.contentDao().getFieldContainer(secondNode, "en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode()).as(
					"Migrated Micronode").isOf(versionB);
			assertThat(tx.contentDao().getFieldContainer(secondNode, "en").getMicronodeList(micronodeFieldName).getList().get(0)
				.getMicronode().getString(
					newFieldName)
				.getString()).as("Migrated field value").isEqualTo("second content");
			assertThat(
				tx.contentDao().getFieldContainer(secondNode, "en").getMicronodeList(micronodeFieldName).getList().get(1).getMicronode()).as(
					"Migrated Micronode").isOf(versionB);
			assertThat(tx.contentDao().getFieldContainer(secondNode, "en").getMicronodeList(micronodeFieldName).getList().get(1)
				.getMicronode().getString(
					newFieldName)
				.getString()).as("Migrated field value").isEqualTo("third content");
		}

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1);

	}

	@Test
	public void testMicroschemaMigrationMixedList() throws Throwable {
		disableAutoPurge();

		String oldFieldName = "field";
		String newFieldName = "changedfield";
		String micronodeFieldName = "micronodefield";
		HibMicroschema container;
		HibMicroschemaVersion versionA;
		HibMicroschemaVersion versionB;
		HibMicronodeFieldList firstMicronodeListField;
		HibNode firstNode;

		try (Tx tx = tx()) {
			PersistingMicroschemaDao microschemaDao = (PersistingMicroschemaDao) tx.microschemaDao();
			ContentDao contentDao = tx.contentDao();
			// create version 1 of the microschema
			container = microschemaDao.createPersisted(UUIDUtil.randomUUID(), m -> {
				m.setCreated(user());
				m.setName(UUID.randomUUID().toString());
			});
			container.generateBucketId();
			versionA = createMicroschemaVersion(tx, container, v -> {
				container.setLatestVersion(v);
				MicroschemaModelImpl microschemaA = new MicroschemaModelImpl();
				microschemaA.setName("migratedSchema");
				microschemaA.setVersion("1.0");
				FieldSchema oldField = FieldUtil.createStringFieldSchema(oldFieldName);
				microschemaA.addField(oldField);
				v.setName("migratedSchema");
				v.setSchema(microschemaA);
			});

			// create version 2 of the microschema (with the field renamed)
			versionB = createMicroschemaVersion(tx, container, v -> {
				MicroschemaModelImpl microschemaB = new MicroschemaModelImpl();
				microschemaB.setName("migratedSchema");
				microschemaB.setVersion("2.0");
				FieldSchema newField = FieldUtil.createStringFieldSchema(newFieldName);
				microschemaB.addField(newField);
				v.setName("migratedSchema");
				v.setSchema(microschemaB);
			});
			microschemaDao.mergeIntoPersisted(container);

			// link the schemas with the changes in between
			HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) microschemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
			updateFieldChange.setFieldName(oldFieldName);
			updateFieldChange.setRestProperty(SchemaChangeModel.NAME_KEY, newFieldName);
			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);
			versionA.setNextChange(updateFieldChange);

			// create micronode based on the old schema
			String english = english();
			firstNode = folder("2015");
			SchemaVersionModel schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(versionA.getName(), "vcard").setName(
				micronodeFieldName).setLabel("Micronode List Field"));
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);
			actions().updateSchemaVersion(firstNode.getSchemaContainer().getLatestVersion());
			Tx.get().commit();

			// 1.0
			HibNodeFieldContainer org = contentDao.getFieldContainer(firstNode, english, firstNode.getProject().getLatestBranch(),
				ContainerType.DRAFT);
			HibNodeFieldContainer newContainer = contentDao.createFieldContainer(firstNode, english, firstNode.getProject().getLatestBranch(),
				user(),
				org,
				true);

			firstMicronodeListField = newContainer
				.createMicronodeList(micronodeFieldName);
			HibMicronode micronode = firstMicronodeListField.createMicronode(versionA);
			micronode.createString(oldFieldName).setString("first content");

			// add another micronode from another microschema
			micronode = firstMicronodeListField.createMicronode(microschemaContainer("vcard").getLatestVersion());
			micronode.createString("firstName").setString("Max");
			tx.success();
		}

		String jobUuid = tx(tx -> { return tx.jobDao().enqueueMicroschemaMigration(user(), project().getLatestBranch(), versionA, versionB).getUuid(); });

		triggerAndWaitForJob(jobUuid);

		try (Tx tx = tx()) {

			// assert that migration worked and created a new version
			assertThat(firstMicronodeListField.getList().get(0).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(firstMicronodeListField.getList().get(0).getMicronode().getString(oldFieldName).getString()).as("Old field value").isEqualTo(
				"first content");
			assertThat(firstMicronodeListField.getList().get(1).getMicronode()).as("Old Micronode").isOf(microschemaContainer("vcard")
				.getLatestVersion());
			assertThat(firstMicronodeListField.getList().get(1).getMicronode().getString("firstName").getString()).as("Old field value").isEqualTo(
				"Max");

			assertThat(tx.contentDao().getFieldContainer(firstNode, "en")).as("Migrated field container").hasVersion("2.1");
			assertThat(
				tx.contentDao().getFieldContainer(firstNode, "en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode()).as(
					"Migrated Micronode").isOf(versionB);
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getMicronodeList(micronodeFieldName).getList().get(0)
				.getMicronode().getString(
					newFieldName)
				.getString()).as("Migrated field value").isEqualTo("first content");

			assertThat(
				tx.contentDao().getFieldContainer(firstNode, "en").getMicronodeList(micronodeFieldName).getList().get(1).getMicronode()).as(
					"Not migrated Micronode").isOf(microschemaContainer("vcard").getLatestVersion());
			assertThat(tx.contentDao().getFieldContainer(firstNode, "en").getMicronodeList(micronodeFieldName).getList().get(1)
				.getMicronode().getString(
					"firstName")
				.getString()).as("Not migrated field value").isEqualTo("Max");
		}

		JobListResponse status = adminCall(() -> client().findJobs());
		assertThat(status).listsAll(COMPLETED).hasInfos(1);

	}

	private HibSchema createDummySchemaWithChanges(String oldFieldName, String newFieldName, boolean setAddRaw) {
		PersistingSchemaDao schemaDao = CommonTx.get().schemaDao();

		// create version 1 of the schema
		HibSchema container = schemaDao.createPersisted(UUIDUtil.randomUUID(), s -> {
			s.setName(UUID.randomUUID().toString());
			s.setCreated(user());
		});
		HibSchemaVersion versionA = createSchemaVersion(Tx.get(), container, v -> {
			SchemaVersionModel schemaA = new SchemaModelImpl();
			schemaA.setName("migratedSchema");
			schemaA.setVersion("1.0");
			FieldSchema oldField = FieldUtil.createStringFieldSchema(oldFieldName);
			schemaA.addField(oldField);
			schemaA.addField(FieldUtil.createStringFieldSchema("name"));
			schemaA.setDisplayField("name");
			schemaA.setSegmentField("name");
			schemaA.setContainer(false);
			schemaA.validate();
			v.setName("migratedSchema");
			v.setSchema(schemaA);
			container.setLatestVersion(v);
		});
		container.generateBucketId();
		schemaDao.mergeIntoPersisted(container);
		
		// create version 2 of the schema (with the field renamed)
		HibSchemaVersion versionB = createSchemaVersion(Tx.get(), container, v -> {
			SchemaVersionModel schemaB = new SchemaModelImpl();
			schemaB.setName("migratedSchema");
			schemaB.setVersion("2.0");
			FieldSchema newField = FieldUtil.createStringFieldSchema(newFieldName);
			if (setAddRaw) {
				newField.setElasticsearch(IndexOptionHelper.getRawFieldOption());
			}
			schemaB.addField(newField);
			schemaB.addField(FieldUtil.createStringFieldSchema("name"));
			schemaB.setDisplayField("name");
			schemaB.setSegmentField("name");
			schemaB.setContainer(false);
			schemaB.validate();
			v.setName("migratedSchema");
			v.setSchema(schemaB);
			v.setSchemaContainer(container);
		});

		// link the schemas with the changes in between
		HibUpdateFieldChange updateFieldChange = (HibUpdateFieldChange) schemaDao.createPersistedChange(versionA, SchemaChangeOperation.UPDATEFIELD);
		updateFieldChange.setFieldName(oldFieldName);
		updateFieldChange.setRestProperty(SchemaChangeModel.NAME_KEY, newFieldName);
		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);
		versionA.setNextChange(updateFieldChange);

		// Link everything together
		container.setLatestVersion(versionB);
		versionA.setNextVersion(versionB);
		versionB.setPreviousVersion(versionA);
		schemaDao.mergeIntoPersisted(container);
		Tx.get().commit();
		actions().updateSchemaVersion(versionA);
		actions().updateSchemaVersion(versionB);
		return container;
	}

	/**
	 * Start a schema migration, await() the result and assert success
	 * 
	 * @param versionA
	 *            version A
	 * @param versionB
	 *            version B
	 * @throws Throwable
	 */
	private void doSchemaMigration(HibSchemaVersion versionA, HibSchemaVersion versionB) throws Throwable {
		String jobUuid = tx(tx -> {
			JobDao jobDao = tx.jobDao();
			return jobDao.enqueueSchemaMigration(user(), project().getLatestBranch(), versionA, versionB).getUuid();
		});
		triggerAndWaitForJob(jobUuid);
	}

}
