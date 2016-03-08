package com.gentics.mesh.core.schema;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.microschema.impl.MicroschemaModel;
import com.gentics.mesh.core.rest.schema.Microschema;
import com.gentics.mesh.core.verticle.microschema.MicroschemaVerticle;
import com.gentics.mesh.query.impl.RolePermissionParameter;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;

import io.vertx.core.Future;

public class MicroschemaVerticleTest extends AbstractBasicCrudVerticleTest {

	@Autowired
	private MicroschemaVerticle microschemaVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(microschemaVerticle);
		return list;
	}

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
		MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
		assertNotNull(vcardContainer);
		String uuid = vcardContainer.getUuid();

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findMicroschemaByUuid(uuid));
		}
		validateSet(set, barrier);
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
		Microschema request = new MicroschemaModel();
		request.setName("new microschema name");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().createMicroschema(request));
		}
		validateCreation(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
		assertNotNull(vcardContainer);
		String uuid = vcardContainer.getUuid();

		Set<Future<Microschema>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findMicroschemaByUuid(uuid));
		}
		for (Future<Microschema> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		Microschema request = new MicroschemaModel();
		request.setName("new microschema name");
		request.setDescription("microschema description");

		assertThat(searchProvider).recordedStoreEvents(0);
		Future<Microschema> future = getClient().createMicroschema(request);
		latchFor(future);
		assertSuccess(future);
		assertThat(searchProvider).recordedStoreEvents(1);
		Microschema microschemaResponse = future.result();
		assertThat((Microschema) microschemaResponse).isEqualToComparingOnlyGivenFields(request, "name", "description");
	}

	@Ignore("Not yet implemented")

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
		assertNotNull(vcardContainer);
		Future<Microschema> future = getClient().findMicroschemaByUuid(vcardContainer.getUuid());
		latchFor(future);
		assertSuccess(future);
		Microschema microschemaResponse = future.result();
		assertThat((Microschema) microschemaResponse).isEqualToComparingOnlyGivenFields(vcardContainer.getLatestVersion().getSchema(), "name", "description");
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
		assertNotNull(vcardContainer);
		String uuid = vcardContainer.getUuid();

		Future<Microschema> future = getClient().findMicroschemaByUuid(uuid, new RolePermissionParameter().setRoleUuid(role().getUuid()));
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result().getRolePerms());
		assertEquals(4, future.result().getRolePerms().length);
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		MicroschemaContainer vcardContainer = microschemaContainers().get("vcard");
		assertNotNull(vcardContainer);

		role().grantPermissions(vcardContainer, DELETE_PERM);
		role().grantPermissions(vcardContainer, UPDATE_PERM);
		role().grantPermissions(vcardContainer, CREATE_PERM);
		role().revokePermissions(vcardContainer, READ_PERM);

		Future<Microschema> future = getClient().findMicroschemaByUuid(vcardContainer.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", vcardContainer.getUuid());
	}

	@Ignore("Not yet implemented")
	@Test
	@Override
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
		MicroschemaContainer microschema = microschemaContainers().get("vcard");
		assertNotNull(microschema);
		role().revokePermissions(microschema, UPDATE_PERM);

		Microschema request = new MicroschemaModel();
		request.setName("new-name");

		Future<GenericMessageResponse> future = getClient().updateMicroschema(microschema.getUuid(), request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", microschema.getUuid());
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		MicroschemaContainer microschema = microschemaContainers().get("vcard");
		assertNotNull(microschema);
		String oldName = microschema.getName();
		Microschema request = new MicroschemaModel();
		request.setName("new-name");

		Future<GenericMessageResponse> future = getClient().updateMicroschema("bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

		MicroschemaContainer reloaded = boot.microschemaContainerRoot().findByUuid(microschema.getUuid()).toBlocking().first();
		assertEquals("The name should not have been changed.", oldName, reloaded.getName());
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		MicroschemaContainer microschema = microschemaContainers().get("vcard");
		assertNotNull(microschema);

		Future<GenericMessageResponse> future = getClient().deleteMicroschema(microschema.getUuid());
		latchFor(future);
		assertSuccess(future);
		expectResponseMessage(future, "microschema_deleted", microschema.getUuid() + "/" + microschema.getName());

		MicroschemaContainer reloaded = boot.microschemaContainerRoot().findByUuid(microschema.getUuid()).toBlocking().single();
		assertNull("The microschema should have been deleted.", reloaded);
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		MicroschemaContainer microschema = microschemaContainers().get("vcard");
		assertNotNull(microschema);

		role().revokePermissions(microschema, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteMicroschema(microschema.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", microschema.getUuid());

		assertElement(boot.microschemaContainerRoot(), microschema.getUuid(), true);
	}

}
