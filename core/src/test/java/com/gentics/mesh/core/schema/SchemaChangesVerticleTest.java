package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.apache.ivy.util.FileUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;
import com.gentics.mesh.core.verticle.admin.AdminVerticle;
import com.gentics.mesh.core.verticle.eventbus.EventbusVerticle;
import com.gentics.mesh.core.verticle.node.NodeMigrationVerticle;
import com.gentics.mesh.core.verticle.schema.SchemaVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;

public class SchemaChangesVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private SchemaVerticle schemaVerticle;

	@Autowired
	private EventbusVerticle eventbusVerticle;

	@Autowired
	private NodeMigrationVerticle nodeMigrationVerticle;

	@Autowired
	private AdminVerticle adminVerticle;

	@Override
	@Before
	public void setupVerticleTest() throws Exception {
		super.setupVerticleTest();
		DeploymentOptions options = new DeploymentOptions();
		options.setWorker(true);
		vertx.deployVerticle(nodeMigrationVerticle, options);
	}

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(schemaVerticle);
		list.add(eventbusVerticle);
		list.add(adminVerticle);
		return list;
	}

	@Test
	public void testBlockingMigrationStatus() throws InterruptedException, IOException {
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());

		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createChangeFieldTypeChange("content", "boolean");

		String blockingScript = FileUtil.readEntirely(getClass().getResourceAsStream("/testscripts/longMigrate.js"));
		change.setMigrationScript(blockingScript);
		listOfChanges.getChanges().add(change);

		// Assert migration is in idle
		Future<GenericMessageResponse> statusFuture = getClient().schemaMigrationStatus();
		latchFor(statusFuture);
		expectResponseMessage(statusFuture, "migration_status_idle");

		// Trigger migration
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "migration_invoked", "content");

		// Wait a few seconds until the migration has started
		Thread.sleep(3000);

		// Assert migration is running
		statusFuture = getClient().schemaMigrationStatus();
		latchFor(statusFuture);
		expectResponseMessage(statusFuture, "migration_status_running");

		Thread.sleep(10000);

		// Assert migration has finished
		statusFuture = getClient().schemaMigrationStatus();
		latchFor(statusFuture);
		expectResponseMessage(statusFuture, "migration_status_idle");

	}

	@Test
	public void testUpdateWithConflictingName() {
		String name = "folder";
		String originalSchemaName = "content";
		SchemaContainer schema = schemaContainer(originalSchemaName);
		Schema request = new SchemaImpl();
		request.setName(name);

		Future<GenericMessageResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
		assertSuccess(future);

		//TODO Assert conflict
		expectException(future, CONFLICT, "schema_conflicting_name", name);
		schema.reload();
		assertEquals("The name of the schema was updated", originalSchemaName, schema.getName());
	}

	@Test
	public void testFieldTypeChange() throws Exception {
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());

		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createChangeFieldTypeChange("content", "boolean");
		listOfChanges.getChanges().add(change);

		// Construct latch in order to wait until the migration completed event was received 
		CountDownLatch latch = new CountDownLatch(1);
		getClient().eventbus(ws -> {
			JsonObject msg = new JsonObject().put("type", "register").put("address", "mesh.schema.migration");
			ws.writeFinalTextFrame(msg.encode());

			ws.handler(buff -> {
				String str = buff.toString();
				JsonObject received = new JsonObject(str);
				JsonObject rec = received.getJsonObject("body");
				System.out.println("Handler:" + rec.getString("type"));
				latch.countDown();
			});

		});

		// Trigger migration
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "migration_invoked", "content");

		// Wait for event
		failingLatch(latch);

		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());
		assertNotNull("The container should now have a new version", container.getNextVersion());

		// Assert that migration worked
		Node node = content();
		node.reload();
		node.getGraphFieldContainer("en").reload();
		assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				container.getVersion() != node.getSchemaContainer().getVersion());
		assertNull("There should no longer be a content field of type html", node.getGraphFieldContainer("en").getHtml("content"));

	}

	@Test
	public void testAddField() {
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddChange("newField", "html");
		listOfChanges.getChanges().add(change);

		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "migration_invoked", "content");
		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());
		assertNotNull("The container should now have a new version", container.getNextVersion());

		//		// Assert that migration worked
		//		Node node = content();
		//		node.reload();
		//		assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
		//				container.getVersion() != node.getSchemaContainer().getVersion());

	}
}
