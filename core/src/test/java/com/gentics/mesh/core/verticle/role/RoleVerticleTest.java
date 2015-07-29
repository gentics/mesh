package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.data.relationship.Permission.CREATE_PERM;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleListResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.role.RoleUpdateRequest;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.json.JsonUtil;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class RoleVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return rolesVerticle;
	}

	// Create tests

	@Test
	public void testCreateRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		assertSuccess(future);

		RoleResponse restRole = future.result();
		test.assertRole(request, restRole);
	}

	@Test
	public void testCreateRoleWithConflictingName() throws Exception {

		// Create first Role
		String name = "new_role";
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName(name);
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		assertSuccess(future);

		future = getClient().createRole(request);
		latchFor(future);
		expectException(future, CONFLICT, "role_conflicting_name");
	}

	@Test
	public void testCreateDeleteRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> createFuture = getClient().createRole(request);
		latchFor(createFuture);
		assertSuccess(createFuture);

		RoleResponse restRole = createFuture.result();
		test.assertRole(request, restRole);

		Future<GenericMessageResponse> deleteFuture = getClient().deleteRole(restRole.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("role_deleted", deleteFuture, restRole.getUuid() + "/" + restRole.getName());

	}

	@Test
	public void testCreateRoleWithNoPermissionOnGroup() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(group().getUuid());

		// Add needed permission to group
		role().revokePermissions(group(), CREATE_PERM);
		// roleRoot.revokePermission(role(), data().getMeshRoot().getRoleRoot(), CREATE);

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
	}

	@Test
	@Ignore("We can't test this using the rest client")
	public void testCreateRoleWithBogusJson() throws Exception {
		// String requestJson = "bogus text";
		// String response = request(info, POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		// expectMessageResponse("error_parse_request_json_error", response);
	}

	@Test
	public void testCreateRoleWithNoGroupId() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "role_missing_parentgroup_field");

	}

	@Test
	public void testCreateRoleWithNoName() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setGroupUuid(group().getUuid());

		Future<RoleResponse> future = getClient().createRole(request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_name_must_be_set");
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = role();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());

		Future<RoleResponse> future = getClient().findRoleByUuid(role.getUuid());
		latchFor(future);
		assertSuccess(future);
		RoleResponse restRole = future.result();
		test.assertRole(role, restRole);
	}

	@Test
	public void testReadExtraRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();

		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();
		Role extraRole = roleRoot.create("extra role");
		extraRole.setCreator(info.getUser());
		extraRole.setEditor(info.getUser());
		group().addRole(extraRole);

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
		role().addPermissions(extraRole, READ_PERM);

		Future<RoleResponse> future = getClient().findRoleByUuid(extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		RoleResponse restRole = future.result();
		test.assertRole(extraRole, restRole);

	}

	@Test
	public void testReadExtraRoleByUUIDWithMissingPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();

		Role extraRole = roleRoot.create("extra role");
		group().addRole(extraRole);

		// Revoke read permission from the role
		role().revokePermissions(extraRole, READ_PERM);

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());

		Future<RoleResponse> future = getClient().findRoleByUuid(extraRole.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraRole.getUuid());

	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = role();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());
		role.revokePermissions(role, READ_PERM);

		Future<RoleResponse> future = getClient().findRoleByUuid(role.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", role.getUuid());
	}

	@Test
	public void testReadRoles() throws Exception {
		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();

		Role noPermRole = roleRoot.create("no_perm_role");
		final int nRoles = 21;

		role().addPermissions(group(), READ_PERM);

		// Create and save some roles
		for (int i = 0; i < nRoles; i++) {
			Role extraRole = roleRoot.create("extra role " + i);
			group().addRole(extraRole);
			role().addPermissions(extraRole, READ_PERM);
		}

		// Role with no permission
		group().addRole(noPermRole);
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
		future = getClient().findRoles(new PagingInfo(page, perPage));
		latchFor(future);
		assertSuccess(future);
		restResponse = future.result();
		assertEquals("The amount of items for page {" + page + "} does not match the expected amount.", 11, restResponse.getData().size());

		// created roles + test data role
		// TODO fix this assertion. Actually we would need to add 1 since the own role must also be included in the list
		int totalRoles = nRoles + data().getRoles().size();
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
			Future<RoleListResponse> pageFuture = getClient().findRoles(new PagingInfo(page, perPage));
			latchFor(pageFuture);
			assertSuccess(pageFuture);
			restResponse = pageFuture.result();
			allRoles.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

		// Verify that extra role is not part of the response
		final String noPermRoleName = noPermRole.getName();
		List<RoleResponse> filteredUserList = allRoles.parallelStream().filter(restRole -> restRole.getName().equals(noPermRoleName))
				.collect(Collectors.toList());
		assertTrue("Extra role should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		future = getClient().findRoles(new PagingInfo(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findRoles(new PagingInfo(1, 0));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findRoles(new PagingInfo(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findRoles(new PagingInfo(4242, 25));
		latchFor(future);
		assertSuccess(future);
		String response = JsonUtil.toJson(future.result());
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":2,\"total_count\":36}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	// Update tests

	@Test
	public void testUpdateRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {

		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();
		Role extraRole = roleRoot.create("extra role");
		group().addRole(extraRole);

		role().addPermissions(extraRole, UPDATE_PERM);
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");

		Future<RoleResponse> future = getClient().updateRole(extraRole.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
		RoleResponse restRole = future.result();
		assertEquals(request.getName(), restRole.getName());
		assertEquals(extraRole.getUuid(), restRole.getUuid());

		// Check that the extra role was updated as expected
		roleRoot.findByUuid(extraRole.getUuid(), rh -> {
			Role reloadedRole = rh.result();
			assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
		});
	}

	@Test
	public void testUpdateOwnRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Role role = role();

		RoleUpdateRequest restRole = new RoleUpdateRequest();
		restRole.setName("renamed role");

		Future<RoleResponse> future = getClient().updateRole(role.getUuid(), restRole);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", role.getUuid());

		// Add the missing permission and try again
		role().addPermissions(role(), Permission.UPDATE_PERM);

		future = getClient().updateRole(role.getUuid(), restRole);
		latchFor(future);
		assertSuccess(future);

		// Check that the role was updated
		boot.roleRoot().findByUuid(role.getUuid(), rh -> {
			Role reloadedRole = rh.result();
			assertEquals(restRole.getName(), reloadedRole.getName());
		});

	}

	// Delete tests

	@Test
	public void testDeleteRoleByUUID() throws Exception {
		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();
		Role extraRole = roleRoot.create("extra role");
		group().addRole(extraRole);
		role().addPermissions(extraRole, DELETE_PERM);

		Future<GenericMessageResponse> future = getClient().deleteRole(extraRole.getUuid());
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("role_deleted", future, extraRole.getUuid() + "/" + extraRole.getName());
		roleRoot.findByUuid(extraRole.getUuid(), rh -> {
			assertNull("The user should have been deleted", rh.result());
		});
	}

	@Test
	public void testDeleteRoleByUUIDWithMissingPermission() throws Exception {
		Future<GenericMessageResponse> future = getClient().deleteRole(role().getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", role().getUuid());
		boot.roleRoot().findByUuid(role().getUuid(), rh -> {
			assertNotNull("The role should not have been deleted", rh.result());
		});
	}

}
