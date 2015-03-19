package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class GroupVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserService userService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	// Create Tests

	// Read Tests

	// Update Tests

	// Delete Tests

	@Test
	public void testDeleteGroupByUUID() throws Exception {
		Group group = info.getGroup();
		assertNotNull(group.getUuid());

		roleService.addPermission(info.getRole(), group, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		String json = "{\"message\":\"Group with uuid \\\"" + group.getUuid() + "\\\" was deleted.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The group should have been deleted", groupService.findByUUID(group.getUuid()));
	}

	@Test
	public void testDeleteGroupByUUIDWithMissingPermission() throws Exception {
		Group group = info.getGroup();
		assertNotNull(group.getUuid());

		roleService.addPermission(info.getRole(), group, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), group, PermissionType.CREATE);
		// Don't allow delete

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The group should not have been deleted", groupService.findByUUID(group.getUuid()));
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		Group group = info.getGroup();

		// Add a child group to group of the user
		Group subGroup = new Group("sub group");
		group.addGroup(subGroup);
		subGroup = groupService.save(subGroup);
		group = groupService.save(group);

		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"groups\":[\"sub group\"],\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		assertEqualsSanitizedJson("The response does not match.", json, response);
	}

	@Test
	public void testCreateGroup() throws Exception {

		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);
		request.setGroupUuid(info.getGroup().getUuid());

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull("Group should have been created.", groupService.findByName(name));
	}

	@Test
	public void testCreateGroupWithMissingName() throws Exception {

		GroupCreateRequest request = new GroupCreateRequest();
		request.setGroupUuid(info.getGroup().getUuid());

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"Name can't be empty or null\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testCreateGroupWithMissingGroupUuid() throws Exception {

		GroupCreateRequest request = new GroupCreateRequest();
		final String name = "somenameforrequest";
		request.setName(name);

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"The group uuid field has not been set. Parent group must be specified.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("Group should not have been created.", groupService.findByName(name));

	}

	@Test
	public void testCreateGroupWithNoPerm() throws Exception {

		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);
		request.setGroupUuid(info.getGroup().getUuid());

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.READ);
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.DELETE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 403, "Forbidden", requestJson);
		String json = "{\"message\":\"Missing permission on object {" + info.getGroup().getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNull(groupService.findByName(name));
	}

	@Test
	public void testUpdateGroup() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, HttpMethod.PUT, "/api/v1/groups/" + group.getUuid(), 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"New Name\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Group reloadedGroup = groupService.reload(group);

		Assert.assertEquals("The group should have been updated", name, reloadedGroup.getName());
	}

	@Test
	public void testUpdateGroupWithEmptyName() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		final String name = "";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, HttpMethod.PUT, "/api/v1/groups/" + group.getUuid(), 400, "Bad Request", JsonUtils.toJson(request));
		String json = "{\"message\":\"Name can't be empty or null\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Group reloadedGroup = groupService.reload(group);
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

	@Test
	public void testUpdateGroupWithConflictingName() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		final String alreadyUsedName = "extraGroup";

		// Create a group which occupies the name
		Group extraGroup = new Group(alreadyUsedName);
		extraGroup = groupService.save(extraGroup);
		extraGroup = groupService.reload(extraGroup);

		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(alreadyUsedName);

		String response = request(info, HttpMethod.PUT, "/api/v1/groups/" + group.getUuid(), 400, "Bad Request", JsonUtils.toJson(request));
		String json = "{\"message\":\"Group name {extraGroup} is already taken. Choose a different one.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Group reloadedGroup = groupService.reload(group);
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

	@Test
	public void testUpdateGroupWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, HttpMethod.PUT, "/api/v1/groups/bogus", 404, "Not Found", JsonUtils.toJson(request));
		String json = "{\"message\":\"Group not found for uuid \\\"bogus\\\".\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Group reloadedGroup = groupService.reload(group);
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

	// Group Role Testcases - PUT / Add

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		Role extraRole = new Role("extraRole");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"extraRole\",\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("Role should be assigned to group.", group.hasRole(extraRole));
	}

	@Test
	public void testAddRoleToGroupWithoutPermOnGroup() throws Exception {
		Group group = info.getGroup();

		Role extraRole = new Role("extraRole");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("Role should not be assigned to group.", group.hasRole(extraRole));
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() throws Exception {
		Group group = info.getGroup();

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/roles/bogus", 404, "Not Found");
		String json = "{\"message\":\"Object with uuid \\\"bogus\\\" could not be found.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	// Group Role Testcases - DELETE / Remove

	@Test
	public void testRemoveRoleFromGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		Role extraRole = new Role("extraRole");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);
		group.addRole(extraRole);
		group = groupService.save(group);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("Role should now no longer be assigned to group.", group.hasRole(extraRole));
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() throws Exception {
		Group group = info.getGroup();

		Role extraRole = new Role("extraRole");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);
		group.addRole(extraRole);
		group = groupService.save(group);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("Role should be stil assigned to group.", group.hasRole(extraRole));
	}

	// Group User Testcases - PUT / Add

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {

		User extraUser = new User("extraUser");
		extraUser = userService.save(extraUser);
		extraUser = userService.reload(extraUser);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/bogus/users/" + extraUser.getUuid(), 404, "Not Found");
		String json = "{\"message\":\"Group not found for uuid \\\"bogus\\\".\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		User extraUser = new User("extraUser");
		extraUser = userService.save(extraUser);
		extraUser = userService.reload(extraUser);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\",\"extraUser\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("User should be member of the group.", group.hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		Group group = info.getGroup();

		User extraUser = new User("extraUser");
		extraUser = userService.save(extraUser);
		extraUser = userService.reload(extraUser);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		Group group = info.getGroup();

		User extraUser = new User("extraUser");
		extraUser = userService.save(extraUser);
		extraUser = userService.reload(extraUser);

		// TODO check with cp whether perms are ok that way.
		// Extra user cannot be read
		roleService.addPermission(info.getRole(), extraUser, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + extraUser.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("User should still be a member of the group.", group.hasUser(user));
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(user));
	}

	// @Test
	// public void testRemoveSameUserFromGroupWithPerm() {
	// fail("Not yet implemented");
	// }

	@Test
	public void testRemoveUserFromLastGroupWithPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 400, "Bad Request");
		String json = "error-user-last-group";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("User should still be member of the group.", group.hasUser(user));
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/bogus", 404, "Not Found");
		String json = "{\"message\":\"Object with uuid \\\"bogus\\\" could not be found.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("User should still be member of the group.", group.hasUser(user));
	}

	// Group SubGroup Testcases - PUT / Add

	@Test
	public void testAddGroupToGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		Group extraGroup = new Group("extraGroup");
		extraGroup = groupService.save(extraGroup);
		extraGroup = groupService.reload(extraGroup);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"groups\":[\"extraGroup\"],\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("Group should be child of the group.", group.hasGroup(extraGroup));
	}

	@Test
	public void testAddGroupToGroupWithoutGroupPerm() throws Exception {
		Group group = info.getGroup();

		Group extraGroup = new Group("extraGroup");
		extraGroup = groupService.save(extraGroup);
		extraGroup = groupService.reload(extraGroup);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("Group should not be a child of the group.", group.hasGroup(extraGroup));
	}

	// Group SubGroup Testcases - DELETE / Remove

	@Test
	public void testRemoveGroupFromGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		Group extraGroup = new Group("extraGroup");
		extraGroup = groupService.save(extraGroup);
		extraGroup = groupService.reload(extraGroup);
		group.addGroup(extraGroup);
		group = groupService.save(group);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("Group should no longer be a child of the group.", group.hasGroup(extraGroup));
	}

	@Test
	public void testRemoveGroupFromGroupWithoutPerm() throws Exception {
		Group group = info.getGroup();

		Group extraGroup = new Group("extraGroup");
		extraGroup = groupService.save(extraGroup);
		extraGroup = groupService.reload(extraGroup);
		group.addGroup(extraGroup);
		group = groupService.save(group);

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertTrue("Group should still be a child of the group.", group.hasGroup(extraGroup));
	}

}
