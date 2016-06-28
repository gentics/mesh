package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.apache.commons.lang3.StringUtils.endsWith;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.AbstractSpringVerticle;
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
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.ListFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.node.NodeVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.impl.TakeOfflineParameters;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;
import com.gentics.mesh.test.TestUtils;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

public class NodeMigrationVerticleTest extends AbstractIsolatedRestVerticleTest {

	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Autowired
	private EventbusVerticle eventbusVerticle;

	@Autowired
	private NodeVerticle nodeVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(eventbusVerticle);
		list.add(nodeVerticle);
		return list;
	}

	@Override
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx.deployVerticle(nodeMigrationVerticle, options);
	}

	@Test
	@Ignore("Unstable test")
	public void testEmptyMigration() throws Throwable {
		try (NoTrx tx = db.noTrx()) {

			CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

			String fieldName = "changedfield";

			SchemaContainer container = createDummySchemaWithChanges(fieldName);
			SchemaContainerVersion versionB = container.getLatestVersion();
			SchemaContainerVersion versionA = versionB.getPreviousVersion();

			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();

			// Trigger migration by sending a event
			vertx.eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
				//			future.complete(rh);
			});

			failingLatch(latch);

			//		AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
			//		if (result.cause() != null) {
			//			throw result.cause();
			//		}
		}

	}

	@Test
	public void testStartSchemaMigration() throws Throwable {
		try (NoTrx tx = db.noTrx()) {

			String fieldName = "changedfield";

			SchemaContainer container = createDummySchemaWithChanges(fieldName);
			SchemaContainerVersion versionB = container.getLatestVersion();
			SchemaContainerVersion versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node based on the old schema
			User user = user();
			Language english = english();
			Node parentNode = folder("2015");
			Node firstNode = parentNode.create(user, versionA, project());
			NodeGraphFieldContainer firstEnglishContainer = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(),
					user);
			firstEnglishContainer.createString(fieldName).setString("first content");

			Node secondNode = parentNode.create(user, versionA, project());
			NodeGraphFieldContainer secondEnglishContainer = secondNode.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(),
					user);
			secondEnglishContainer.createString(fieldName).setString("second content");

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
		}
	}

	@Test
	public void testMigrateAgain() throws Throwable {
		try (NoTrx tx = db.noTrx()) {

			String fieldName = "changedfield";

			SchemaContainer container = createDummySchemaWithChanges(fieldName);
			SchemaContainerVersion versionB = container.getLatestVersion();
			SchemaContainerVersion versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node based on the old schema
			User user = user();
			Language english = english();
			Node parentNode = folder("2015");
			Node firstNode = parentNode.create(user, versionA, project());
			NodeGraphFieldContainer firstEnglishContainer = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(),
					user);
			firstEnglishContainer.createString(fieldName).setString("first content");

			// do the schema migration twice
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
		try (NoTrx tx = db.noTrx()) {

			String fieldName = "changedfield";

			SchemaContainer container = createDummySchemaWithChanges(fieldName);
			SchemaContainerVersion versionB = container.getLatestVersion();
			SchemaContainerVersion versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node and publish
			Node node = folder("2015").create(user(), versionA, project());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english(), project().getLatestRelease(), user());
			englishContainer.createString(fieldName).setString("content");
			englishContainer.createString("name").setString("someName");
			node.publish(InternalActionContext.create(getMockedRoutingContext(user())), "en").toBlocking().single();

			doSchemaMigration(container, versionA, versionB);

			node.reload();
			assertThat(node.getGraphFieldContainer("en")).as("Migrated draft").isOf(versionB).hasVersion("2.0");
			assertThat(node.getGraphFieldContainer("en", project().getLatestRelease().getUuid(), ContainerType.PUBLISHED)).as("Migrated published")
					.isOf(versionB).hasVersion("2.0");
		}
	}

	@Test
	public void testMigrateDraftAndPublished() throws Throwable {
		String fieldName = "changedfield";
		Node node = null;
		SchemaContainerVersion versionA = null;
		SchemaContainerVersion versionB = null;
		SchemaContainer container = null;

		try (NoTrx tx = db.noTrx()) {
			container = createDummySchemaWithChanges(fieldName);
			versionB = container.getLatestVersion();
			versionA = versionB.getPreviousVersion();

			project().getLatestRelease().assignSchemaVersion(versionA);

			// create a node and publish
			node = folder("2015").create(user(), versionA, project());
			NodeGraphFieldContainer englishContainer = node.createGraphFieldContainer(english(), project().getLatestRelease(), user());
			englishContainer.createString("name").setString("someName");
			englishContainer.createString(fieldName).setString("content");
		}
		try (NoTrx tx = db.noTrx()) {
			node.reload();
			node.publish(InternalActionContext.create(getMockedRoutingContext(user())), "en").toBlocking().single();
		}

		try (NoTrx tx = db.noTrx()) {
			node.reload();
			NodeGraphFieldContainer updatedEnglishContainer = node.createGraphFieldContainer(english(), project().getLatestRelease(), user());
			updatedEnglishContainer.getString(fieldName).setString("new content");

			doSchemaMigration(container, versionA, versionB);

			node.reload();
			assertThat(node.getGraphFieldContainer("en")).as("Migrated draft").isOf(versionB).hasVersion("2.1");
			assertThat(node.getGraphFieldContainer("en", project().getLatestRelease().getUuid(), ContainerType.PUBLISHED)).as("Migrated published")
					.isOf(versionB).hasVersion("2.0");
		}
	}

	private SchemaContainer createDummySchemaWithChanges(String fieldName) {

		SchemaContainer container = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		boot.schemaContainerRoot().addSchemaContainer(container);

		// create version 1 of the schema
		SchemaContainerVersion versionA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		Schema schemaA = new SchemaModel();
		schemaA.setName("migratedSchema");
		schemaA.setVersion(1);
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
		SchemaContainerVersion versionB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerVersionImpl.class);
		Schema schemaB = new SchemaModel();
		schemaB.setName("migratedSchema");
		schemaB.setVersion(2);
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
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript(
				"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

		updateFieldChange.setPreviousContainerVersion(versionA);
		updateFieldChange.setNextSchemaContainerVersion(versionB);

		// Link everything together
		container.setLatestVersion(versionB);
		versionA.setNextVersion(versionB);
		boot.schemaContainerRoot().addSchemaContainer(container);
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
		vertx.eventBus().send(NodeMigrationVerticle.SCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
			future.complete(rh);
		});

		AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
		if (result.cause() != null) {
			throw result.cause();
		}
	}

	@Test
	public void testStartMicroschemaMigration() throws Throwable {

		try (NoTrx tx = db.noTrx()) {

			String fieldName = "changedfield";
			String micronodeFieldName = "micronodefield";

			call(() -> getClient().takeNodeOffline(PROJECT_NAME, project().getBaseNode().getUuid(), new TakeOfflineParameters().setRecursive(true)));

			// create version 1 of the microschema
			MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
			MicroschemaContainerVersion versionA = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			versionA.setSchemaContainer(container);

			Microschema microschemaA = new MicroschemaModel();
			microschemaA.setName("migratedSchema");
			microschemaA.setVersion(1);
			FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaA.addField(oldField);
			versionA.setName("migratedSchema");
			versionA.setSchema(microschemaA);
			boot.microschemaContainerRoot().addMicroschema(container);

			// create version 2 of the microschema (with the field renamed)
			MicroschemaContainerVersion versionB = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			versionB.setSchemaContainer(container);
			Microschema microschemaB = new MicroschemaModel();
			microschemaB.setName("migratedSchema");
			microschemaB.setVersion(2);
			FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaB.addField(newField);
			versionB.setName("migratedSchema");
			versionB.setSchema(microschemaB);
			//boot.microschemaContainerRoot().addMicroschema(container);

			// link the schemas with the changes in between
			UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			updateFieldChange.setFieldName(fieldName);
			updateFieldChange.setCustomMigrationScript(
					"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);

			// create micronode based on the old schema
			Language english = english();
			Node firstNode = folder("2015");
			Schema schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new MicronodeFieldSchemaImpl().setName(micronodeFieldName).setLabel("Micronode Field"));
			schema.getField(micronodeFieldName, MicronodeFieldSchema.class).setAllowedMicroSchemas(versionA.getName());
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			MicronodeGraphField firstMicronodeField = firstNode.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), user())
					.createMicronode(micronodeFieldName, versionA);
			firstMicronodeField.getMicronode().createString(fieldName).setString("first content");

			Node secondNode = folder("news");
			MicronodeGraphField secondMicronodeField = secondNode
					.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(), user())
					.createMicronode(micronodeFieldName, versionA);
			secondMicronodeField.getMicronode().createString(fieldName).setString("second content");

			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
			options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
			vertx.eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
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

		try (NoTrx tx = db.noTrx()) {

			// create version 1 of the microschema
			MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
			MicroschemaContainerVersion versionA = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			versionA.setSchemaContainer(container);

			Microschema microschemaA = new MicroschemaModel();
			microschemaA.setName("migratedSchema");
			microschemaA.setVersion(1);
			FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaA.addField(oldField);
			versionA.setName("migratedSchema");
			versionA.setSchema(microschemaA);
			boot.microschemaContainerRoot().addMicroschema(container);

			// create version 2 of the microschema (with the field renamed)
			MicroschemaContainerVersion versionB = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			versionB.setSchemaContainer(container);
			Microschema microschemaB = new MicroschemaModel();
			microschemaB.setName("migratedSchema");
			microschemaB.setVersion(2);
			FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaB.addField(newField);
			versionB.setName("migratedSchema");
			versionB.setSchema(microschemaB);
			//boot.microschemaContainerRoot().addMicroschema(container);

			// link the schemas with the changes in between
			UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			updateFieldChange.setFieldName(fieldName);
			updateFieldChange.setCustomMigrationScript(
					"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);

			// create micronode based on the old schema
			Language english = english();
			Node firstNode = folder("2015");
			Schema schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(versionA.getName()).setName(micronodeFieldName)
					.setLabel("Micronode List Field"));
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			MicronodeGraphFieldList firstMicronodeListField = firstNode
					.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), user())
					.createMicronodeFieldList(micronodeFieldName);
			Micronode micronode = firstMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("first content");

			Node secondNode = folder("news");
			MicronodeGraphFieldList secondMicronodeListField = secondNode
					.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(), user())
					.createMicronodeFieldList(micronodeFieldName);
			micronode = secondMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("second content");
			micronode = secondMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("third content");

			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
			options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
			vertx.eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
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

		try (NoTrx tx = db.noTrx()) {
			// create version 1 of the microschema
			MicroschemaContainer container = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerImpl.class);
			MicroschemaContainerVersion versionA = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			container.setLatestVersion(versionA);
			versionA.setSchemaContainer(container);

			Microschema microschemaA = new MicroschemaModel();
			microschemaA.setName("migratedSchema");
			microschemaA.setVersion(1);
			FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaA.addField(oldField);
			versionA.setName("migratedSchema");
			versionA.setSchema(microschemaA);
			boot.microschemaContainerRoot().addMicroschema(container);

			// create version 2 of the microschema (with the field renamed)
			MicroschemaContainerVersion versionB = Database.getThreadLocalGraph().addFramedVertex(MicroschemaContainerVersionImpl.class);
			versionB.setSchemaContainer(container);
			Microschema microschemaB = new MicroschemaModel();
			microschemaB.setName("migratedSchema");
			microschemaB.setVersion(2);
			FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
			microschemaB.addField(newField);
			versionB.setName("migratedSchema");
			versionB.setSchema(microschemaB);
			//boot.microschemaContainerRoot().addMicroschema(container);

			// link the schemas with the changes in between
			UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
			updateFieldChange.setFieldName(fieldName);
			updateFieldChange.setCustomMigrationScript(
					"function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

			updateFieldChange.setPreviousContainerVersion(versionA);
			updateFieldChange.setNextSchemaContainerVersion(versionB);
			versionA.setNextVersion(versionB);

			// create micronode based on the old schema
			Language english = english();
			Node firstNode = folder("2015");
			Schema schema = firstNode.getSchemaContainer().getLatestVersion().getSchema();
			schema.addField(new ListFieldSchemaImpl().setListType("micronode").setAllowedSchemas(versionA.getName(), "vcard")
					.setName(micronodeFieldName).setLabel("Micronode List Field"));
			firstNode.getSchemaContainer().getLatestVersion().setSchema(schema);

			MicronodeGraphFieldList firstMicronodeListField = firstNode
					.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), user())
					.createMicronodeFieldList(micronodeFieldName);
			Micronode micronode = firstMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(versionA);
			micronode.createString(fieldName).setString("first content");

			// add another micronode from another microschema
			micronode = firstMicronodeListField.createMicronode();
			micronode.setSchemaContainerVersion(microschemaContainer("vcard").getLatestVersion());
			micronode.createString("firstName").setString("Max");

			DeliveryOptions options = new DeliveryOptions();
			options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
			options.addHeader(NodeMigrationVerticle.PROJECT_UUID_HEADER, project().getUuid());
			options.addHeader(NodeMigrationVerticle.RELEASE_UUID_HEADER, project().getLatestRelease().getUuid());
			options.addHeader(NodeMigrationVerticle.FROM_VERSION_UUID_HEADER, versionA.getUuid());
			options.addHeader(NodeMigrationVerticle.TO_VERSION_UUID_HEADER, versionB.getUuid());
			CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
			vertx.eventBus().send(NodeMigrationVerticle.MICROSCHEMA_MIGRATION_ADDRESS, null, options, (rh) -> {
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
