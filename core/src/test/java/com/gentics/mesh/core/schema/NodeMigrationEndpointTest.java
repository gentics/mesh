package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.gentics.ferma.Tx;
import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.ContainerType;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.SchemaContainerVersion;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModelImpl;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModelImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaUpdateRequest;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.performance.TestUtils;
import com.gentics.mesh.util.Tuple;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class NodeMigrationEndpointTest extends AbstractMeshTest {

	@Before
	public void deployWorkerVerticle() throws Exception {
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx().deployVerticle(meshDagger().nodeMigrationVerticle(), options);
	}

	@Test
	public void testEmptyMigration() throws Throwable {
		try (Tx tx = tx()) {

			CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());

			String fieldName = "changedfield";

			SchemaContainer container = createDummySchemaWithChanges(fieldName);
			SchemaContainerVersion versionB = container.getLatestVersion();
			SchemaContainerVersion versionA = versionB.getPreviousVersion();

			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());

			// Trigger migration by sending a event
			vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
				latch.countDown();
			});

			failingLatch(latch);

		}

	}

	@Test
	public void testStartSchemaMigration() throws Throwable {
		SchemaContainer container;
		SchemaContainerVersion versionA;
		SchemaContainerVersion versionB;
		Node firstNode;
		Node secondNode;
		String fieldName = "changedfield";

		try (Tx tx = tx()) {
			container = createDummySchemaWithChanges(fieldName);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node based on the old schema
			User user = user();
			Language english = english();
			Node parentNode = folder("2015");
			firstNode = parentNode.create(user, versionA, project());
			NodeGraphFieldContainer firstEnglishContainer = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(),
					user);
			firstEnglishContainer.createString(fieldName).setString("first content");

			secondNode = parentNode.create(user, versionA, project());
			NodeGraphFieldContainer secondEnglishContainer = secondNode.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(),
					user);
			secondEnglishContainer.createString(fieldName).setString("second content");

			project().getLatestRelease().assignSchemaVersion(versionB);
			tx.success();
		}

		try (Tx tx = tx()) {
			doSchemaMigration(container, versionA, versionB);

			// assert that migration worked
			firstNode.reload();
			firstNode.getGraphFieldContainer("en").reload();
			assertThat(firstNode).as("Migrated Node").isOf(container).hasTranslation("en");
			assertThat(firstNode.getGraphFieldContainer("en")).as("Migrated field container").isOf(versionB).hasVersion("0.2");
			assertThat(firstNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
					.isEqualTo("modified first content");
			secondNode.reload();
			secondNode.getGraphFieldContainer("en").reload();
			assertThat(secondNode).as("Migrated Node").isOf(container).hasTranslation("en");
			assertThat(secondNode.getGraphFieldContainer("en")).as("Migrated field container").isOf(versionB).hasVersion("0.2");
			assertThat(secondNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
					.isEqualTo("modified second content");
			assertThat(dummySearchProvider()).hasEvents(2, 0, 0, 0);
		}
	}

	@Test
	public void testMigrateAgain() throws Throwable {
		String fieldName = "changedfield";
		SchemaContainer container;
		SchemaContainerVersion versionA;
		SchemaContainerVersion versionB;
		Node firstNode;

		try (Tx tx = tx()) {
			container = createDummySchemaWithChanges(fieldName);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node based on the old schema
			User user = user();
			Language english = english();
			Node parentNode = folder("2015");
			firstNode = parentNode.create(user, versionA, project());
			NodeGraphFieldContainer firstEnglishContainer = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(),
					user);
			firstEnglishContainer.createString(fieldName).setString("first content");

			// do the schema migration twice
			project().getLatestRelease().assignSchemaVersion(versionB);
			tx.success();
		}

		try (Tx tx = tx()) {
			doSchemaMigration(container, versionA, versionB);
			doSchemaMigration(container, versionA, versionB);

			// assert that migration worked, but was only performed once
			firstNode.reload();
			firstNode.getGraphFieldContainer("en").reload();
			assertThat(firstNode).as("Migrated Node").isOf(container).hasTranslation("en");
			assertThat(firstNode.getGraphFieldContainer("en")).as("Migrated field container").isOf(versionB).hasVersion("0.2");
			assertThat(firstNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
					.isEqualTo("modified first content");
		}
	}

	@Test
	public void testMigratePublished() throws Throwable {
		String fieldName = "changedfield";
		SchemaContainer container;
		SchemaContainerVersion versionA;
		SchemaContainerVersion versionB;
		Node node;

		try (Tx tx = tx()) {
			container = createDummySchemaWithChanges(fieldName);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node and publish
			node = folder("2015").create(user(), versionA, project());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english(), project().getLatestRelease(), user());
			englishContainer.createString(fieldName).setString("content");
			englishContainer.createString("name").setString("someName");
			InternalActionContext ac = new InternalRoutingActionContextImpl(mockRoutingContext());
			node.publish(ac, createBatch(), "en");

			project().getLatestRelease().assignSchemaVersion(versionB);
			tx.success();
		}

		try (Tx tx = tx()) {
			doSchemaMigration(container, versionA, versionB);

			node.reload();
			assertThat(node.getGraphFieldContainer("en")).as("Migrated draft").isOf(versionB).hasVersion("2.0");
			assertThat(node.getGraphFieldContainer("en", project().getLatestRelease().getUuid(), ContainerType.PUBLISHED)).as("Migrated published")
					.isOf(versionB).hasVersion("2.0");
		}
	}

	@Test
	public void testMicronodeListMigration() throws Exception {

		// 1. Prepare the schema and add micronode list to the content schema
		SchemaUpdateRequest schemaUpdate = db()
				.tx(() -> JsonUtil.readValue(schemaContainer("content").getLatestVersion().getJson(), SchemaUpdateRequest.class));
		schemaUpdate.addField(FieldUtil.createListFieldSchema("micronode", "micronode").setAllowedSchemas("vcard"));

		String schemaUuid = db().tx(() -> schemaContainer("content").getUuid());

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(client());
		call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		failingLatch(latch);

		String parentNodeUuid = tx(() -> folder("2015").getUuid());
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setSchema(new SchemaReference().setName("content"));
		nodeCreateRequest.getFields().put("teaser", FieldUtil.createStringField("test"));
		nodeCreateRequest.getFields().put("slug", FieldUtil.createStringField("test"));

		MicronodeField micronodeA = FieldUtil.createMicronodeField("vcard",
				Tuple.tuple("firstName", FieldUtil.createStringField("test-updated-firstname")),
				Tuple.tuple("lastName", FieldUtil.createStringField("test-updated-lastname")));

		MicronodeField micronodeB = FieldUtil.createMicronodeField("vcard", Tuple.tuple("firstName", FieldUtil.createStringField("test")),
				Tuple.tuple("lastName", FieldUtil.createStringField("test")));

		nodeCreateRequest.getFields().put("micronode", FieldUtil.createMicronodeListField(micronodeA, micronodeB));
		nodeCreateRequest.setParentNodeUuid(parentNodeUuid);
		nodeCreateRequest.setLanguage("en");

		// 1. Create a node which contains a micronode list and at least two micronodes
		NodeResponse response = call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
		String nodeUuid = response.getUuid();
		assertThat(response.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		call(() -> client().publishNode(PROJECT_NAME, nodeUuid));

		// 2. Update the name and allow of the micronode list of the used schema
		latch = TestUtils.latchForMigrationCompleted(client());
		schemaUpdate.setName("someOtherName");
		ListFieldSchema micronodeListFieldSchema = schemaUpdate.getField("micronode", ListFieldSchema.class);
		micronodeListFieldSchema.setAllowedSchemas("vcard", "captionedImage");
		call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		failingLatch(latch);

		// 3. Assert that the node still contains the micronode list contents
		NodeResponse migratedNode = call(
				() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion("published")));
		assertThat(migratedNode.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		assertNotEquals("The node should have been migrated due to the schema update.", migratedNode.getVersion(), response.getVersion());

		// 4. Update the allow of the micronode list of the used schema
		latch = TestUtils.latchForMigrationCompleted(client());
		ListFieldSchema micronodeListFieldSchema2 = schemaUpdate.getField("micronode", ListFieldSchema.class);
		micronodeListFieldSchema2.setAllowedSchemas("vcard");
		schemaUpdate.addField(FieldUtil.createMicronodeFieldSchema("otherMicronode").setAllowedMicroSchemas("vcard"));
		call(() -> client().updateSchema(schemaUuid, schemaUpdate));
		failingLatch(latch);

		// 5. Assert that the node still contains the micronode list contents
		migratedNode = call(() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion("published")));
		assertThat(migratedNode.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		assertNotEquals("The node should have been migrated due to the schema update.", migratedNode.getVersion(), response.getVersion());

		// 6. Now update the name of the microschema
		String microschemaUuid = tx(() -> microschemaContainer("vcard").getUuid());
		MicroschemaUpdateRequest microschemaUpdate = db()
				.tx(() -> JsonUtil.readValue(microschemaContainer("vcard").getLatestVersion().getJson(), MicroschemaUpdateRequest.class));
		microschemaUpdate.setName("someOtherName2");
		microschemaUpdate.addField(FieldUtil.createStringFieldSchema("enemenemuh"));
		latch = TestUtils.latchForMigrationCompleted(client());
		call(() -> client().updateMicroschema(microschemaUuid, microschemaUpdate));
		failingLatch(latch);

		// 7. Verify that the node has been migrated again
		NodeResponse migratedNode2 = call(
				() -> client().findNodeByUuid(PROJECT_NAME, nodeUuid, new VersioningParametersImpl().setVersion("published")));
		assertThat(migratedNode2.getFields().getMicronodeFieldList("micronode").getItems()).isNotEmpty();
		assertNotEquals("The node should have been migrated due to the schema update.", migratedNode.getVersion(), migratedNode2.getVersion());

	}

	@Test
	public void testMigrateDraftAndPublished() throws Throwable {
		String fieldName = "changedfield";
		Node node;
		SchemaContainerVersion versionA;
		SchemaContainerVersion versionB;
		SchemaContainer container;

		try (Tx tx = tx()) {
			container = createDummySchemaWithChanges(fieldName);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node and publish
			node = folder("2015").create(user(), versionA, project());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english(), project().getLatestRelease(), user());
			englishContainer.createString("name").setString("someName");
			englishContainer.createString(fieldName).setString("content");
			tx.success();
		}

		try (Tx tx = tx()) {
			node.reload();
			InternalActionContext ac = new InternalRoutingActionContextImpl(mockRoutingContext());
			node.publish(ac, createBatch(), "en");
			tx.success();
		}

		try (Tx tx = tx()) {
			node.reload();
			NodeGraphFieldContainer updatedEnglishContainer = node.createGraphFieldContainer(english(), project().getLatestRelease(), user());
			updatedEnglishContainer.getString(fieldName).setString("new content");
			project().getLatestRelease().assignSchemaVersion(versionB);
			tx.success();
		}

		try (Tx tx = tx()) {
			doSchemaMigration(container, versionA, versionB);
			node.reload();
			assertThat(node.getGraphFieldContainer("en")).as("Migrated draft").isOf(versionB).hasVersion("2.1");
			assertThat(node.getGraphFieldContainer("en", project().getLatestRelease().getUuid(), ContainerType.PUBLISHED)).as("Migrated published")
					.isOf(versionB).hasVersion("2.0");
		}
	}

	private SchemaContainer createDummySchemaWithChanges(String fieldName) {

		SchemaContainer container = Tx.getActive().getGraph().addFramedVertex(SchemaContainerImpl.class);
		boot().schemaContainerRoot().addSchemaContainer(container);

		// create version 1 of the schema
		SchemaContainerVersion versionA = Tx.getActive().getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		SchemaModel schemaA = new SchemaModelImpl();
		schemaA.setName("migratedSchema");
		schemaA.setVersion("1.0");
		FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
		schemaA.addField(oldField);
		schemaA.addField(FieldUtil.createStringFieldSchema("name"));
		schemaA.setDisplayField("name");
		schemaA.setSegmentField("name");
		schemaA.validate();
		versionA.setName("migratedSchema");
		versionA.setSchema(schemaA);
		versionA.setSchemaContainer(container);

		// create version 2 of the schema (with the field renamed)
		SchemaContainerVersion versionB = Tx.getActive().getGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		SchemaModel schemaB = new SchemaModelImpl();
		schemaB.setName("migratedSchema");
		schemaB.setVersion("2.0");
		FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
		schemaB.addField(newField);
		schemaB.addField(FieldUtil.createStringFieldSchema("name"));
		schemaB.setDisplayField("name");
		schemaB.setSegmentField("name");
		schemaB.validate();
		versionB.setName("migratedSchema");
		versionB.setSchema(schemaB);
		versionB.setSchemaContainer(container);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Tx.getActive().getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(
				"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);

		// Link everything together
		container.setLatestVersion(versionB);
		versionA.setNextVersion(versionB);
		boot().schemaContainerRoot().addSchemaContainer(container);
		return container;

	}

	/**
	 * Start a schema migration, await the result and assert success
	 * 
	 * @param container
	 *            schema container
	 * @param versionA
	 *            version A
	 * @param versionB
	 *            version B
	 * @throws Throwable
	 */
	private void doSchemaMigration(SchemaContainer container, SchemaContainerVersion versionA, SchemaContainerVersion versionB) throws Throwable {
		DeliveryOptions options = new DeliveryOptions();
		options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
		options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
		options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
		options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
		options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
		CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
		vertx().eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
			future.complete(rh);
		});

		AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
		if (result.cause() != null) {
			throw result.cause();
		}
	}

	@Test
	public void testStartMicroschemaMigration() throws Throwable {
		String fieldName = "changedfield";
		String micronodeFieldName = "micronodefield";
		MicroschemaContainer container;
		MicroschemaContainerVersion versionA;
		MicroschemaContainerVersion versionB;
		MicronodeGraphField firstMicronodeField;
		MicronodeGraphField secondMicronodeField;
		Node firstNode;
		Node secondNode;

		try (Tx tx = tx()) {
			call(() -> client().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new PublishParametersImpl().setRecursive(true)));

			// create version 1 of the microschema
			container = tx.getGraph().addFramedVertex(MicroschemaContainerImpl.class);
			versionA = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			versionA.setSchemaContainer(container);

			MicroschemaModelImpl microschemaA = new MicroschemaModelImpl();
			microschemaA.setName("migratedSchema");
			microschemaA.setVersion("1.0");
			FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaA.addField(oldField);
			versionA.setName("migratedSchema");
			versionA.setSchema(microschemaA);
			boot().microschemaContainerRoot().addMicroschema(container);

			// create version 2 of the microschema (with the field renamed)
			versionB = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			versionB.setSchemaContainer(container);
			MicroschemaModelImpl microschemaB = new MicroschemaModelImpl();
			microschemaB.setName("migratedSchema");
			microschemaB.setVersion("2.0");
			FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaB.addField(newField);
			versionB.setName("migratedSchema");
			versionB.setSchema(microschemaB);
			// boot().microschemaContainerRoot().addMicroschema(container);

			// link the schemas with the changes in between
			UpdateFieldChangeImpl updateFieldChange = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			updateFieldChange.setFieldName(fieldName);
			updateFieldChange.setCustomMigrationScript(
					"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);

			// create micronode based on the old schema
			Language english = english();
			firstNode = folder("2015");
			SchemaModel schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
			schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(versionA.getName());
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			firstMicronodeField = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), user())
					.createMicronode(micronodeFieldName, versionA);
			firstMicronodeField.getMicronode().createString(fieldName).setString("first content");

			secondNode = folder("news");
			secondMicronodeField = secondNode.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(), user())
					.createMicronode(micronodeFieldName, versionA);
			secondMicronodeField.getMicronode().createString(fieldName).setString("second content");
			tx.success();
		}

		try (Tx tx = tx()) {
			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
			options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
			vertx().eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
				future.complete(rh);
			});

			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			if (result.cause() != null) {
				throw result.cause();
			}

			// assert that migration worked and created a new version
			firstMicronodeField.getMicronode().reload();
			assertThat(firstMicronodeField.getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(firstMicronodeField.getMicronode().getString(fieldName).getString()).as("Old field value").isEqualTo("first content");

			firstNode.reload();
			assertThat(firstNode.getGraphFieldContainer("en")).as("Migrated field container").hasVersion("1.2");
			assertThat(firstNode.getGraphFieldContainer("en").getMicronode(micronodeFieldName).getMicronode()).as("Migrated Micronode")
					.isOf(versionB);
			assertThat(firstNode.getGraphFieldContainer("en").getMicronode(micronodeFieldName).getMicronode().getString(fieldName).getString())
					.as("Migrated field value").isEqualTo("modified first content");

			secondNode.reload();
			secondMicronodeField.getMicronode().reload();
			assertThat(secondMicronodeField.getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(secondMicronodeField.getMicronode().getString(fieldName).getString()).as("Old field value").isEqualTo("second content");

			assertThat(secondNode.getGraphFieldContainer("en")).as("Migrated field container").hasVersion("1.2");
			assertThat(secondNode.getGraphFieldContainer("en").getMicronode(micronodeFieldName).getMicronode()).as("Migrated Micronode")
					.isOf(versionB);
			assertThat(secondNode.getGraphFieldContainer("en").getMicronode(micronodeFieldName).getMicronode().getString(fieldName).getString())
					.as("Migrated field value").isEqualTo("modified second content");
		}
	}

	@Test
	public void testMicroschemaMigrationInListField() throws Throwable {
		String fieldName = "changedfield";
		String micronodeFieldName = "micronodefield";
		MicroschemaContainer container;
		MicroschemaContainerVersion versionA;
		MicroschemaContainerVersion versionB;
		MicronodeGraphFieldList firstMicronodeListField;
		MicronodeGraphFieldList secondMicronodeListField;
		Node firstNode;
		Node secondNode;

		try (Tx tx = tx()) {
			// create version 1 of the microschema
			container = tx.getGraph().addFramedVertex(MicroschemaContainerImpl.class);
			versionA = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			versionA.setSchemaContainer(container);

			MicroschemaModelImpl microschemaA = new MicroschemaModelImpl();
			microschemaA.setName("migratedSchema");
			microschemaA.setVersion("1.0");
			FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaA.addField(oldField);
			versionA.setName("migratedSchema");
			versionA.setSchema(microschemaA);
			boot().microschemaContainerRoot().addMicroschema(container);

			// create version 2 of the microschema (with the field renamed)
			versionB = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			versionB.setSchemaContainer(container);
			MicroschemaModelImpl microschemaB = new MicroschemaModelImpl();
			microschemaB.setName("migratedSchema");
			microschemaB.setVersion("2.0");
			FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaB.addField(newField);
			versionB.setName("migratedSchema");
			versionB.setSchema(microschemaB);
			// boot().microschemaContainerRoot().addMicroschema(container);

			// link the schemas with the changes in between
			UpdateFieldChangeImpl updateFieldChange = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			updateFieldChange.setFieldName(fieldName);
			updateFieldChange.setCustomMigrationScript(
					"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);

			// create micronode based on the old schema
			Language english = english();
			firstNode = folder("2015");
			SchemaModel schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(versionA.getName()).setName(micronodeFieldName)
					.setLabel("Micronode List Field"));
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			firstMicronodeListField = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), user())
					.createMicronodeFieldList(micronodeFieldName);
			Micronode micronode = firstMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("first content");

			secondNode = folder("news");
			secondMicronodeListField = secondNode.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(), user())
					.createMicronodeFieldList(micronodeFieldName);
			micronode = secondMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("second content");
			micronode = secondMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("third content");
			tx.success();
		}

		try (Tx tx = tx()) {
			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
			options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
			vertx().eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
				future.complete(rh);
			});

			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			if (result.cause() != null) {
				throw result.cause();
			}

			// assert that migration worked and created a new version
			firstMicronodeListField.getList().forEach((field) -> {
				field.getMicronode().reload();
			});
			assertThat(firstMicronodeListField.getList().get(0).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(firstMicronodeListField.getList().get(0).getMicronode().getString(fieldName).getString()).as("Old field value")
					.isEqualTo("first content");

			firstNode.reload();
			assertThat(firstNode.getGraphFieldContainer("en")).as("Migrated field container").hasVersion("2.1");
			assertThat(firstNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode())
					.as("Migrated Micronode").isOf(versionB);
			assertThat(firstNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode()
					.getString(fieldName).getString()).as("Migrated field value").isEqualTo("modified first content");

			secondNode.reload();
			secondMicronodeListField.getList().forEach((field) -> {
				field.getMicronode().reload();
			});
			assertThat(secondMicronodeListField.getList().get(0).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(secondMicronodeListField.getList().get(0).getMicronode().getString(fieldName).getString()).as("Old field value")
					.isEqualTo("second content");
			assertThat(secondMicronodeListField.getList().get(1).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(secondMicronodeListField.getList().get(1).getMicronode().getString(fieldName).getString()).as("Old field value")
					.isEqualTo("third content");

			assertThat(secondNode.getGraphFieldContainer("en")).as("Migrated field container").hasVersion("2.1");
			assertThat(secondNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode())
					.as("Migrated Micronode").isOf(versionB);
			assertThat(secondNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode()
					.getString(fieldName).getString()).as("Migrated field value").isEqualTo("modified second content");
			assertThat(secondNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(1).getMicronode())
					.as("Migrated Micronode").isOf(versionB);
			assertThat(secondNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(1).getMicronode()
					.getString(fieldName).getString()).as("Migrated field value").isEqualTo("modified third content");
		}
	}

	@Test
	public void testMicroschemaMigrationMixedList() throws Throwable {
		String fieldName = "changedfield";
		String micronodeFieldName = "micronodefield";
		MicroschemaContainer container;
		MicroschemaContainerVersion versionA;
		MicroschemaContainerVersion versionB;
		MicronodeGraphFieldList firstMicronodeListField;
		Node firstNode;

		try (Tx tx = tx()) {
			// create version 1 of the microschema
			container = tx.getGraph().addFramedVertex(MicroschemaContainerImpl.class);
			versionA = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			versionA.setSchemaContainer(container);

			MicroschemaModelImpl microschemaA = new MicroschemaModelImpl();
			microschemaA.setName("migratedSchema");
			microschemaA.setVersion("1.0");
			FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaA.addField(oldField);
			versionA.setName("migratedSchema");
			versionA.setSchema(microschemaA);
			boot().microschemaContainerRoot().addMicroschema(container);

			// create version 2 of the microschema (with the field renamed)
			versionB = tx.getGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			versionB.setSchemaContainer(container);
			MicroschemaModelImpl microschemaB = new MicroschemaModelImpl();
			microschemaB.setName("migratedSchema");
			microschemaB.setVersion("2.0");
			FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaB.addField(newField);
			versionB.setName("migratedSchema");
			versionB.setSchema(microschemaB);
			// boot().microschemaContainerRoot().addMicroschema(container);

			// link the schemas with the changes in between
			UpdateFieldChangeImpl updateFieldChange = tx.getGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			updateFieldChange.setFieldName(fieldName);
			updateFieldChange.setCustomMigrationScript(
					"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);

			// create micronode based on the old schema
			Language english = english();
			firstNode = folder("2015");
			SchemaModel schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(versionA.getName(), "vcard")
					.setName(micronodeFieldName).setLabel("Micronode List Field"));
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			firstMicronodeListField = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), user())
					.createMicronodeFieldList(micronodeFieldName);
			Micronode micronode = firstMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("first content");

			// add another micronode from another microschema
			micronode = firstMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			micronode.createString("firstName").setString("Max");
			tx.success();
		}

		try (Tx tx = tx()) {
			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
			options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
			vertx().eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
				future.complete(rh);
			});

			AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			if (result.cause() != null) {
				throw result.cause();
			}

			// assert that migration worked and created a new version
			firstMicronodeListField.getList().forEach((field) -> {
				field.getMicronode().reload();
			});
			assertThat(firstMicronodeListField.getList().get(0).getMicronode()).as("Old Micronode").isOf(versionA);
			assertThat(firstMicronodeListField.getList().get(0).getMicronode().getString(fieldName).getString()).as("Old field value")
					.isEqualTo("first content");
			assertThat(firstMicronodeListField.getList().get(1).getMicronode()).as("Old Micronode")
					.isOf(microschemaContainer("vcard").getLatestVersion());
			assertThat(firstMicronodeListField.getList().get(1).getMicronode().getString("firstName").getString()).as("Old field value")
					.isEqualTo("Max");

			firstNode.reload();
			assertThat(firstNode.getGraphFieldContainer("en")).as("Migrated field container").hasVersion("2.1");
			assertThat(firstNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode())
					.as("Migrated Micronode").isOf(versionB);
			assertThat(firstNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(0).getMicronode()
					.getString(fieldName).getString()).as("Migrated field value").isEqualTo("modified first content");

			assertThat(firstNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(1).getMicronode())
					.as("Not migrated Micronode").isOf(microschemaContainer("vcard").getLatestVersion());
			assertThat(firstNode.getGraphFieldContainer("en").getMicronodeList(micronodeFieldName).getList().get(1).getMicronode()
					.getString("firstName").getString()).as("Not migrated field value").isEqualTo("Max");
		}
	}
}
