package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import org.apache.ivy.util.FileUtil;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;

import io.vertx.core.Future;

public class SchemaChangesVerticleTest extends AbstractChangesVerticleTest {

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
		Schema request = schema.getSchema();

		// Update name to folder to create a conflict
		request.setName(name);

		Future<GenericMessageResponse> future = getClient().updateSchema(schema.getUuid(), request);
		latchFor(future);
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

		CountDownLatch latch = latchForMigrationCompleted();
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
	public void testRemoveSegmentField() throws Exception {
		Node node = content();
		assertNotNull("The node should have been created.", node);
		assertNotNull("The node should have a filename string graph field", node.getGraphFieldContainer("en").getString("filename"));

		// 1. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("filename");
		listOfChanges.getChanges().add(change);

		// 2. Invoke migration
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		expectException(future, BAD_REQUEST, "schema_error_segmentfield_invalid", "filename");

		// 3. Assert migrated node
		node.reload();
		NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
		fieldContainer.reload();
		assertNull("The node should still have a filename string graph field", fieldContainer.getHtml("filename"));
	}

	@Test
	public void testRemoveField() throws Exception {

		Node node = content();
		assertNotNull("The node should have been created.", node);
		assertNotNull("The node should have a html graph field", node.getGraphFieldContainer("en").getHtml("content"));

		// 1. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("content");
		listOfChanges.getChanges().add(change);

		// 2. Setup eventbus bridged latch
		CountDownLatch latch = latchForMigrationCompleted();

		// 3. Invoke migration
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);

		// 6. Wait for migration to finish
		failingLatch(latch);
		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());

		// 7. Assert migrated node
		node.reload();
		NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
		fieldContainer.reload();
		assertNull("The node should no longer have a content html graph field", fieldContainer.getHtml("content"));
	}

	@Test
	public void testAddField() throws Exception {

		// 1. Setup changes
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddChange("newField", "html");
		listOfChanges.getChanges().add(change);

		// 2. Setup eventbus bridged latch
		CountDownLatch latch = latchForMigrationCompleted();

		// 3. Invoke migration
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "migration_invoked", "content");

		// 4. Latch for completion
		failingLatch(latch);
		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());
		assertNotNull("The container should now have a new version", container.getNextVersion());

		// Assert that migration worked
		Node node = content();
		node.reload();
		assertNotNull("The schema of the node should contain the new field schema", node.getSchemaContainer().getSchema().getField("newField"));
		assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				container.getVersion() != node.getSchemaContainer().getVersion());

	}
}
