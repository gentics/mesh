package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.MICROSCHEMA_DELETED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.ListFieldSchema;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.syncleus.ferma.tx.Tx;

import io.reactivex.Observable;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class MicroschemaEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testUpdateMultithreaded() throws Exception {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		try (Tx tx = tx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			String uuid = vcardContainer.getUuid();

			Observable.range(0, nJobs)
				.flatMapCompletable(i -> client().findMicroschemaByUuid(uuid).toCompletable())
				.blockingAwait();
		}
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() throws Exception {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;

		Observable.range(0, nJobs)
			.flatMapCompletable(i -> {
				MicroschemaCreateRequest request = new MicroschemaCreateRequest();
				request.setName("new_microschema_name" + i);
				return client().createMicroschema(request).toCompletable();
			}).blockingAwait();
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (Tx tx = tx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			String uuid = vcardContainer.getUuid();

			Observable.range(0, nJobs)
				.flatMapCompletable(i -> client().findMicroschemaByUuid(uuid).toCompletable())
				.blockingAwait();
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName("new_microschema_name");
		request.setDescription("microschema description");

		expect(MICROSCHEMA_CREATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("new_microschema_name").uuidNotNull();
			return true;
		});

		assertThat(trackingSearchProvider()).recordedStoreEvents(0);
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(request));
		awaitEvents();

		assertThat(trackingSearchProvider()).recordedStoreEvents(1);
		assertThat(microschemaResponse.getPermissions()).hasPerm(READ, CREATE, DELETE, UPDATE);
		assertThat((Microschema) microschemaResponse).isEqualToComparingOnlyGivenFields(request, "name", "description");
	}

	@Test
	public void testCreateWithConflictingName() {
		String name = "new_microschema_name";
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.getFields().add(FieldUtil.createStringFieldSchema("name").setRequired(true));
		request.setName(name);
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(request));
		assertEquals("The microschema with the given name should haven been created.", microschemaResponse.getName(), name);

		// Create the same schema again
		call(() -> client().createMicroschema(request), CONFLICT, "microschema_conflicting_name", name);
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName("new_microschema_name");
		request.setDescription("microschema description");

		String microschemaRootUuid = db().tx(() -> meshRoot().getMicroschemaContainerRoot().getUuid());
		try (Tx tx = tx()) {
			role().revokePermissions(meshRoot().getMicroschemaContainerRoot(), CREATE_PERM);
			tx.success();
		}
		call(() -> client().createMicroschema(request), FORBIDDEN, "error_missing_perm", microschemaRootUuid, CREATE_PERM.getRestPerm().getName());

	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testCreateWithUuid() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testCreateWithDuplicateUuid() throws Exception {
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testCreateReadDelete() throws Exception {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (Tx tx = tx()) {
			MicroschemaContainer vcardContainer = microschemaContainer("vcard");
			assertNotNull(vcardContainer);
			MicroschemaResponse microschemaResponse = call(() -> client().findMicroschemaByUuid(vcardContainer.getUuid()));
			assertThat((Microschema) microschemaResponse).isEqualToComparingOnlyGivenFields(vcardContainer.getLatestVersion().getSchema(), "name",
				"description");
		}
	}

	@Test
	public void testReadVersion() {
		String uuid = tx(() -> microschemaContainer("vcard").getUuid());
		String latestVersion = tx(() -> microschemaContainer("vcard").getLatestVersion().getVersion());
		String json = tx(() -> microschemaContainer("vcard").getLatestVersion().getJson());

		// Load the latest version
		MicroschemaResponse restSchema = call(() -> client().findMicroschemaByUuid(uuid, new VersioningParametersImpl().setVersion(latestVersion)));
		assertEquals("The loaded version did not match up with the requested version.", latestVersion, restSchema.getVersion());

		// Now update the microschema
		MicroschemaUpdateRequest request = JsonUtil.readValue(json, MicroschemaUpdateRequest.class);
		request.setDescription("New description");
		request.addField(FieldUtil.createHtmlFieldSchema("someHtml"));
		call(() -> client().updateMicroschema(uuid, request));

		// Load the previous version
		restSchema = call(() -> client().findMicroschemaByUuid(uuid, new VersioningParametersImpl().setVersion(latestVersion)));
		assertEquals("The loaded version did not match up with the requested version.", latestVersion, restSchema.getVersion());

		// Load the latest version (2.0)
		restSchema = call(() -> client().findMicroschemaByUuid(uuid));
		assertEquals("The loaded version did not match up with the requested version.", "2.0", restSchema.getVersion());

		// Load the expected 2.0 version
		restSchema = call(() -> client().findMicroschemaByUuid(uuid, new VersioningParametersImpl().setVersion("2.0")));
		assertEquals("The loaded version did not match up with the requested version.", "2.0", restSchema.getVersion());
	}

	@Test
	public void testReadBogusVersion() {
		String uuid = tx(() -> microschemaContainer("vcard").getUuid());

		call(() -> client().findMicroschemaByUuid(uuid, new VersioningParametersImpl().setVersion("5.0")), NOT_FOUND,
			"object_not_found_for_uuid_version", uuid, "5.0");

		call(() -> client().findMicroschemaByUuid(uuid, new VersioningParametersImpl().setVersion("sadgsdgasgd")), BAD_REQUEST,
			"error_illegal_version", "sadgsdgasgd");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (Tx tx = tx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			String uuid = vcardContainer.getUuid();

			MicroschemaResponse microschema = call(
				() -> client().findMicroschemaByUuid(uuid, new RolePermissionParametersImpl().setRoleUuid(role().getUuid())));
			assertNotNull(microschema.getRolePerms());
			assertThat(microschema.getRolePerms()).hasPerm(Permission.values());
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			uuid = vcardContainer.getUuid();
			role().grantPermissions(vcardContainer, DELETE_PERM);
			role().grantPermissions(vcardContainer, UPDATE_PERM);
			role().grantPermissions(vcardContainer, CREATE_PERM);
			role().revokePermissions(vcardContainer, READ_PERM);
			tx.success();
		}
		call(() -> client().findMicroschemaByUuid(uuid), FORBIDDEN, "error_missing_perm", uuid, READ_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	@Ignore("Not yet implemented")
	public void testReadMultiple() throws Exception {
		fail("Not yet implemented");
	}

	@Test
	@Override
	@Ignore("Handled via microschema changes verticle test")
	public void testUpdate() throws Exception {

	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			MicroschemaContainer microschema = microschemaContainers().get("vcard");
			uuid = microschema.getUuid();
			role().revokePermissions(microschema, UPDATE_PERM);
			tx.success();
		}

		MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
		request.setName("new-name");
		call(() -> client().updateMicroschema(uuid, request), FORBIDDEN, "error_missing_perm", uuid, UPDATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		try (Tx tx = tx()) {
			MicroschemaContainer microschema = microschemaContainers().get("vcard");
			String oldName = microschema.getName();
			MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
			request.setName("new-name");

			call(() -> client().updateMicroschema("bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");

			MicroschemaContainer reloaded = boot().microschemaContainerRoot().findByUuid(microschema.getUuid());
			assertEquals("The name should not have been changed.", oldName, reloaded.getName());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String uuid = db().tx(() -> microschemaContainers().get("vcard").getUuid());

		expect(MICROSCHEMA_DELETED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("vcard").uuidNotNull();
			return true;
		}).total(1);

		call(() -> client().deleteMicroschema(uuid));

		awaitEvents();
		// schema_delete_still_in_use

		try (Tx tx = tx()) {
			MicroschemaContainer reloaded = boot().microschemaContainerRoot().findByUuid(uuid);
			assertNull("The microschema should have been deleted.", reloaded);
		}
	}

	/**
	 * Test delete of node with single micronode.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteByUUIDWhileInUse1() throws Exception {
		String baseUuid = tx(() -> project().getBaseNode().getUuid());

		// 1. Create a new schema which uses the vcard microschema
		SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();
		schema.addField(FieldUtil.createMicronodeFieldSchema("vcardtest").setAllowedMicroSchemas("vcard"));
		SchemaResponse response = call(() -> client().createSchema(schema));

		// 2. Assign the new schema to the project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));

		// 3. Create a new node which uses the schema
		NodeResponse nodeResponse1 = createNodeWithMicronode(baseUuid);
		NodeResponse nodeResponse2 = createNodeWithMicronode(baseUuid);
		assertDelete(nodeResponse1.getUuid(), nodeResponse2.getUuid());

	}

	private NodeResponse createNodeWithMicronode(String baseUuid) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");

		MicronodeResponse micronodeField = new MicronodeResponse();
		micronodeField.getFields().put("firstName", FieldUtil.createStringField("firstnameValue"));
		micronodeField.getFields().put("lastName", FieldUtil.createStringField("lastnameValue"));
		micronodeField.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
		nodeCreateRequest.getFields().put("vcardtest", micronodeField);
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("test"));
		nodeCreateRequest.setParentNodeUuid(baseUuid);
		return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
	}

	/**
	 * Test delete of node with micronode list.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testDeleteByUUIDWhileInUse2() throws Exception {
		String baseUuid = tx(() -> project().getBaseNode().getUuid());

		// 1. Create a new schema which uses the vcard microschema
		SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();
		ListFieldSchema fieldSchema = FieldUtil.createListFieldSchema("vcardslist", "micronode").setAllowedSchemas("vcard");
		schema.addField(fieldSchema);
		SchemaResponse response = call(() -> client().createSchema(schema));

		// 2. Assign the new schema to the project
		call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));

		// 3. Create a new node which uses the schema
		NodeResponse nodeResponse1 = createNodeWithMicronodeList(baseUuid);
		NodeResponse nodeResponse2 = createNodeWithMicronodeList(baseUuid);
		assertDelete(nodeResponse1.getUuid(), nodeResponse2.getUuid());

	}

	private NodeResponse createNodeWithMicronodeList(String baseUuid) {
		NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
		nodeCreateRequest.setLanguage("en");

		MicronodeFieldListImpl micronodeListField = new MicronodeFieldListImpl();
		for (int i = 0; i < 3; i++) {
			MicronodeResponse micronodeField = new MicronodeResponse();
			micronodeField.getFields().put("firstName", FieldUtil.createStringField("firstnameValue" + i));
			micronodeField.getFields().put("lastName", FieldUtil.createStringField("lastnameValue" + i));
			micronodeField.setMicroschema(new MicroschemaReferenceImpl().setName("vcard"));
			micronodeListField.add(micronodeField);
		}

		nodeCreateRequest.getFields().put("vcardslist", micronodeListField);
		nodeCreateRequest.setSchema(new SchemaReferenceImpl().setName("test"));
		nodeCreateRequest.setParentNodeUuid(baseUuid);
		return call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));
	}

	private void assertDelete(String node1Uuid, String node2Uuid) {
		String microschemaContainerUuid = tx(() -> microschemaContainers().get("vcard").getUuid());

		// 4. Try to delete the microschema
		call(() -> client().deleteMicroschema(microschemaContainerUuid), BAD_REQUEST, "microschema_delete_still_in_use",
			microschemaContainerUuid);

		try (Tx tx = tx()) {
			MicroschemaContainer reloaded = boot().microschemaContainerRoot().findByUuid(microschemaContainerUuid);
			assertNotNull("The microschema should not have been deleted.", reloaded);
		}

		// 5. Delete the newly created node
		call(() -> client().deleteNode(PROJECT_NAME, node1Uuid));

		call(() -> client().deleteMicroschema(microschemaContainerUuid), BAD_REQUEST, "microschema_delete_still_in_use",
			microschemaContainerUuid);

		// 6. Delete the second node and thus free the microschema from any references
		call(() -> client().deleteNode(PROJECT_NAME, node2Uuid));

		// 7. Attempt to delete the microschema now
		call(() -> client().deleteMicroschema(microschemaContainerUuid));

		try (Tx tx = tx()) {
			MicroschemaContainer searched = boot().microschemaContainerRoot().findByUuid(microschemaContainerUuid);
			assertNull("The microschema should have been deleted.", searched);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		MicroschemaContainer microschema;
		try (Tx tx = tx()) {
			microschema = microschemaContainers().get("vcard");
			assertNotNull(microschema);
			role().revokePermissions(microschema, DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().deleteMicroschema(microschema.getUuid()), FORBIDDEN, "error_missing_perm", microschema.getUuid(),
				DELETE_PERM.getRestPerm().getName());

			assertElement(boot().microschemaContainerRoot(), microschema.getUuid(), true);
		}
	}

}
