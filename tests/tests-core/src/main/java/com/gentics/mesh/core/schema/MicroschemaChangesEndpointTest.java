package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_MIGRATION_START;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_UPDATED;
import static com.gentics.mesh.core.rest.job.JobStatus.COMPLETED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.schema.HibMicroschemaVersion;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaVersionModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;

@MeshTestSetting(testSize = FULL, startServer = true)
public class MicroschemaChangesEndpointTest extends AbstractMeshTest {

	@Before
	public void addAdminPerms() {
		// Grant admin perms. Otherwise we can't check the jobs
		grantAdmin();
	}

	@Test
	public void testRemoveField() throws Exception {
		// 1. Create node that uses the microschema
		HibMicroschema microschemaContainer = microschemaContainer("vcard");
		HibNode node = createMicronodeNode();
		HibMicroschemaVersion beforeVersion = tx(() -> microschemaContainer.getLatestVersion());
		assertNull("The microschema should not yet have any changes", tx(() -> microschemaContainer.getLatestVersion().getNextChange()));

		String microschemaUuid = tx(() -> microschemaContainer.getUuid());
		// 2. Create changes
		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createRemoveFieldChange("firstName");
		listOfChanges.getChanges().add(change);

		try (Tx tx = tx()) {
			assertNull("The schema should not yet have any changes", microschemaContainer.getLatestVersion().getNextChange());
		}

		// 3. Invoke migration
		call(() -> client().applyChangesToMicroschema(microschemaUuid, listOfChanges));
		MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(microschemaUuid));
		waitForJobs(() -> {
			call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(),
				new MicroschemaReferenceImpl().setName(microschema.getName()).setVersion(microschema.getVersion())));
		}, COMPLETED, 1);

		// 4. Assert migrated node
		try (Tx tx = tx()) {
			HibMicroschemaVersion reloaded = CommonTx.get().load(beforeVersion.getId(), tx().<CommonTx>unwrap().microschemaDao().getVersionPersistenceClass());
			assertNotNull("The change should have been added to the schema.", reloaded.getNextChange());
			HibNodeFieldContainer fieldContainer = tx.contentDao().getFieldContainer(node, "en");
			assertNotNull("The node should have a micronode graph field", fieldContainer.getMicronode("micronodeField"));
		}
	}

	@Test
	public void testAddField() throws Exception {
		HibMicroschema microschemaContainer = microschemaContainer("vcard");

		// 1. Setup changes
		HibMicroschemaVersion beforeVersion;
		try (Tx tx = tx()) {
			beforeVersion = microschemaContainer.getLatestVersion();
			assertNull("The microschema should not yet have any changes", beforeVersion.getNextChange());
		}
		String microschemaUuid = tx(() -> microschemaContainer.getUuid());

		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField", "html", "fieldLabel", new JsonObject().put("test", "test"));
		listOfChanges.getChanges().add(change);

		// 2. Invoke migration
		waitForLatestJob(() -> {
			call(() -> client().applyChangesToMicroschema(microschemaUuid, listOfChanges));
			MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(microschemaUuid));
			call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(),
				new MicroschemaReferenceImpl().setName(microschema.getName()).setVersion(microschema.getVersion())));
		});

		try (Tx tx = tx()) {
			HibMicroschemaVersion reloaded = CommonTx.get().load(beforeVersion.getId(), tx().<CommonTx>unwrap().microschemaDao().getVersionPersistenceClass());
			assertNotNull("The change should have been added to the schema.", reloaded.getNextChange());
			assertNotNull("The container should now have a new version", reloaded.getNextVersion());
		}

	}

	@Test
	public void testUpdateName() throws Exception {
		final String newName = "new_name";

		String vcardUuid = tx(() -> microschemaContainers().get("vcard").getUuid());
		HibMicroschemaVersion beforeVersion = tx(() -> data().getMicroschemaContainer("vcard").getLatestVersion());

		expect(MICROSCHEMA_UPDATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(newName).hasUuid(vcardUuid);
		}).one();
		expect(MICROSCHEMA_MIGRATION_START).none();
		MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
		request.setName(newName);
		call(() -> client().updateMicroschema(vcardUuid, request, new SchemaUpdateParametersImpl().setUpdateAssignedBranches(false)));
		MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(vcardUuid));
		awaitEvents();

		// Invoke migration
		waitForJobs(() -> {
			call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(),
				new MicroschemaReferenceImpl().setName(microschema.getName()).setVersion(microschema.getVersion())));
		}, COMPLETED, 1);

		try (Tx tx = tx()) {
			HibMicroschemaVersion reloaded = CommonTx.get().load(beforeVersion.getId(), tx().<CommonTx>unwrap().microschemaDao().getVersionPersistenceClass());
			assertEquals("The name of the microschema was not updated", newName, reloaded.getNextVersion().getName());
		}
	}

	@Test
	public void testUpdateWithConflictingName() {
		try (Tx tx = tx()) {
			String name = "captionedImage";
			String originalSchemaName = "vcard";
			HibMicroschema microschema = microschemaContainers().get(originalSchemaName);
			assertNotNull(microschema);
			MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
			request.setName(name);

			call(() -> client().updateMicroschema(microschema.getUuid(), request), CONFLICT, "schema_conflicting_name", name);
			assertEquals("The name of the microschema was updated but it should not.", originalSchemaName, microschema.getName());
		}
	}

	private HibNode createMicronodeNode() {

		// 1. Update folder schema
		tx(() -> {
			SchemaVersionModel schema = tx(() -> schemaContainer("folder").getLatestVersion().getSchema());
			MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
			microschemaFieldSchema.setName("micronodeField");
			microschemaFieldSchema.setLabel("Some label");
			microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
			schema.addField(microschemaFieldSchema);
			schemaContainer("folder").getLatestVersion().setSchema(schema);
		});

		// 2. Create node with vcard micronode
		MicronodeResponse micronode = new MicronodeResponse();
		MicroschemaReferenceImpl ref = new MicroschemaReferenceImpl();
		ref.setName("vcard");
		micronode.setMicroschema(ref);
		micronode.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		micronode.getFields().put("lastName", new StringFieldImpl().setString("Mustermann"));
		NodeResponse response = createNode("micronodeField", micronode);

		return tx(tx -> {
			HibNode node = tx.nodeDao().findByUuid(project(), response.getUuid());
			assertNotNull("The node should have been created.", node);
			assertNotNull("The node should have a micronode graph field", tx.contentDao().getFieldContainer(node, "en").getMicronode("micronodeField"));
			return node;
		});
	}

}
