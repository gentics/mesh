package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaCreateRequest;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaResponse;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaUpdateRequest;
import com.gentics.mesh.core.rest.node.NodeCreateRequest;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;

public class MicroschemaEndpointTest extends AbstractBasicCrudEndpointTest {

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
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			String uuid = vcardContainer.getUuid();

			CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findMicroschemaByUuid(uuid).invoke());
			}
			validateSet(set, barrier);
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
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName("new microschema name");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().createMicroschema(request).invoke());
		}
		validateCreation(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			String uuid = vcardContainer.getUuid();

			Set<MeshResponse<MicroschemaResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findMicroschemaByUuid(uuid).invoke());
			}
			for (MeshResponse<MicroschemaResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName("new microschema name");
		request.setDescription("microschema description");

		assertThat(dummySearchProvider).recordedStoreEvents(0);
		MicroschemaResponse microschemaResponse = call(() -> client().createMicroschema(request));
		assertThat(dummySearchProvider).recordedStoreEvents(1);
		assertThat(microschemaResponse.getPermissions()).hasPerm(READ, CREATE, DELETE, UPDATE);
		assertThat((Microschema) microschemaResponse).isEqualToComparingOnlyGivenFields(request, "name", "description");
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		MicroschemaCreateRequest request = new MicroschemaCreateRequest();
		request.setName("new microschema name");
		request.setDescription("microschema description");

		String microschemaRootUuid = db.noTx(() -> meshRoot().getMicroschemaContainerRoot().getUuid());
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getMicroschemaContainerRoot(), CREATE_PERM);
		}
		call(() -> client().createMicroschema(request), FORBIDDEN, "error_missing_perm", microschemaRootUuid);

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
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			MicroschemaResponse microschemaResponse = call(() -> client().findMicroschemaByUuid(vcardContainer.getUuid()));
			assertThat((Microschema) microschemaResponse).isEqualToComparingOnlyGivenFields(vcardContainer.getLatestVersion().getSchema(), "name",
					"description");
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);
			String uuid = vcardContainer.getUuid();

			MicroschemaResponse microschema = call(
					() -> client().findMicroschemaByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())));
			assertNotNull(microschema.getRolePerms());
			assertThat(microschema.getRolePerms()).hasPerm(Permission.values());
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
			assertNotNull(vcardContainer);

			role().grantPermissions(vcardContainer, DELETE_PERM);
			role().grantPermissions(vcardContainer, UPDATE_PERM);
			role().grantPermissions(vcardContainer, CREATE_PERM);
			role().revokePermissions(vcardContainer, READ_PERM);

			call(() -> client().findMicroschemaByUuid(vcardContainer.getUuid()), FORBIDDEN, "error_missing_perm", vcardContainer.getUuid());
		}
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
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainers().get("vcard");
			assertNotNull(microschema);
			role().revokePermissions(microschema, UPDATE_PERM);

			MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
			request.setName("new-name");

			MeshResponse<GenericMessageResponse> future = client().updateMicroschema(microschema.getUuid(), request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", microschema.getUuid());
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainers().get("vcard");
			assertNotNull(microschema);
			String oldName = microschema.getName();
			MicroschemaUpdateRequest request = new MicroschemaUpdateRequest();
			request.setName("new-name");

			call(() -> client().updateMicroschema("bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");

			MicroschemaContainer reloaded = boot.microschemaContainerRoot().findByUuid(microschema.getUuid());
			assertEquals("The name should not have been changed.", oldName, reloaded.getName());
		}
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String uuid = db.noTx(() -> microschemaContainers().get("vcard").getUuid());

		call(() -> client().deleteMicroschema(uuid));

		// schema_delete_still_in_use

		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer reloaded = boot.microschemaContainerRoot().findByUuid(uuid);
			assertNull("The microschema should have been deleted.", reloaded);
		}
	}

	@Test
	public void testDeleteByUUIDWhileInUse() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschemaContainer = microschemaContainers().get("vcard");

			// 1. Create a new schema which uses the vcard microschema
			SchemaCreateRequest schema = FieldUtil.createMinimalValidSchemaCreateRequest();
			schema.addField(FieldUtil.createMicronodeFieldSchema("vcardtest").setAllowedMicroSchemas("vcard"));
			SchemaResponse response = call(() -> client().createSchema(schema));

			// 2. Assign the new schema to the project
			call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));

			// 3. Create a new node which uses the schema
			NodeCreateRequest nodeCreateRequest = new NodeCreateRequest();
			nodeCreateRequest.setLanguage("en");

			MicronodeResponse micronodeField = new MicronodeResponse();
			micronodeField.getFields().put("firstName", FieldUtil.createStringField("firstnameValue"));
			micronodeField.getFields().put("lastName", FieldUtil.createStringField("lastnameValue"));
			micronodeField.setMicroschema(new MicroschemaReference().setName("vcard"));
			nodeCreateRequest.getFields().put("vcardtest", micronodeField);
			nodeCreateRequest.setSchema(new SchemaReference().setName("test"));
			nodeCreateRequest.setParentNodeUuid(project().getBaseNode().getUuid());
			call(() -> client().createNode(PROJECT_NAME, nodeCreateRequest));

			// 4. Try to delete the microschema
			call(() -> client().deleteMicroschema(microschemaContainer.getUuid()), BAD_REQUEST, "microschema_delete_still_in_use",
					microschemaContainer.getUuid());

			MicroschemaContainer reloaded = boot.microschemaContainerRoot().findByUuid(microschemaContainer.getUuid());
			assertNotNull("The microschema should not have been deleted.", reloaded);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			MicroschemaContainer microschema = microschemaContainers().get("vcard");
			assertNotNull(microschema);

			role().revokePermissions(microschema, DELETE_PERM);
			call(() -> client().deleteMicroschema(microschema.getUuid()), FORBIDDEN, "error_missing_perm", microschema.getUuid());

			assertElement(boot.microschemaContainerRoot(), microschema.getUuid(), true);
		}
	}

}
