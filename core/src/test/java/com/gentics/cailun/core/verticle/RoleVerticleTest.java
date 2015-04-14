package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.rest.role.request.RoleCreateRequest;
import com.gentics.cailun.core.rest.role.request.RoleUpdateRequest;
import com.gentics.cailun.core.rest.role.response.RoleListResponse;
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.demo.UserInfo;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

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

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"new_role\",\"perms\":[],\"groups\":[{\"uuid\":\"uuid-value\",\"name\":\"joe1_group\",\"roles\":[],\"users\":[],\"perms\":[]}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testCreateDeleteRole() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(info.getGroup().getUuid());

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 200, "OK", requestJson);
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		test.assertRole(request, restRole);

		response = request(info, HttpMethod.DELETE, "/api/v1/roles/" + restRole.getUuid(), 200, "OK");
		expectMessageResponse("role_deleted", response, restRole.getUuid());

	}

	@Test
	public void testCreateRoleWithNoPermissionOnGroup() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(info.getGroup().getUuid());

		// Add needed permission to group
		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), info.getGroup(), PermissionType.CREATE);
			// roleService.revokePermission(info.getRole(), data().getCaiLunRoot().getRoleRoot(), PermissionType.CREATE);
			tx.success();
		}

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 403, "Forbidden", requestJson);
		expectMessageResponse("error_missing_perm", response, info.getGroup().getUuid());
	}

	@Test
	public void testCreateRoleWithBogusJson() throws Exception {

		String requestJson = "bogus text";
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		expectMessageResponse("error_parse_request_json_error", response);
	}

	@Test
	public void testCreateRoleWithNoGroupId() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		expectMessageResponse("role_missing_parentgroup_field", response);

	}

	@Test
	public void testCreateRoleWithNoName() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setGroupUuid(info.getGroup().getUuid());

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		expectMessageResponse("error_name_must_be_set", response);
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = info.getRole();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());

		roleService.addPermission(info.getRole(), info.getRole(), PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + role.getUuid(), 200, "OK");
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		test.assertRole(role, restRole);
	}

	@Test
	public void testReadExtraRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();

		Role extraRole = new Role("extra role");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);

		info.getGroup().addRole(extraRole);
		groupService.save(info.getGroup());

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());

		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK");
		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		assertEquals(extraRole.getUuid(), restRole.getUuid());
		assertEquals(extraRole.getName(), restRole.getName());
	}

	@Test
	public void testReadExtraRoleByUUIDWithMissingPermission() throws Exception {
		UserInfo info = data().getUserInfo();

		Role extraRole = new Role("extra role");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);

		info.getGroup().addRole(extraRole);
		groupService.save(info.getGroup());

		assertNotNull("The UUID of the role must not be null.", extraRole.getUuid());

		// Assign no read permission to the group
		roleService.addPermission(info.getRole(), extraRole, PermissionType.UPDATE);

		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + extraRole.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, extraRole.getUuid());
	}

	@Test
	public void testReadOwnRoleByUUIDWithMissingPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = info.getRole();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());

		roleService.addPermission(info.getRole(), info.getRole(), PermissionType.DELETE);

		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + role.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, role.getUuid());

	}

	@Test
	public void testReadRoles() throws Exception {

		Role noPermRole = new Role("no_perm_role");
		final int nRoles = 21;
		try (Transaction tx = graphDb.beginTx()) {

			roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.READ);

			// Create and save some roles
			for (int i = 0; i < nRoles; i++) {
				Role extraRole = new Role("extra role " + i);
				extraRole = roleService.save(extraRole);
				info.getGroup().addRole(extraRole);
				groupService.save(info.getGroup());
				roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
			}

			// Role with no permission

			noPermRole = roleService.save(noPermRole);
			info.getGroup().addRole(noPermRole);
			tx.success();
		}
		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/roles/", 200, "OK");
		RoleListResponse restResponse = JsonUtils.readValue(response, RoleListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		assertEquals(21, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + 1, 200, "OK");
		restResponse = JsonUtils.readValue(response, RoleListResponse.class);
		assertEquals(perPage, restResponse.getData().size());

		// created roles + test data role
		// TODO fix this assertion. Actually we would need to add 1 since the own role must also be included in the list
		int totalRoles = nRoles + data().getTotalRoles() + 1;
		int totalPages = (int) Math.ceil(totalRoles / (double) perPage);
		assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The total pages could does not match. We expect {" + totalRoles + "} total roles and {" + perPage
				+ "} roles per page. Thus we expect {" + totalPages + "} pages", totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		assertEquals(totalRoles, restResponse.getMetainfo().getTotalCount());

		List<RoleResponse> allRoles = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, RoleListResponse.class);
			allRoles.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

		// Verify that extra role is not part of the response
		final String noPermRoleName = noPermRole.getName();
		List<RoleResponse> filteredUserList = allRoles.parallelStream().filter(restRole -> restRole.getName().equals(noPermRoleName))
				.collect(Collectors.toList());
		assertTrue("Extra role should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":6,\"total_count\":142}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	// Update tests

	@Test
	public void testUpdateRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Role extraRole = new Role("extra role");
		try (Transaction tx = graphDb.beginTx()) {
			roleService.save(extraRole);
			info.getGroup().addRole(extraRole);
			roleService.addPermission(info.getRole(), extraRole, PermissionType.UPDATE);
			tx.success();
		}
		extraRole = roleService.reload(extraRole);
		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");
		request.setUuid(extraRole.getUuid());

		String response = request(info, HttpMethod.PUT, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK", JsonUtils.toJson(request));
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

		RoleResponse restRole = new RoleResponse();
		restRole.setName("renamed role");

		String response = request(info, HttpMethod.PUT, "/api/v1/roles/" + role.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restRole));
		String json = "?";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// Check that the role was updated
		Role reloadedRole = roleService.findByUUID(role.getUuid());
		assertEquals(restRole.getName(), reloadedRole.getName());

	}

	// Delete tests

	@Test
	public void testDeleteRoleByUUID() throws Exception {
		roleService.addPermission(info.getRole(), info.getRole(), PermissionType.DELETE);
		String response = request(info, HttpMethod.DELETE, "/api/v1/roles/" + info.getRole().getUuid(), 200, "OK");
		String json = "{\"message\":\"Role with uuid \\\"" + info.getRole().getUuid() + "\\\" was deleted.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", roleService.findByUUID(info.getRole().getUuid()));
	}

	@Test
	public void testDeleteRoleByUUIDWithMissingPermission() throws Exception {
		roleService.addPermission(info.getRole(), info.getRole(), PermissionType.READ);
		String response = request(info, HttpMethod.DELETE, "/api/v1/roles/" + info.getRole().getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, info.getRole().getUuid());
		assertNotNull("The role should not have been deleted", roleService.findByUUID(info.getRole().getUuid()));
	}

}
