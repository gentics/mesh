package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;

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
import com.gentics.cailun.core.rest.role.response.RoleResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;
import com.gentics.cailun.test.UserInfo;

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
		RoleResponse newRole = new RoleResponse();
		newRole.setName("new_role");

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		String requestJson = new ObjectMapper().writeValueAsString(newRole);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"new_role\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testCreateRoleWithNoPermission() throws Exception {
		RoleResponse newRole = new RoleResponse();
		newRole.setName("new_role");

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.READ);
		// No Update permission of the parent group

		String requestJson = new ObjectMapper().writeValueAsString(newRole);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 403, "Forbidden", requestJson);
		String json = "error";
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
	public void testCreateRoleWithNoName() throws Exception {
		RoleResponse newRole = new RoleResponse();

		// Add needed permission to group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		String requestJson = new ObjectMapper().writeValueAsString(newRole);
		String response = request(info, HttpMethod.POST, "/api/v1/roles/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"The name for the role was not specified.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Read tests

	@Test
	public void testReadRoleByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Role adminRole = info.getRole();
		assertNotNull("The UUID of the role must not be null.", adminRole.getUuid());

		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + adminRole.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_role\"}";
		TestUtil.assertEqualsSanitizedJson(json, response, RoleResponse.class);
	}

	@Test
	public void testReadRoleByName() throws Exception {
		UserInfo info = data().getUserInfo();
		Role adminRole = info.getRole();
		assertNotNull("The UUID of the role must not be null.", adminRole.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/roles/" + adminRole.getName(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_role\"}";
		TestUtil.assertEqualsSanitizedJson(json, response, RoleResponse.class);
	}

	@Test
	public void testReadRoles() throws Exception {
		// Role with no permission
		Role extraRole1 = new Role("extra role");
		roleService.save(extraRole1);
		info.getGroup().addRole(extraRole1);

		// Extra role with permission
		Role extraRole2 = new Role("extra role 2");
		roleService.save(extraRole2);
		info.getGroup().addRole(extraRole2);
		groupService.save(info.getGroup());

		roleService.addPermission(info.getRole(), extraRole2, PermissionType.READ);
		// Don't grant read permission on extraRole1

		String response = request(info, HttpMethod.GET, "/api/v1/roles/", 200, "OK");
		// TODO the own role should also be included here
		String json = "{\"extra role 2\":{\"uuid\":\"uuid-value\",\"name\":\"extra role 2\"}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	// Update tests

	@Test
	public void testUpdateRole() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		Role extraRole = new Role("extra role");
		roleService.save(extraRole);
		info.getGroup().addRole(extraRole);
		extraRole = roleService.reload(extraRole);

		roleService.addPermission(info.getRole(), extraRole, PermissionType.UPDATE);

		RoleResponse restRole = new RoleResponse();
		restRole.setName("renamed role");

		String response = request(info, HttpMethod.PUT, "/api/v1/roles/" + extraRole.getUuid(), 200, "OK",
				new ObjectMapper().writeValueAsString(restRole));
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// Check that the extra role was updated as expected
		Role reloadedRole = roleService.findByUUID(extraRole.getUuid());
		Assert.assertEquals(restRole.getName(), reloadedRole.getName());
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
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", roleService.findByUUID(info.getRole().getUuid()));
	}

	@Test
	public void testDeleteRoleByName() throws Exception {
		roleService.addPermission(info.getRole(), info.getRole(), PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/roles/" + info.getRole().getName(), 200, "OK");
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", roleService.findByUUID(info.getRole().getUuid()));
	}

}
