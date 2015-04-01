package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
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
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.UserInfo;
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
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"new_role\",\"groups\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\"}]}";
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
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"new_role\",\"groups\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\"}]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		RoleResponse restRole = JsonUtils.readValue(response, RoleResponse.class);
		response = request(info, HttpMethod.DELETE, "/api/v1/roles/" + restRole.getUUID(), 200, "OK");
		json = "{\"message\":\"Role with uuid \\\"" + restRole.getUUID() + "\\\" was deleted.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testCreateRoleWithNoPermission() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");
		request.setGroupUuid(info.getGroup().getUuid());

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.READ);
		// No Update permission of the parent group

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 403, "Forbidden", requestJson);
		String json = "{\"message\":\"Missing permission on object {" + info.getGroup().getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testCreateRoleWithBogusJson() throws Exception {

		String requestJson = "bogus text";
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"Could not parse request json.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testCreateRoleWithNoGroupId() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setName("new_role");

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"No parent group was specified for the role. Please set a parent group uuid.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testCreateRoleWithNoName() throws Exception {
		RoleCreateRequest request = new RoleCreateRequest();
		request.setGroupUuid(info.getGroup().getUuid());

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"The name must be set.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Read tests

	@Test
	public void testReadOwnRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Role role = info.getRole();
		assertNotNull("The UUID of the role must not be null.", role.getUuid());

		roleService.addPermission(info.getRole(), info.getRole(), PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + role.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_role\",\"groups\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\"}]}";
		assertEqualsSanitizedJson("The response does not match.", json, response);
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
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"extra role\",\"groups\":[{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\"}]}";
		assertEqualsSanitizedJson("The response does not match.", json, response);
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

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.READ);

		// Create and save some roles
		final int nRoles = 142;
		for (int i = 0; i < nRoles; i++) {
			Role extraRole = new Role("extra role " + i);
			extraRole = roleService.save(extraRole);
			info.getGroup().addRole(extraRole);
			groupService.save(info.getGroup());
			roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		}

		// Role with no permission
		Role noPermRole = new Role("no_perm_role");
		roleService.save(noPermRole);
		info.getGroup().addRole(noPermRole);

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/roles/", 200, "OK");
		RoleListResponse restResponse = JsonUtils.readValue(response, RoleListResponse.class);
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, RoleListResponse.class);
		Assert.assertEquals(perPage, restResponse.getData().size());

		// created roles + test data role
		// TODO fix this assertion. Actually we would need to add 1 since the own role must also be included in the list
		int totalRoles = nRoles;
		int totalPages = (int) Math.ceil(totalRoles / (double)perPage);
		Assert.assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		Assert.assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals("The total pages could does not match. We expect {" + totalRoles + "} total roles and {" + perPage
				+ "} roles per page. Thus we expect {" + totalPages + "} pages", totalPages, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(totalRoles, restResponse.getMetainfo().getTotalCount());

		List<RoleResponse> allRoles = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/roles/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, RoleListResponse.class);
			allRoles.addAll(restResponse.getData());
		}
		Assert.assertEquals("Somehow not all roles were loaded when loading all pages.", totalRoles, allRoles.size());

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
		roleService.save(extraRole);
		info.getGroup().addRole(extraRole);
		extraRole = roleService.reload(extraRole);

		roleService.addPermission(info.getRole(), extraRole, PermissionType.UPDATE);

		RoleUpdateRequest request = new RoleUpdateRequest();
		request.setName("renamed role");
		request.setUuid(extraRole.getUuid());

		String response = request(info, HttpMethod.PUT, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"renamed role\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// Check that the extra role was updated as expected
		Role reloadedRole = roleService.findByUUID(extraRole.getUuid());
		Assert.assertEquals("The role should have been renamed", request.getName(), reloadedRole.getName());
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
		Assert.assertEquals(restRole.getName(), reloadedRole.getName());

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

	// Role Group Testcases - PUT / Add

	// @Test
	// public void testAddRoleToGroupWithPerm() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testAddRoleToGroupWithoutPerm() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testAddRoleToGroupWithBogusUUID() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testAddRoleToGroupWithoutRoleReadPerm() {
	// fail("Not yet implemented");
	// }
	//
	// // Role Group Testcases - DELETE / Remove
	//
	// @Test
	// public void testRemoveRoleFromGroupWithPerm() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testRemoveRoleFromGroupWithoutPerm() {
	//
	// }
	//
	// @Test
	// public void testRemoveRoleFromGroupWithBogusGroupID() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testRemoveRoleFromLastGroup() {
	// fail("Not yet implemented");
	// }
	//
	// @Test
	// public void testRemoveRoleFromGroupWithoutRoleReadPerm() {
	// fail("Not yet implemented");
	// }
}
