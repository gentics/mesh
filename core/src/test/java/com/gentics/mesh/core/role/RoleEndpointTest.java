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

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.dagger.MeshInternal;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;

import rx.Single;

public class RoleEndpointTest extends AbstractBasicCrudEndpointTest {

	@Test
	@Override
	public void testCreate() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		RoleResponse restRole = call(() -> client().createRole(request));
		assertThat(dummySearchProvider).hasStore(Role.composeIndexName(), Role.composeIndexType(), restRole.getUuid());
		assertThat(dummySearchProvider).hasEvents(1, 0, 0, 0);

		try (NoTx noTx = db.noTx()) {
			Role createdRole = meshRoot().getRoleRoot().findByUuid(restRole.getUuid());
			assertTrue(user().hasPermission(createdRole, UPDATE_PERM));
			assertTrue(user().hasPermission(createdRole, READ_PERM));
			assertTrue(user().hasPermission(createdRole, DELETE_PERM));
			assertTrue(user().hasPermission(createdRole, CREATE_PERM));

			String roleUuid = restRole.getUuid();
			restRole = call(() -> client().findRoleByUuid(roleUuid));
			assertThat(restRole).matches(request);
			assertElement(meshRoot().getRoleRoot(), restRole.getUuid(), true);
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getRoleRoot(), CREATE_PERM);
		}

		String roleRootUuid = db.noTx(() -> meshRoot().getRoleRoot().getUuid());
		call(() -> client().createRole(request), FORBIDDEN, "error_missing_perm", roleRootUuid);

	}

	@Test
	public void testCreateRoleWithConflictingName() throws Exception {
		String name = "new_role";
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName(name);
		call(() -> client().createRole(request));

		// Creating the second role must fail due to name conflict
		call(() -> client().createRole(request), CONFLICT, "role_conflicting_name");
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		RoleResponse restRole = call(() -> client().createRole(request));
		assertThat(restRole).matches(request);

		call(() -> client().deleteRole(restRole.getUuid()));
	}

	@Test
	public void testCreateWithNoPermissionRoleRoot() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		try (NoTx noTx = db.noTx()) {
			// Add needed permission to group
			role().revokePermissions(meshRoot().getRoleRoot(), CREATE_PERM);
		}

		try (NoTx noTx = db.noTx()) {
			call(() -> client().createRole(request), FORBIDDEN, "error_missing_perm", meshRoot().getRoleRoot().getUuid());
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
		call(() -> client().createRole(request), BAD_REQUEST, "error_name_must_be_set");
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			String uuid = role.getUuid();
			assertNotNull("The UUID of the role must not be null.", role.getUuid());

			RoleResponse restRole = call(() -> client().findRoleByUuid(uuid));
			assertThat(restRole).matches(role());
		}
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
			role().grantPermissions(extraRole, READ_PERM);

			RoleResponse restRole = call(() -> client().findRoleByUuid(extraRole.getUuid()));
			assertThat(restRole).matches(extraRole);
		}

	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			String uuid = role.getUuid();

			RoleResponse restRole = call(() -> client().findRoleByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())));
			assertNotNull(restRole.getRolePerms());
			assertThat(restRole.getRolePerms()).hasPerm(Permission.values());
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			// Revoke read permission from the role
			role().revokePermissions(extraRole, READ_PERM);

			assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
			call(() -> client().findRoleByUuid(extraRole.getUuid()), FORBIDDEN, "error_missing_perm", extraRole.getUuid());
		}

	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			String uuid = role.getUuid();
			assertNotNull("The UUID of the role must not be null.", role.getUuid());
			role.revokePermissions(role, READ_PERM);
			call(() -> client().findRoleByUuid(uuid), FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTx noTx = db.noTx()) {
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
			RoleListResponse restResponse = call(() -> client().findRoles());
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			assertEquals(25, restResponse.getData().size());

			int perPage = 11;
			final int currentPage = 1;
			restResponse = call(() -> client().findRoles(new PagingParametersImpl(currentPage, perPage)));
			assertEquals("The amount of items for page {" + currentPage + "} does not match the expected amount.", 11, restResponse.getData().size());

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
			for (int page = 1; page <= totalPages; page++) {
				final int cPage = page;
				restResponse = call(() -> client().findRoles(new PagingParametersImpl(cPage, perPage)));
				allRoles.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

			// Verify that extra role is not part of the response
			List<RoleResponse> filteredUserList = allRoles.parallelStream().filter(restRole -> restRole.getName().equals(noPermRoleName))
					.collect(Collectors.toList());
			assertTrue("Extra role should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			call(() -> client().findRoles(new PagingParametersImpl(-1, perPage)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");
			call(() -> client().findRoles(new PagingParametersImpl(1, -1)), BAD_REQUEST, "error_pagesize_parameter", "-1");
			RoleListResponse response = call(() -> client().findRoles(new PagingParametersImpl(4242, 25)));

			assertEquals(0, response.getData().size());
			assertEquals(4242, response.getMetainfo().getCurrentPage());
			assertEquals(1, response.getMetainfo().getPageCount());
			assertEquals(25, response.getMetainfo().getTotalCount());
			assertEquals(25, response.getMetainfo().getPerPage());
		}
	}

	@Test
	public void testReadMetaCountOnly() {
		MeshResponse<RoleListResponse> future = client().findRoles(new PagingParametersImpl(1, 0)).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getData().size());
	}

	// Update tests

	@Test
	@Override
	public void testUpdate() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		try (NoTx noTx = db.noTx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			String roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, UPDATE_PERM);
			RoleUpdateRequest request = new RoleUpdateRequest();
			request.setName("renamed role");

			MeshResponse<RoleResponse> future = client().updateRole(roleUuid, request).invoke();
			latchFor(future);
			assertSuccess(future);
			RoleResponse restRole = future.result();
			assertEquals(request.getName(), restRole.getName());
			assertEquals(roleUuid, restRole.getUuid());

			// Check that the extra role was updated as expected
			Role reloadedRole = roleRoot.findByUuid(roleUuid);
			reloadedRole.reload();
			assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(role(), UPDATE_PERM);
			String uuid = role().getUuid();
			RoleUpdateRequest request = new RoleUpdateRequest();
			request.setName("New Name");

			MeshResponse<RoleResponse> future = client().updateRole(uuid, request).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		}

	}

	@Test
	public void testUpdateConflictCheck() {
		try (NoTx noTx = db.noTx()) {
			MeshInternal.get().boot().meshRoot().getRoleRoot().create("test123", user());
			RoleUpdateRequest request = new RoleUpdateRequest();
			request.setName("test123");

			MeshResponse<RoleResponse> future = client().updateRole(role().getUuid(), request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "role_conflicting_name");
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		MeshResponse<RoleResponse> future = client().updateRole("bogus", request).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

	}

	@Test
	public void testUpdateOwnRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		try (NoTx noTx = db.noTx()) {
			Role role = role();
			String uuid = role.getUuid();

			role().revokePermissions(role, UPDATE_PERM);

			RoleUpdateRequest restRole = new RoleUpdateRequest();
			restRole.setName("renamed role");

			MeshResponse<RoleResponse> future = client().updateRole(uuid, restRole).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);

			// Add the missing permission and try again
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);

			future = client().updateRole(uuid, restRole).invoke();
			latchFor(future);
			assertSuccess(future);

			// Check that the role was updated
			Role reloadedRole = boot.roleRoot().findByUuid(uuid);
			reloadedRole.reload();
			assertEquals(restRole.getName(), reloadedRole.getName());
		}

	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			RoleRoot roleRoot = meshRoot().getRoleRoot();
			Role extraRole = roleRoot.create("extra role", user());
			group().addRole(extraRole);
			String roleUuid = extraRole.getUuid();
			role().grantPermissions(extraRole, DELETE_PERM);

			dummySearchProvider.clear();
			call(() -> client().deleteRole(roleUuid));
			assertThat(dummySearchProvider).hasStore(Group.composeIndexName(), Group.composeIndexType(), group().getUuid());
			assertThat(dummySearchProvider).hasDelete(Role.composeIndexName(), Role.composeIndexType(), roleUuid);
			assertThat(dummySearchProvider).hasEvents(1, 1, 0, 0);
			meshRoot().getRoleRoot().reload();
			assertElement(meshRoot().getRoleRoot(), roleUuid, false);
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(role(), DELETE_PERM);
		}

		try (NoTx noTx = db.noTx()) {
			String uuid = role().getUuid();
			call(() -> client().deleteRole(uuid), FORBIDDEN, "error_missing_perm", uuid);
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
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().updateRole(role().getUuid(), request).invoke());
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testReadByUuidMultithreaded() throws Exception {

		Single<GenericMessageResponse> future = client().login();
		future.toBlocking().value();

		int nJobs = 10;
		String uuid = role().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().findRoleByUuid(uuid).invoke());
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
		Set<MeshResponse<Void>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().deleteRole(uuid).invoke());
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testCreateMultithreaded() throws Exception {

		Single<GenericMessageResponse> future = client().login();
		future.toBlocking().value();

		int nJobs = 20;
		// CyclicBarrier barrier = prepareBarrier(1);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName("new_role_" + i);
			set.add(client().createRole(request).invoke());
		}
		validateFutures(set);
	}

	@Test
	@Override
	@Ignore("disabled due to instability")
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Single<GenericMessageResponse> observable = client().login();
			observable.toBlocking().value();

			int nJobs = 400;
			Set<MeshResponse<RoleResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findRoleByUuid(role().getUuid()).invoke());
			}
			for (MeshResponse<RoleResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

}
