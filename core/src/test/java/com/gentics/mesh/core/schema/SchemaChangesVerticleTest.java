package com.gentics.mesh.core.schema;

import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
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

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.data.service.ServerSchemaStorage;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.NodeUpdateRequest;
import com.gentics.mesh.core.rest.node.field.impl.NumberFieldImpl;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.test.TestUtils;
import com.gentics.mesh.util.FieldUtil;

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

		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());
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
		container.reload();
		assertTrue("The version of the original schema and the schema that is now linked to the node should be different.",
				container.getVersion() != node.getSchemaContainer().getVersion());
		assertNull("There should no longer be a content field of type html", node.getGraphFieldContainer("en").getHtml("content"));

	}

	@Test
	public void testRemoveAddFieldTypeWithSameKey() throws Exception {

		Node content = content();

		// 1. Remove title field from schema and add new title with different type
		SchemaContainer container = schemaContainer("content");
		Schema schema = container.getSchema();
		schema.removeField("content");
		schema.addField(FieldUtil.createNumberFieldSchema("content"));
		container.setSchema(schema);

		// 2. Update the schema client side
		getClient().getClientSchemaStorage().removeSchema("content");
		getClient().getClientSchemaStorage().addSchema(schema);

		// 3. Setup eventbus bridged latch
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

		// 4. Update the schema server side
		Future<GenericMessageResponse> future = getClient().updateSchema(container.getUuid(), schema);
		latchFor(future);
		assertSuccess(future);
		failingLatch(latch);

		// 5. Read node and check additional field
		Future<NodeResponse> nodeFuture = getClient().findNodeByUuid(PROJECT_NAME, content.getUuid());
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		NodeResponse response = nodeFuture.result();
		assertNotNull("The response should contain the content field.", response.getField("content"));
		assertEquals("The type of the content field was not changed to a number field.", NumberFieldImpl.class,
				response.getField("content").getClass());

		// 6. Update the node and set the new field
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setSchema(new SchemaReference().setName("content"));
		nodeUpdateRequest.getFields().put("content", new NumberFieldImpl().setNumber(42.01));
		nodeFuture = getClient().updateNode(PROJECT_NAME, content.getUuid(), nodeUpdateRequest);
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("content"));
		assertEquals(42.01, ((NumberFieldImpl) response.getField("content")).getNumber());

	}

	@Test
	public void testApplyWithEmptyChangesList() {
		SchemaContainer container = schemaContainer("content");
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		expectException(future, BAD_REQUEST, "schema_migration_no_changes_specified");
	}

	@Test
	public void testUnsetSegmentField() throws JsonParseException, JsonMappingException, IOException {

		// 1. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createUpdateSchemaChange();
		change.setProperty(SchemaChangeModel.SEGMENT_FIELD_KEY, null);
		listOfChanges.getChanges().add(change);

		// 2. Invoke migration
		SchemaContainer container = schemaContainer("content");
		assertNull("The schema should not yet have any changes", container.getNextChange());
		Future<GenericMessageResponse> future = getClient().applyChangesToSchema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);

		// 3. Assert updated schema
		container.reload();
		assertNull("The segment field reference should have been set to null", container.getNextVersion().getSchema().getSegmentField());
	}

	@Test
	public void testRemoveSegmentField() throws Exception {
		Node node = content();
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
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

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
		SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField", "html");
		listOfChanges.getChanges().add(change);

		// 2. Setup eventbus bridged latch
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

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

	@Test
	public void testUpdateAddField() throws Exception {

		Node content = content();

		SchemaContainer container = schemaContainer("content");
		Schema schema = container.getSchema();
		schema.getFields().add(FieldUtil.createStringFieldSchema("extraname"));
		container.setSchema(schema);

		// Update the schema client side
		getClient().getClientSchemaStorage().removeSchema("content");
		getClient().getClientSchemaStorage().addSchema(schema);

		// Update the schema server side
		Future<GenericMessageResponse> future = getClient().updateSchema(container.getUuid(), schema);
		latchFor(future);
		assertSuccess(future);

		// Read node and check additional field
		Future<NodeResponse> nodeFuture = getClient().findNodeByUuid(PROJECT_NAME, content.getUuid());
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		NodeResponse response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("extraname"));

		// Update the node and set the new field
		NodeUpdateRequest nodeUpdateRequest = new NodeUpdateRequest();
		nodeUpdateRequest.setLanguage("en");
		nodeUpdateRequest.setSchema(new SchemaReference().setName("content"));
		nodeUpdateRequest.getFields().put("extraname", new StringFieldImpl().setString("sometext"));
		nodeFuture = getClient().updateNode(PROJECT_NAME, content.getUuid(), nodeUpdateRequest);
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		response = nodeFuture.result();
		assertNotNull(response);
		assertNotNull(response.getField("extraname"));
		assertEquals("sometext", ((StringFieldImpl) response.getField("extraname")).getString());

	}

	@Test
	public void testRemoveField2() throws Exception {
		Node content = content();

		SchemaContainer container = schemaContainer("content");
		Schema schema = container.getSchema();
		schema.removeField("content");
		container.setSchema(schema);

		// Update the schema client side
		getClient().getClientSchemaStorage().removeSchema("content");
		getClient().getClientSchemaStorage().addSchema(schema);
		ServerSchemaStorage.getInstance().clear();

		// 2. Setup eventbus bridged latch
		CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

		// Update the schema server side
		Future<GenericMessageResponse> future = getClient().updateSchema(container.getUuid(), schema);
		latchFor(future);
		assertSuccess(future);

		failingLatch(latch);

		// Read node and check additional field
		Future<NodeResponse> nodeFuture = getClient().findNodeByUuid(PROJECT_NAME, content.getUuid());
		latchFor(nodeFuture);
		assertSuccess(nodeFuture);
		NodeResponse response = nodeFuture.result();
		assertNotNull(response);
		assertNull(response.getField("content"));

	}
}
