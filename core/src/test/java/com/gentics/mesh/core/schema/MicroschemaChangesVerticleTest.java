package com.gentics.mesh.core.schema;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.Schema;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.performance.TestUtils;

public class MicroschemaChangesVerticleTest extends AbstractChangesVerticleTest {

	@Test
	public void testRemoveField() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. Setup eventbus bridge latch
			CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

			// 2. Create node that uses the microschema
			Node node = createMicronodeNode();
			MicroschemaContainer container = microschemaContainer("vcard");
			MicroschemaContainerVersion currentVersion = container.getLatestVersion();
			assertNull("The microschema should not yet have any changes", container.getLatestVersion().getNextChange());

			// 3. Create changes
			SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
			SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("firstName");
			listOfChanges.getChanges().add(change);

			// 4. Invoke migration
			assertNull("The schema should not yet have any changes", container.getLatestVersion().getNextChange());
			call(() -> getClient().applyChangesToMicroschema(container.getUuid(), listOfChanges));
			Microschema microschema = call(() -> getClient().findMicroschemaByUuid(container.getUuid()));
			call(() -> getClient().assignReleaseMicroschemaVersions(project().getName(), project().getLatestRelease().getUuid(),
					new MicroschemaReference().setName(microschema.getName()).setVersion(microschema.getVersion())));

			// 5. Wait for migration to finish
			failingLatch(latch);

			container.reload();
			currentVersion.reload();
			assertNotNull("The change should have been added to the schema.", currentVersion.getNextChange());

			// 6. Assert migrated node
			node.reload();
			NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
			fieldContainer.reload();
			assertNotNull("The node should have a micronode graph field", fieldContainer.getMicronode("micronodeField"));
		}
	}

	private Node createMicronodeNode() {

		// 1. Update folder schema
		Schema schema = schemaContainer("folder").getLatestVersion().getSchema();
		MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
		microschemaFieldSchema.setName("micronodeField");
		microschemaFieldSchema.setLabel("Some label");
		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);

		// 2. Create node with vcard micronode
		MicronodeResponse micronode = new MicronodeResponse();
		MicroschemaReference ref = new MicroschemaReference();
		ref.setName("vcard");
		micronode.setMicroschema(ref);
		micronode.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		micronode.getFields().put("lastName", new StringFieldImpl().setString("Mustermann"));
		NodeResponse response = createNode("micronodeField", micronode);
		Node node = MeshRoot.getInstance().getNodeRoot().findByUuid(response.getUuid()).toBlocking().value();
		assertNotNull("The node should have been created.", node);
		assertNotNull("The node should have a micronode graph field", node.getGraphFieldContainer("en").getMicronode("micronodeField"));

		return node;

	}

	@Test
	public void testAddField() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// 1. Setup changes
			MicroschemaContainer container = microschemaContainer("vcard");
			MicroschemaContainerVersion currentVersion = container.getLatestVersion();
			assertNull("The microschema should not yet have any changes", currentVersion.getNextChange());
			SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
			SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField", "html", "fieldLabel");
			listOfChanges.getChanges().add(change);

			// 2. Setup eventbus bridged latch
			CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

			// 3. Invoke migration
			call(() -> getClient().applyChangesToMicroschema(container.getUuid(), listOfChanges));
			Microschema microschema = call(() -> getClient().findMicroschemaByUuid(container.getUuid()));
			call(() -> getClient().assignReleaseMicroschemaVersions(project().getName(), project().getLatestRelease().getUuid(),
					new MicroschemaReference().setName(microschema.getName()).setVersion(microschema.getVersion())));

			// 4. Latch for completion
			failingLatch(latch);
			container.reload();
			currentVersion.reload();
			assertNotNull("The change should have been added to the schema.", currentVersion.getNextChange());
			assertNotNull("The container should now have a new version", currentVersion.getNextVersion());
		}
	}

	@Test
	public void testUpdateName() throws Exception {
		try (NoTx noTx = db.noTx()) {
			String name = "new-name";
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			MicroschemaContainerVersion currentVersion = vcardContainer.getLatestVersion();
			assertNotNull(vcardContainer);

			// 1. Setup new microschema
			Microschema request = new MicroschemaModel();
			request.setName(name);

			// 2. Setup eventbus bridged latch
			CountDownLatch latch = TestUtils.latchForMigrationCompleted(getClient());

			// 3. Invoke migration
			call(() -> getClient().updateMicroschema(vcardContainer.getUuid(), request));
			Microschema microschema = call(() -> getClient().findMicroschemaByUuid(vcardContainer.getUuid()));
			call(() -> getClient().assignReleaseMicroschemaVersions(project().getName(), project().getLatestRelease().getUuid(),
					new MicroschemaReference().setName(microschema.getName()).setVersion(microschema.getVersion())));

			// 4. Wait and assert
			failingLatch(latch);
			vcardContainer.reload();
			currentVersion.reload();
			assertEquals("The name of the microschema was not updated", name, currentVersion.getNextVersion().getName());
		}
	}

	@Test
	public void testUpdateWithConflictingName() {
		try (NoTx noTx = db.noTx()) {
			String name = "captionedImage";
			String originalSchemaName = "vcard";
			MicroschemaContainer microschema = microschemaContainers().get(originalSchemaName);
			assertNotNull(microschema);
			Microschema request = new MicroschemaModel();
			request.setName(name);

			call(() -> getClient().updateMicroschema(microschema.getUuid(), request), CONFLICT, "schema_conflicting_name", name);
			microschema.reload();
			assertEquals("The name of the microschema was updated but it should not.", originalSchemaName, microschema.getName());
		}
	}

}
