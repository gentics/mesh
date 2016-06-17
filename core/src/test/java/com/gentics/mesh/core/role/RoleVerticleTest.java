package com.gentics.mesh.core.role;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.core.verticle.role.RoleVerticle;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedCrudVerticleTest;

import io.vertx.core.Future;
import rx.Observable;

public class RoleVerticleTest extends AbstractBasicIsolatedCrudVerticleTest {

	@Autowired
	private RoleVerticle roleVerticle;

	@Autowired
	private AuthenticationVerticle authVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(authVerticle);
		list.add(roleVerticle);
		return list;
	}
	// Create tests

	@Test
	@Override
	public void testCreate() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		assertSuccess(future);

		try (NoTrx noTx = db.noTrx()) {
			Role createdRole = meshRoot().getRoleRoot().findByUuid(future.result().getUuid()).toBlocking().single();
			assertTrue(user().hasPermission(createdRole, UPDATE_PERM));
			assertTrue(user().hasPermission(createdRole, READ_PERM));
			assertTrue(user().hasPermission(createdRole, DELETE_PERM));
			assertTrue(user().hasPermission(createdRole, CREATE_PERM));

			Future<RoleResponse> readFuture = getClient().findRoleByUuid(future.result().getUuid());
			latchFor(readFuture);
			assertSuccess(readFuture);

			RoleResponse restRole = future.result();
			test.assertRole(request, restRole);
			assertElement(meshRoot().getRoleRoot(), restRole.getUuid(), true);
		}
	}

	@Test
	public void testCreateRoleWithConflictingName() throws Exception {
		// Create first Role
		String name = "new_role";
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName(name);

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		assertSuccess(future);

		future = getClient().createRole(request);
		latchFor(future);
		expectException(future, CONFLICT, "role_conflicting_name");
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		Future<RoleResponse> createFuture = getClient().createRole(request);
		latchFor(createFuture);
		assertSuccess(createFuture);

		RoleResponse restRole = createFuture.result();
		test.assertRole(request, restRole);

		Future<GenericMessageResponse> deleteFuture = getClient().deleteRole(restRole.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectResponseMessage(deleteFuture, "role_deleted", restRole.getUuid() + "/" + restRole.getName());
	}

	@Test
	public void testCreateWithNoPermissionRoleRoot() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		try (NoTrx noTx = db.noTrx()) {
			// Add needed permission to group
			role().revokePermissions(meshRoot().getRoleRoot(), CREATE_PERM);
		}

		try (NoTrx noTx = db.noTrx()) {
			Future<RoleResponse> future = getClient().createRole(request);
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", meshRoot().getRoleRoot().getUuid());
		}
	}

	@Test
	@Ignore("We can't test this using the rest client")
	public void testCreateRoleWithBogusJson() throws Exception {
		// String requestJson = "bogus text";
		// String response = request(info, POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		// expectMessageResponse("error_parse_request_json_error", response);
	}

	@Test
	public void testCreateRoleWithNoName() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_name_must_be_set");
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Role role = role();
			String uuid = role.getUuid();
			assertNotNull("The UUID of the role must not be null.", role.getUuid());

			Future<RoleResponse> future = getClient().findRoleByUuid(uuid);
			latchFor(future);
			assertSuccess(future);
			RoleResponse restRole = future.result();
			assertThat(restRole).matches(role());
		}
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
			role().grantPermissions(extraRole, READ_PERM);

			Future<RoleResponse> future = getClient().findRoleByUuid(extraRole.getUuid());
			latchFor(future);
			assertSuccess(future);
			RoleResponse restRole = future.result();
			assertThat(restRole).matches(extraRole);
		}

	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (NoTrx noTx = db.noTrx()) {
			Role role = role();
			String uuid = role.getUuid();

			Future<RoleResponse> future = getClient().findRoleByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid()));
			latchFor(future);
			assertSuccess(future);
			assertNotNull(future.result().getRolePerms());
			assertEquals(4, future.result().getRolePerms().length);
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			// Revoke read permission from the role
			role().revokePermissions(extraRole, READ_PERM);

			assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
			Future<RoleResponse> future = getClient().findRoleByUuid(extraRole.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", extraRole.getUuid());
		}

	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Role role = role();
			String uuid = role.getUuid();
			assertNotNull("The UUID of the role must not be null.", role.getUuid());
			role.revokePermissions(role, READ_PERM);
			Future<RoleResponse> future = getClient().findRoleByUuid(uuid);
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			final int nRoles = 21;
			String noPermRoleName;

			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role noPermRole = roleRoot.create("no_perm_role", user());

			role().grantPermissions(group(), READ_PERM);

			// Create and save some roles
			for (int i = 0; i < nRoles; i++) {
				Role extraRole = roleRoot.create("extra role " + i, user());
				group().addRole(extraRole);
				role().grantPermissions(extraRole, READ_PERM);
			}
			// Role with no permission
			group().addRole(noPermRole);

			noPermRoleName = noPermRole.getName();

			// Test default paging parameters
			Future<RoleListResponse> future = getClient().findRoles();
			latchFor(future);
			assertSuccess(future);
			RoleListResponse restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 11;
			int page = 1;
			future = getClient().findRoles(new PagingParameters(page, perPage));
			latchFor(future);
			assertSuccess(future);
			restResponse = future.result();
			assertEquals("The amount of items for page {" + page + "} does not match the expected amount.", 11, restResponse.getData().size());

			// created roles + test data role
			// TODO fix this assertion. Actually we would need to add 1 since the own role must also be included in the list
			int totalRoles = nRoles + roles().size();
			int totalPages = (int) Math.ceil(totalRoles / (double) perPage);
			assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals("The total pages could does not match. We expect {" + totalRoles + "} total roles and {" + perPage
					+ "} roles per page. Thus we expect {" + totalPages + "} pages", totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			for (RoleResponse role : restResponse.getData()) {
				System.out.println(role.getName());
			}
			assertEquals(totalRoles, restResponse.getMetainfo().getTotalCount());

			List<RoleResponse> allRoles = new ArrayList<>();
			for (page = 1; page <= totalPages; page++) {
				Future<RoleListResponse> pageFuture = getClient().findRoles(new PagingParameters(page, perPage));
				latchFor(pageFuture);
				assertSuccess(pageFuture);
				restResponse = pageFuture.result();
				allRoles.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

			// Verify that extra role is not part of the response
			List<RoleResponse> filteredUserList = allRoles.parallelStream().filter(restRole -> restRole.getName().equals(noPermRoleName))
					.collect(Collectors.toList());
			assertTrue("Extra role should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			future = getClient().findRoles(new PagingParameters(-1, perPage));
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");

			future = getClient().findRoles(new PagingParameters(1, -1));
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = getClient().findRoles(new PagingParameters(4242, 25));
			latchFor(future);
			assertSuccess(future);

			assertEquals(0, future.result().getData().size());
			assertEquals(4242, future.result().getMetainfo().getCurrentPage());
			assertEquals(1, future.result().getMetainfo().getPageCount());
			assertEquals(25, future.result().getMetainfo().getTotalCount());
			assertEquals(25, future.result().getMetainfo().getPerPage());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		Future<RoleListResponse> future = getClient().findRoles(new PagingParameters(1, 0));
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	// Update tests

	@Test
	@Override
	public void testUpdate() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		try (NoTrx noTx = db.noTrx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			String roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, UPDATE_PERM);
			RoleUpdateRequest request = new RoleUpdateRequest();
			request.setName("renamed role");

			Future<RoleResponse> future = getClient().updateRole(roleUuid, request);
			latchFor(future);
			assertSuccess(future);
			RoleResponse restRole = future.result();
			assertEquals(request.getName(), restRole.getName());
			assertEquals(roleUuid, restRole.getUuid());

			// Check that the extra role was updated as expected
			Role reloadedRole = roleRoot.findByUuid(roleUuid).toBlocking().single();
			reloadedRole.reload();
			assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			role().revokePermissions(role(), UPDATE_PERM);
			String uuid = role().getUuid();
			RoleUpdateRequest request = new RoleUpdateRequest();
			request.setName("New Name");

			Future<RoleResponse> future = getClient().updateRole(uuid, request);
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}

	}

	@Test
	public void testUpdateConflictCheck() {
		try (NoTrx noTx = db.noTrx()) {
			MeshRoot.getInstance().getRoleRoot().create("test123", user());
			RoleUpdateRequest request = new RoleUpdateRequest();
			request.setName("test123");

			Future<RoleResponse> future = getClient().updateRole(role().getUuid(), request);
			latchFor(future);
			expectException(future, CONFLICT, "role_conflicting_name");
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		Future<RoleResponse> future = getClient().updateRole("bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

	}

	@Test
	public void testUpdateOwnRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		try (NoTrx noTx = db.noTrx()) {
			Role role = role();
			String uuid = role.getUuid();

			role().revokePermissions(role, UPDATE_PERM);

			RoleUpdateRequest restRole = new RoleUpdateRequest();
			restRole.setName("renamed role");

			Future<RoleResponse> future = getClient().updateRole(uuid, restRole);
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);

			// Add the missing permission and try again
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);

			future = getClient().updateRole(uuid, restRole);
			latchFor(future);
			assertSuccess(future);

			// Check that the role was updated
			Role reloadedRole = boot.roleRoot().findByUuid(uuid).toBlocking().first();
			reloadedRole.reload();
			assertEquals(restRole.getName(), reloadedRole.getName());
		}

	}

	// Delete tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			String roleUuid = extraRole.getUuid();
			String roleName = extraRole.getName();
			role().grantPermissions(extraRole, DELETE_PERM);

			Future<GenericMessageResponse> future = getClient().deleteRole(roleUuid);
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "role_deleted", roleUuid + "/" + roleName);
			meshRoot().getRoleRoot().reload();
			assertElement(meshRoot().getRoleRoot(), roleUuid, false);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			role().revokePermissions(role(), DELETE_PERM);
		}

		try (NoTrx noTx = db.noTrx()) {
			String uuid = role().getUuid();
			Future<GenericMessageResponse> future = getClient().deleteRole(uuid);
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
			assertElement(meshRoot().getRoleRoot(), uuid, true);
		}
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws InterruptedException {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		int nJobs = 5;
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().updateRole(role().getUuid(), request));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testReadByUuidMultithreaded() throws Exception {

		Observable<GenericMessageResponse> future = getClient().login();
		future.toBlocking().single();

		int nJobs = 10;
		String uuid = role().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findRoleByUuid(uuid));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		String uuid = role().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<GenericMessageResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().deleteRole(uuid));
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testCreateMultithreaded() throws Exception {

		Observable<GenericMessageResponse> future = getClient().login();
		future.toBlocking().single();

		int nJobs = 20;
		// CyclicBarrier barrier = prepareBarrier(1);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName("new_role_" + i);
			set.add(getClient().createRole(request));
		}
		validateFutures(set);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Observable<GenericMessageResponse> observable = getClient().login();
			observable.toBlocking().single();

			int nJobs = 400;
			Set<Future<RoleResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(getClient().findRoleByUuid(role().getUuid()));
			}
			for (Future<RoleResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

}
