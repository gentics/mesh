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

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.NodeGraphFieldContainer;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.data.schema.MicroschemaContainerVersion;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.core.rest.schema.MicronodeFieldSchema;
import com.gentics.mesh.core.rest.schema.SchemaModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangeModel;
import com.gentics.mesh.core.rest.schema.change.impl.SchemaChangesListModel;
import com.gentics.mesh.core.rest.schema.impl.MicronodeFieldSchemaImpl;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.parameter.client.SchemaUpdateParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(testSize = FULL, startServer = true)
public class MicroschemaChangesEndpointTest extends AbstractMeshTest {

	@Before
	public void addAdminPerms() {
		// Grant admin perms. Otherwise we can't check the jobs
		tx(() -> group().addRole(roles().get("admin")));
	}

	@Test
	public void testRemoveField() throws Exception {
		// 1. Create node that uses the microschema
		Node node;
		MicroschemaContainer microschemaContainer = microschemaContainer("vcard");
		MicroschemaContainerVersion beforeVersion;
		try (Tx tx = tx()) {
			node = createMicronodeNode();
			beforeVersion = microschemaContainer.getLatestVersion();
			assertNull("The microschema should not yet have any changes", microschemaContainer.getLatestVersion().getNextChange());
			tx.success();
		}
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
			assertNotNull("The change should have been added to the schema.", beforeVersion.getNextChange());
			NodeGraphFieldContainer fieldContainer = node.getGraphFieldContainer("en");
			assertNotNull("The node should have a micronode graph field", fieldContainer.getMicronode("micronodeField"));
		}
	}

	@Test
	public void testAddField() throws Exception {
		MicroschemaContainer microschemaContainer = microschemaContainer("vcard");

		// 1. Setup changes
		MicroschemaContainerVersion beforeVersion;
		try (Tx tx = tx()) {
			beforeVersion = microschemaContainer.getLatestVersion();
			assertNull("The microschema should not yet have any changes", beforeVersion.getNextChange());
		}
		String microschemaUuid = tx(() -> microschemaContainer.getUuid());

		SchemaChangesListModel listOfChanges = new SchemaChangesListModel();
		SchemaChangeModel change = SchemaChangeModel.createAddFieldChange("newField", "html", "fieldLabel");
		listOfChanges.getChanges().add(change);

		// 2. Invoke migration
		waitForLatestJob(() -> {
			call(() -> client().applyChangesToMicroschema(microschemaUuid, listOfChanges));
			MicroschemaResponse microschema = call(() -> client().findMicroschemaByUuid(microschemaUuid));
			call(() -> client().assignBranchMicroschemaVersions(PROJECT_NAME, initialBranchUuid(),
				new MicroschemaReferenceImpl().setName(microschema.getName()).setVersion(microschema.getVersion())));
		});

		try (Tx tx = tx()) {
			assertNotNull("The change should have been added to the schema.", beforeVersion.getNextChange());
			assertNotNull("The container should now have a new version", beforeVersion.getNextVersion());
		}

	}

	@Test
	public void testUpdateName() throws Exception {
		final String newName = "new_name";

		String vcardUuid = tx(() -> microschemaContainers().get("vcard").getUuid());
		MicroschemaContainerVersion beforeVersion = tx(() -> microschemaContainers().get("vcard").getLatestVersion());

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
			assertEquals("The name of the microschema was not updated", newName, beforeVersion.getNextVersion().getName());
		}
	}

	@Test
	public void testUpdateWithConflictingName() {
		try (Tx tx = tx()) {
			String name = "captionedImage";
			String originalSchemaName = "vcard";
			MicroschemaContainer microschema = microschemaContainers().get(originalSchemaName);
			assertNotNull(microschema);
			MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
			request.setName(name);

			call(() -> client().updateMicroschema(microschema.getUuid(), request), CONFLICT, "schema_conflicting_name", name);
			assertEquals("The name of the microschema was updated but it should not.", originalSchemaName, microschema.getName());
		}
	}

	private Node createMicronodeNode() {

		// 1. Update folder schema
		SchemaModel schema = schemaContainer("folder").getLatestVersion().getSchema();
		MicronodeFieldSchema microschemaFieldSchema = new MicronodeFieldSchemaImpl();
		microschemaFieldSchema.setName("micronodeField");
		microschemaFieldSchema.setLabel("Some label");
		microschemaFieldSchema.setAllowedMicroSchemas(new String[] { "vcard" });
		schema.addField(microschemaFieldSchema);
		schemaContainer("folder").getLatestVersion().setSchema(schema);

		// 2. Create node with vcard micronode
		MicronodeResponse micronode = new MicronodeResponse();
		MicroschemaReferenceImpl ref = new MicroschemaReferenceImpl();
		ref.setName("vcard");
		micronode.setMicroschema(ref);
		micronode.getFields().put("firstName", new StringFieldImpl().setString("Max"));
		micronode.getFields().put("lastName", new StringFieldImpl().setString("Mustermann"));
		NodeResponse response = createNode("micronodeField", micronode);
		Node node = MeshInternal.get().boot().meshRoot().getNodeRoot().findByUuid(response.getUuid());
		assertNotNull("The node should have been created.", node);
		assertNotNull("The node should have a micronode graph field", node.getGraphFieldContainer("en").getMicronode("micronodeField"));

		return node;

	}

}
