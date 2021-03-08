package com.gentics.mesh.core.role;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;

import io.reactivex.Single;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT, startServer = true)
public class RoleEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	@Override
	public void testCreate() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		expect(ROLE_CREATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("new_role").uuidNotNull();
		}).total(1);
		expect(ROLE_UPDATED).total(0);

		RoleResponse restRole = call(() -> client().createRole(request));

		awaitEvents();
		waitForSearchIdleEvent();

		assertThat(trackingSearchProvider()).hasStore(Role.composeIndexName(), restRole.getUuid());
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);

		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibRole createdRole = tx.roleDao().findByUuid(restRole.getUuid());
			assertTrue(userDao.hasPermission(user(), createdRole, UPDATE_PERM));
			assertTrue(userDao.hasPermission(user(), createdRole, READ_PERM));
			assertTrue(userDao.hasPermission(user(), createdRole, DELETE_PERM));
			assertTrue(userDao.hasPermission(user(), createdRole, CREATE_PERM));

			String roleUuid = restRole.getUuid();
			restRole = call(() -> client().findRoleByUuid(roleUuid));
			assertThat(restRole).matches(request);
			assertNotNull(tx.roleDao().findByUuid(restRole.getUuid()));
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), tx.data().permissionRoots().role(), CREATE_PERM);
			tx.success();
		}

		String roleRootUuid = tx(tx -> {
			return tx.data().permissionRoots().role().getUuid();
		});
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		call(() -> client().createRole(request), FORBIDDEN, "error_missing_perm", roleRootUuid, CREATE_PERM.getRestPerm().getName());

	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		String uuid = UUIDUtil.randomUUID();
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		RoleResponse restRole = call(() -> client().createRole(uuid, request));
		assertThat(restRole).hasName("new_role").hasUuid(uuid);
	}

	@Test
	@Override
	public void testCreateWithDuplicateUuid() throws Exception {
		String uuid = userUuid();
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		call(() -> client().createRole(uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
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

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			// Add needed permission to group
			roleDao.revokePermissions(role(), tx.data().permissionRoots().role(), CREATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().createRole(request), FORBIDDEN, "error_missing_perm", tx.data().permissionRoots().role().getUuid(),
				CREATE_PERM.getRestPerm().getName());
		}
	}

	@Test
	@Ignore("We can't test this using the rest client")
	public void testCreateRoleWithBogusJson() throws Exception {
		// String requestJson = "bogus text";
		// String response = request(info, POST, CURRENT_API_BASE_PATH + "/roles/", 400, "Bad Request", requestJson);
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
		try (Tx tx = tx()) {
			RoleResponse restRole = call(() -> client().findRoleByUuid(roleUuid()));
			assertThat(restRole).matches(role());
		}
	}

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		HibRole extraRole;
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupDao = tx.groupDao();

			extraRole = roleDao.create("extra role", user());
			groupDao.addRole(group(), extraRole);
			assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
			roleDao.grantPermissions(role(), extraRole, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			RoleResponse restRole = call(() -> client().findRoleByUuid(extraRole.getUuid()));
			assertThat(restRole).matches(extraRole);
		}
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		RoleResponse restRole = call(() -> client().findRoleByUuid(roleUuid(), new RolePermissionParametersImpl().setRoleUuid(roleUuid())));
		assertNotNull(restRole.getRolePerms());
		assertThat(restRole.getRolePerms()).hasPerm(Permission.basicPermissions());
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String extraRoleUuid;
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupRoot = tx.groupDao();
			HibRole extraRole = roleDao.create("extra role", user());
			extraRoleUuid = extraRole.getUuid();
			groupRoot.addRole(group(), extraRole);
			// Revoke read permission from the role
			roleDao.revokePermissions(role(), extraRole, READ_PERM);
			tx.success();
		}

		call(() -> client().findRoleByUuid(extraRoleUuid), FORBIDDEN, "error_missing_perm", extraRoleUuid, READ_PERM.getRestPerm().getName());

	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), role(), READ_PERM);
			tx.success();
		}

		call(() -> client().findRoleByUuid(roleUuid()), FORBIDDEN, "error_missing_perm", roleUuid(), READ_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		final int nRoles = 21;
		final String noPermRoleName = "no_perm_role";
		final int initialRolesCount = roles().size();

		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupRoot = tx.groupDao();

			HibRole noPermRole = roleDao.create(noPermRoleName, user());
			roleDao.grantPermissions(role(), group(), READ_PERM);

			// Create and save some roles
			for (int i = 0; i < nRoles; i++) {
				HibRole extraRole = roleDao.create("extra role " + i, user());
				groupRoot.addRole(group(), extraRole);
				roleDao.grantPermissions(role(), extraRole, READ_PERM);
			}
			// Role with no permission
			groupRoot.addRole(group(), noPermRole);
			tx.success();
		}

		// Test default paging parameters
		RoleListResponse restResponse = call(() -> client().findRoles());
		assertNull(restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(nRoles + initialRolesCount, restResponse.getData().size());

		long perPage = 11;
		final int currentPage = 1;
		restResponse = call(() -> client().findRoles(new PagingParametersImpl(currentPage, perPage)));
		assertEquals("The amount of items for page {" + currentPage + "} does not match the expected amount.", 11, restResponse.getData().size());

		// created roles + test data role
		// TODO fix this assertion. Actually we would need to add 1 since the own role must also be included in the list
		int totalRoles = nRoles + data().getRoles().size();
		int totalPages = (int) Math.ceil(totalRoles / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The total pages could does not match. We expect {" + totalRoles + "} total roles and {" + perPage
			+ "} roles per page. Thus we expect {" + totalPages + "} pages", totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage().longValue());
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
		call(() -> client().findRoles(new PagingParametersImpl(1, -1L)), BAD_REQUEST, "error_pagesize_parameter", "-1");
		RoleListResponse response = call(() -> client().findRoles(new PagingParametersImpl(4242, 25L)));

		assertEquals(0, response.getData().size());
		assertEquals(4242, response.getMetainfo().getCurrentPage());
		assertEquals(1, response.getMetainfo().getPageCount());
		assertEquals(nRoles + initialRolesCount, response.getMetainfo().getTotalCount());
		assertEquals(25, response.getMetainfo().getPerPage().longValue());

	}

	@Test
	public void testReadMetaCountOnly() {
		RoleListResponse response = client().findRoles(new PagingParametersImpl(1, 0L)).blockingGet();
		assertEquals(0, response.getData().size());
	}

	// Update tests

	@Test
	@Override
	public void testUpdate() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		String extraRoleUuid = tx(tx -> {
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupDao = tx.groupDao();
			HibRole extraRole = roleDao.create("extra role", user());
			groupDao.addRole(group(), extraRole);
			roleDao.grantPermissions(role(), extraRole, UPDATE_PERM);
			return extraRole.getUuid();
		});

		expect(ROLE_UPDATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("renamed role").hasUuid(extraRoleUuid);
		}).total(1);
		expect(ROLE_CREATED).total(0);

		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		RoleResponse restRole = call(() -> client().updateRole(extraRoleUuid, request));
		assertEquals(request.getName(), restRole.getName());
		assertEquals(extraRoleUuid, restRole.getUuid());

		awaitEvents();

		try (Tx tx = tx()) {
			// Check that the extra role was updated as expected
			HibRole reloadedRole = tx.roleDao().findByUuid(extraRoleUuid);
			assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), role(), UPDATE_PERM);
			tx.success();
		}

		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("New Name");
		call(() -> client().updateRole(roleUuid(), request), FORBIDDEN, "error_missing_perm", roleUuid(), UPDATE_PERM.getRestPerm().getName());

	}

	@Test
	public void testUpdateConflictCheck() {
		try (Tx tx = tx()) {
			tx.roleDao().create("test123", user());
			tx.success();
		}

		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("test123");
		call(() -> client().updateRole(roleUuid(), request), CONFLICT, "role_conflicting_name");
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		call(() -> client().updateRole("bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");

	}

	@Test
	public void testUpdateOwnRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), role(), UPDATE_PERM);
			tx.success();
		}
		RoleUpdateRequest restRole = new RoleUpdateRequest();
		restRole.setName("renamed role");
		call(() -> client().updateRole(roleUuid(), restRole), FORBIDDEN, "error_missing_perm", roleUuid(), UPDATE_PERM.getRestPerm().getName());

		// Add the missing permission and try again
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			tx.success();
		}

		call(() -> client().updateRole(roleUuid(), restRole));

		// Check that the role was updated
		try (Tx tx = tx()) {
			HibRole reloadedRole = tx.roleDao().findByUuid(roleUuid());
			assertEquals(restRole.getName(), reloadedRole.getName());
		}

	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {

		String extraRoleUuid = tx(tx -> {
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupDao = tx.groupDao();
			HibRole extraRole = roleDao.create("extra role", user());
			groupDao.addRole(group(), extraRole);
			roleDao.grantPermissions(role(), extraRole, DELETE_PERM);
			return extraRole.getUuid();
		});

		expect(ROLE_DELETED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("extra role").hasUuid(extraRoleUuid);
		});

		trackingSearchProvider().clear().blockingAwait();
		call(() -> client().deleteRole(extraRoleUuid));

		awaitEvents();
		waitForSearchIdleEvent();

		assertThat(trackingSearchProvider()).hasDelete(Role.composeIndexName(), extraRoleUuid);
		assertThat(trackingSearchProvider()).hasEvents(0, 0, 1, 0, 0);

		try (Tx tx = tx()) {
			assertElement(tx.roleDao(), extraRoleUuid, false);
		}

	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), role(), DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			String uuid = role().getUuid();
			call(() -> client().deleteRole(uuid), FORBIDDEN, "error_missing_perm", uuid, DELETE_PERM.getRestPerm().getName());
			assertElement(tx.roleDao(), uuid, true);
		}
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testUpdateMultithreaded() throws InterruptedException {
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		int nJobs = 5;
		awaitConcurrentRequests(nJobs, i -> client().updateRole(role().getUuid(), request));
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testReadByUuidMultithreaded() throws Exception {

		Single<GenericMessageResponse> future = client().login();
		future.blockingGet();

		int nJobs = 10;
		String uuid = role().getUuid();
		awaitConcurrentRequests(nJobs, i -> client().findRoleByUuid(uuid));
	}

	@Test
	@Override
	@Ignore("not yet enabled")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		String uuid = role().getUuid();
		validateDeletion(i -> client().deleteRole(uuid), nJobs);
	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {

		Single<GenericMessageResponse> future = client().login();
		future.blockingGet();

		int nJobs = 20;

		awaitConcurrentRequests(nJobs, i -> {
			RoleCreateRequest request = new RoleCreateRequest();
			request.setName("new_role_" + i);

			return client().createRole(request);
		});
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			uuid = role().getUuid();
		}

		Single<GenericMessageResponse> observable = client().login();
		observable.blockingGet();

		int nJobs = 400;
		awaitConcurrentRequests(nJobs, i -> client().findRoleByUuid(uuid));
	}

	@Test
	@Override
	public void testPermissionResponse() {
		RoleResponse role = client().findRoles().blockingGet().getData().get(0);
		assertThat(role.getPermissions()).hasNoPublishPermsSet();
	}
}
