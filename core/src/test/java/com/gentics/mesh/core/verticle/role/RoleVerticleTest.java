package com.gentics.mesh.core.verticle.role;

import static com.gentics.mesh.core.data.model.relationship.Permission.CREATE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.model.relationship.Permission.UPDATE_PERM;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.Role;
import com.gentics.mesh.core.data.model.root.RoleRoot;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.rest.role.request.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.request.RoleUpdateRequest;
import com.gentics.mesh.core.rest.role.response.RoleListResponse;
import com.gentics.mesh.core.rest.role.response.RoleResponse;
import com.gentics.mesh.core.verticle.RoleVerticle;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.JsonUtils;

public class RoleVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private RoleVerticle rolesVerticle;

	@Autowired
	private GroupService groupService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return rolesVerticle;
	}

	// Create tests

	@Test
	public void testCreateRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(info.getGroup().getUuid());

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, POST, "/api/v1/roles/", 200, "OK", requestJson);
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		test.assertRole(request, restRole);
	}

	@Test
	public void testCreateDeleteRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(info.getGroup().getUuid());

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, POST, "/api/v1/roles/", 200, "OK", requestJson);
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		test.assertRole(request, restRole);

		response = request(info, DELETE, "/api/v1/roles/" + restRole.getUuid(), 200, "OK");
		expectMessageResponse("role_deleted", response, restRole.getUuid());

	}

	@Test
	public void testCreateRoleWithNoPermissionOnGroup() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(info.getGroup().getUuid());

		// Add needed permission to group
		info.getRole().revokePermissions(info.getGroup(), CREATE_PERM);
		// roleService.revokePermission(info.getRole(), data().getMeshRoot().getRoleRoot(), CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, POST, "/api/v1/roles/", 403, "Forbidden", requestJson);
		expectMessageResponse("error_missing_perm", response, info.getGroup().getUuid());
	}

	@Test
	public void testCreateRoleWithBogusJson() throws Exception {
		String requestJson = "bogus text";
		String response = request(info, POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		expectMessageResponse("error_parse_request_json_error", response);
	}

	@Test
	public void testCreateRoleWithNoGroupId() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		expectMessageResponse("role_missing_parentgroup_field", response);

	}

	@Test
	public void testCreateRoleWithNoName() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setGroupUuid(info.getGroup().getUuid());

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		expectMessageResponse("error_name_must_be_set", response);
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = info.getRole();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());

		String response = request(info, GET, "/api/v1/roles/" + role.getUuid(), 200, "OK");
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		test.assertRole(role, restRole);
	}

	@Test
	public void testReadExtraRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();

		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();
		Role extraRole = roleRoot.create("extra role");
		info.getGroup().addRole(extraRole);

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());
		info.getRole().addPermissions(extraRole, READ_PERM);

		String response = request(info, GET, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK");
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		test.assertRole(extraRole, restRole);

	}

	@Test
	public void testReadExtraRoleByUUIDWithMissingPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();

		Role extraRole = roleRoot.create("extra role");
		info.getGroup().addRole(extraRole);

		// Revoke read permission from the role
		info.getRole().revokePermissions(extraRole, READ_PERM);

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());

		String response = request(info, GET, "/api/v1/roles/" + extraRole.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, extraRole.getUuid());
	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = info.getRole();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());
		role.revokePermissions(role, READ_PERM);

		String response = request(info, GET, "/api/v1/roles/" + role.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, role.getUuid());

	}

	@Test
	public void testReadRoles() throws Exception {
		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();

		Role noPermRole = roleRoot.create("no_perm_role");
		final int nRoles = 21;

		info.getRole().addPermissions(info.getGroup(), READ_PERM);

		// Create and save some roles
		for (int i = 0; i < nRoles; i++) {
			Role extraRole = roleRoot.create("extra role " + i);
			info.getGroup().addRole(extraRole);
			info.getRole().addPermissions(extraRole, READ_PERM);
		}

		// Role with no permission
		info.getGroup().addRole(noPermRole);
		// Test default paging parameters
		String response = request(info, GET, "/api/v1/roles/", 200, "OK");
		RoleListResponse restResponse = JsonUtils.readValue(response, RoleListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		int page = 1;
		response = request(info, GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + page, 200, "OK");
		restResponse = JsonUtils.readValue(response, RoleListResponse.class);
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
			response = request(info, GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, RoleListResponse.class);
			allRoles.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

		// Verify that extra role is not part of the response
		final String noPermRoleName = noPermRole.getName();
		List<RoleResponse> filteredUserList = allRoles.parallelStream().filter(restRole -> restRole.getName().equals(noPermRoleName))
				.collect(Collectors.toList());
		assertTrue("Extra role should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, GET, "/api/v1/roles/?per_page=" + perPage + "&page=-1", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, GET, "/api/v1/roles/?per_page=0&page=1", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, GET, "/api/v1/roles/?per_page=-1&page=1", 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, GET, "/api/v1/roles/?per_page=25&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":2,\"total_count\":35}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	// Update tests

	@Test
	public void testUpdateRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {

		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();
		Role extraRole = roleRoot.create("extra role");
		info.getGroup().addRole(extraRole);

		info.getRole().addPermissions(extraRole, UPDATE_PERM);
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");
		request.setUuid(extraRole.getUuid());

		String response = request(info, PUT, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK", JsonUtils.toJson(request));
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		assertEquals(request.getName(), restRole.getName());
		assertEquals(extraRole.getUuid(), restRole.getUuid());

		// Check that the extra role was updated as expected
		Role reloadedRole = roleService.findByUUID(extraRole.getUuid());
		assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
	}

	@Test
	public void testUpdateOwnRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Role role = info.getRole();

		RoleUpdateRequest restRole = new RoleUpdateRequest();
		restRole.setName("renamed role");

		String response = request(info, PUT, "/api/v1/roles/" + role.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restRole));
		String json = "?";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// Check that the role was updated
		Role reloadedRole = roleService.findByUUID(role.getUuid());
		assertEquals(restRole.getName(), reloadedRole.getName());

	}

	// Delete tests

	@Test
	public void testDeleteRoleByUUID() throws Exception {
		RoleRoot roleRoot = data().getMeshRoot().getRoleRoot();
		Role extraRole = roleRoot.create("extra role");
		info.getGroup().addRole(extraRole);
		info.getRole().addPermissions(extraRole, DELETE_PERM);

		String response = request(info, DELETE, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK");
		expectMessageResponse("role_deleted", response, extraRole.getUuid());
		assertNull("The user should have been deleted", roleService.findByUUID(extraRole.getUuid()));
	}

	@Test
	public void testDeleteRoleByUUIDWithMissingPermission() throws Exception {
		String response = request(info, DELETE, "/api/v1/roles/" + info.getRole().getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, info.getRole().getUuid());
		assertNotNull("The role should not have been deleted", roleService.findByUUID(info.getRole().getUuid()));
	}

}
