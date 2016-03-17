package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
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

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.GraphFieldContainerEdge.Type;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerImpl;
import com.gentics.mesh.core.data.container.impl.MicroschemaContainerVersionImpl;
import com.gentics.mesh.core.data.node.Node;
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
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaModel;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.test.TestUtils;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

public class NodeMigrationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Autowired
	private EventbusVerticle eventbusVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(eventbusVerticle);
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

	@Test
	public void testStartSchemaMigration() throws Throwable {
		String fieldName = "changedfield";

		SchemaContainer container = createDummySchemaWithChanges(fieldName);
		SchemaContainerVersion versionB = container.getLatestVersion();
		SchemaContainerVersion versionA = versionB.getPreviousVersion();

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node firstNode = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer firstEnglishContainer = firstNode.createGraphFieldContainer(english,
				firstNode.getProject().getLatestRelease(), Type.DRAFT);
		firstEnglishContainer.createString(fieldName).setString("first content");

		Node secondNode = parentNode.create(user, versionA, project());
		NodeGraphFieldContainer secondEnglishContainer = secondNode.createGraphFieldContainer(english,
				secondNode.getProject().getLatestRelease(), Type.DRAFT);
		secondEnglishContainer.createString(fieldName).setString("second content");

		DeliveryOptions options = new DeliveryOptions();
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

		// assert that migration worked
		firstNode.reload();
		firstNode.getGraphFieldContainer("en").reload();
		assertThat(firstNode).as("Migrated Node").isOf(versionB).hasTranslation("en");
		assertThat(firstNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
				.isEqualTo("modified first content");
		secondNode.reload();
		secondNode.getGraphFieldContainer("en").reload();
		assertThat(secondNode).as("Migrated Node").isOf(versionB).hasTranslation("en");
		assertThat(secondNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
				.isEqualTo("modified second content");
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
		schemaA.setDisplayField("name");
		schemaA.setSegmentField("name");
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
		schemaB.setDisplayField("name");
		schemaB.setSegmentField("name");
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

	@Test
	public void testStartMicroschemaMigration() throws Throwable {
		String fieldName = "changedfield";
		String micronodeFieldName = "micronodefield";

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

		MicronodeGraphField firstMicronodeField = firstNode
				.createGraphFieldContainer(english, firstNode.getProject().getLatestRelease(), Type.DRAFT)
				.createMicronode(micronodeFieldName, versionA);
		firstMicronodeField.getMicronode().createString(fieldName).setString("first content");

		Node secondNode = folder("news");
		MicronodeGraphField secondMicronodeField = secondNode
				.createGraphFieldContainer(english, secondNode.getProject().getLatestRelease(), Type.DRAFT)
				.createMicronode(micronodeFieldName, versionA);
		secondMicronodeField.getMicronode().createString(fieldName).setString("second content");

		DeliveryOptions options = new DeliveryOptions();
		options.addHeader(NodeMigrationVerticle.UUID_HEADER, container.getUuid());
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

		// assert that migration worked
		firstMicronodeField.getMicronode().reload();
		assertThat(firstMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(firstMicronodeField.getMicronode().getString(fieldName).getString()).as("Migrated field value")
				.isEqualTo("modified first content");

		secondMicronodeField.getMicronode().reload();
		assertThat(secondMicronodeField.getMicronode()).as("Migrated Micronode").isOf(versionB);
		assertThat(secondMicronodeField.getMicronode().getString(fieldName).getString()).as("Migrated field value")
				.isEqualTo("modified second content");
	}
}
