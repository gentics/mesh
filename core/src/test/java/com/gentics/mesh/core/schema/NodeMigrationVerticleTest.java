package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl;
import com.gentics.mesh.core.data.schema.impl.UpdateFieldChangeImpl;
import com.gentics.mesh.core.rest.schema.FieldSchema;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.FieldUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.Message;

public class NodeMigrationVerticleTest extends AbstractRestVerticleTest {
	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		return new ArrayList<>();
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
	public void testStartMigration() throws Throwable {
		String fieldName = "changedfield";

		// create version 1 of the schema
		SchemaContainer containerA = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schemaA = new SchemaImpl();
		schemaA.setName("migratedSchema");
		schemaA.setVersion(1);
		FieldSchema oldField = FieldUtil.createStringFieldSchema(fieldName);
		schemaA.addField(oldField);
		schemaA.setDisplayField("name");
		schemaA.setSegmentField("name");
		containerA.setName("migratedSchema");
		containerA.setSchema(schemaA);
		boot.schemaContainerRoot().addSchemaContainer(containerA);

		// create version 2 of the schema (with the field renamed)
		SchemaContainer containerB = Database.getThreadLocalGraph().addFramedVertex(SchemaContainerImpl.class);
		Schema schemaB = new SchemaImpl();
		schemaB.setName("migratedSchema");
		schemaB.setVersion(2);
		FieldSchema newField = FieldUtil.createStringFieldSchema(fieldName);
		schemaB.addField(newField);
		schemaB.setDisplayField("name");
		schemaB.setSegmentField("name");
		containerB.setName("migratedSchema");
		containerB.setSchema(schemaB);
		boot.schemaContainerRoot().addSchemaContainer(containerB);

		// link the schemas with the changes in between
		UpdateFieldChangeImpl updateFieldChange = Database.getThreadLocalGraph().addFramedVertex(UpdateFieldChangeImpl.class);
		updateFieldChange.setFieldName(fieldName);
		updateFieldChange.setCustomMigrationScript("function migrate(node, fieldname, convert) {node.fields[fieldname] = 'modified ' + node.fields[fieldname]; return node;}");

		updateFieldChange.setPreviousContainer(containerA);
		updateFieldChange.setNextSchemaContainer(containerB);
		containerA.setNextVersion(containerB);

		// create a node based on the old schema
		User user = user();
		Language english = english();
		Node parentNode = folder("2015");
		Node firstNode = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer firstEnglishContainer = firstNode.getOrCreateGraphFieldContainer(english);
		firstEnglishContainer.createString(fieldName).setString("first content");

		Node secondNode = parentNode.create(user, containerA, project());
		NodeGraphFieldContainer secondEnglishContainer = secondNode.getOrCreateGraphFieldContainer(english);
		secondEnglishContainer.createString(fieldName).setString("second content");

		DeliveryOptions options = new DeliveryOptions();
		options.addHeader(NodeMigrationVerticle.SCHEMA_UUID_HEADER, containerA.getUuid());
		CompletableFuture<AsyncResult<Message<Object>>> future = new CompletableFuture<>();
		vertx.eventBus().send(NodeMigrationVerticle.MIGRATION_ADDRESS, null, options, (rh) -> {
			future.complete(rh);
		});

		AsyncResult<Message<Object>> result = future.get(10, TimeUnit.SECONDS);
		if (result.cause() != null) {
			throw result.cause();
		}

		// assert that migration worked
		firstNode.reload();
		firstNode.getGraphFieldContainer("en").reload();
		assertThat(firstNode).as("Migrated Node").isOf(containerB).hasTranslation("en");
		assertThat(firstNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
				.isEqualTo("modified first content");
		secondNode.reload();
		secondNode.getGraphFieldContainer("en").reload();
		assertThat(secondNode).as("Migrated Node").isOf(containerB).hasTranslation("en");
		assertThat(secondNode.getGraphFieldContainer("en").getString(fieldName).getString()).as("Migrated field value")
				.isEqualTo("modified second content");
	}
}
