package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;

import io.vertx.core.Future;

public class MicroschemaChangesVerticleTest extends AbstractChangesVerticleTest {

	@Test
	public void testRemoveField() throws Exception {

		// 1. Create node that uses the microschema
		Node node = createMicronodeNode();

		// 2. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("firstName");
		listOfChanges.getChanges().add(change);

		// 3. Setup eventbus bridged latch
		CountDownLatch latch = latchForMigrationCompleted();

		// 4. Invoke migration
		MicroschemaContainer container = microschemaContainer("vcard");
		assertNull("The microschema should not yet have any changes", container.getNextChange());
		Future<GenericMessageResponse> future = getClient().applyChangesToMicroschema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());

		// 5. Wait for migration to finish
		failingLatch(latch);

		// 6. Assert migrated node
		node.reload();
		NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
		fieldContainer.reload();
		assertNotNull("The node should have a micronode graph field", fieldContainer.getMicronode("micronodeField"));
	}

	private Node createMicronodeNode() {

		// 1. Update folder schema
		Schema schema = schemaContainer("folder").getSchema();
		MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
		microschemaFieldSchema.setName("micronodeField");
		microschemaFieldSchema.setLabel("Some label");
		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").setSchema(schema);

		//		getClient().getClientSchemaStorage().clear();
		//		ServerSchemaStorage.getInstance().clear();

		// 2. Create node with vcard micronode
		MicronodeResponse micronode = new MicronodeResponse();
		MicroschemaReference ref = new MicroschemaReference();
		ref.setName("vcard");
		micronode.setMicroschema(ref);
		micronode.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		micronode.getFields().put("lastName", new StringFieldImpl().setString("Mustermann"));
		NodeResponse response = createNode("micronodeField", micronode);
		Node node = MeshRoot.getInstance().getNodeRoot().findByUuid(response.getUuid()).toBlocking().single();
		assertNotNull("The node should have been created.", node);
		assertNotNull("The node should have a micronode graph field", node.getGraphFieldContainer("en").getMicronode("micronodeField"));

		return node;

	}

	@Test
	public void testAddField() throws Exception {
		// 1. Setup changes
		MicroschemaContainer container = microschemaContainer("vcard");
		assertNull("The microschema should not yet have any changes", container.getNextChange());
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField", "html");
		listOfChanges.getChanges().add(change);

		// 2. Setup eventbus bridged latch
		CountDownLatch latch = latchForMigrationCompleted();

		// 3. Invoke migration
		Future<GenericMessageResponse> future = getClient().applyChangesToMicroschema(container.getUuid(), listOfChanges);
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "migration_invoked", "vcard");

		// 4. Latch for completion
		failingLatch(latch);
		container.reload();
		assertNotNull("The change should have been added to the schema.", container.getNextChange());
		assertNotNull("The container should now have a new version", container.getNextVersion());

	}

	@Test
	public void testUpdateName() throws Exception {
		String name = "new-name";
		MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
		assertNotNull(vcardContainer);

		Microschema request = new MicroschemaImpl();
		request.setName(name);

		Future<GenericMessageResponse> future = getClient().updateMicroschema(vcardContainer.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		expectFailureMessage(future, OK, "blub");

		//		
		//		assertEquals(request.getName(), restSchema.getName());
		//		vcardContainer.reload();
		//		assertEquals("The name of the microschema was not updated", name, vcardContainer.getName());
		//		MicroschemaContainer reloaded = boot.microschemaContainerRoot().findByUuid(vcardContainer.getUuid()).toBlocking().single();
		//		assertEquals("The name should have been updated", name, reloaded.getName());

	}

	@Test
	public void testUpdateWithConflictingName() {
		String name = "captionedImage";
		String originalSchemaName = "vcard";
		MicroschemaContainer microschema = microschemaContainers().get(originalSchemaName);
		assertNotNull(microschema);
		Microschema request = new MicroschemaImpl();
		request.setName(name);

		Future<GenericMessageResponse> future = getClient().updateMicroschema(microschema.getUuid(), request);
		latchFor(future);
		expectException(future, CONFLICT, "schema_conflicting_name", name);
		microschema.reload();
		assertEquals("The name of the microschema was updated but it should not.", originalSchemaName, microschema.getName());
	}

}
