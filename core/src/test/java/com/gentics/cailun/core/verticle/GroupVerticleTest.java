package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertFalse;
import static com.gentics.cailun.util.RestAssert.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.CaiLunPermission;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.Role;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.group.request.GroupCreateRequest;
import com.gentics.cailun.core.rest.group.request.GroupUpdateRequest;
import com.gentics.cailun.core.rest.group.response.GroupListResponse;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
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
	@Test
	public void testCreateGroup() throws Exception {

		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);
		String requestJson = JsonUtils.toJson(request);

		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(info.getRole(), data().getCaiLunRoot().getGroupRoot(), PermissionType.CREATE);
			tx.success();
		}

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\",\"roles\":[],\"users\":[],\"perms\":[]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		assertNotNull("Group should have been created.", groupService.findByName(name));
	}

	@Test
	public void testCreateDeleteGroup() throws Exception {

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		// Create the group
		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);

		String requestJson = JsonUtils.toJson(request);
		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"test12345\",\"roles\":[],\"users\":[],\"perms\":[]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("Group should have been created.", groupService.findByName(name));

		// Now delete the group
		GroupResponse restGroup = JsonUtils.readValue(response, GroupResponse.class);
		response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + restGroup.getUuid(), 200, "OK", requestJson);

		expectMessageResponse("group_deleted", response, restGroup.getUuid());
	}

	@Test
	public void testCreateGroupWithMissingName() throws Exception {

		GroupCreateRequest request = new GroupCreateRequest();

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);
		String requestJson = JsonUtils.toJson(request);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 400, "Bad Request", requestJson);
		expectMessageResponse("error_name_must_be_set", response);

	}

	@Test
	public void testCreateGroupWithNoPerm() throws Exception {

		final String name = "test12345";
		GroupCreateRequest request = new GroupCreateRequest();
		request.setName(name);
		String requestJson = JsonUtils.toJson(request);

		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), data().getCaiLunRoot().getGroupRoot(), PermissionType.CREATE);
			tx.success();
		}

		assertFalse("The create permission to the groups root node should have been revoked.",
				userService.isPermitted(info.getUser().getId(), new CaiLunPermission(data().getCaiLunRoot().getGroupRoot(), PermissionType.CREATE)));

		String response = request(info, HttpMethod.POST, "/api/v1/groups/", 403, "Forbidden", requestJson);
		expectMessageResponse("error_missing_perm", response, data().getCaiLunRoot().getGroupRoot().getUuid());

		assertNull(groupService.findByName(name));
	}

	// Read Tests

	@Test
	public void testReadGroups() throws Exception {

		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.READ);

		// Create and save some groups
		final int nGroups = 21;
		try (Transaction tx = graphDb.beginTx()) {

			for (int i = 0; i < nGroups; i++) {
				Group group = new Group("group_" + i);
				group = groupService.save(group);
				roleService.addPermission(info.getRole(), group, PermissionType.READ);
			}
			tx.success();
		}

		Group extraGroupWithNoPerm = new Group("no_perm_group");
		try (Transaction tx = graphDb.beginTx()) {
			// Don't grant permissions to extra group
			extraGroupWithNoPerm = groupService.save(extraGroupWithNoPerm);
			tx.success();
		}

		int totalGroups = nGroups + data().getTotalGroups();

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/groups/", 200, "OK");
		GroupListResponse restResponse = JsonUtils.readValue(response, GroupListResponse.class);
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(totalGroups, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/groups/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, GroupListResponse.class);
		Assert.assertEquals(perPage, restResponse.getData().size());

		// created groups + test data group
		int totalPages = (int) Math.ceil(totalGroups / (double) perPage);
		Assert.assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		Assert.assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals("We expect {" + totalGroups + "} groups and with a paging size of {" + perPage + "} exactly {" + totalPages + "} pages.",
				totalPages, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(totalGroups, restResponse.getMetainfo().getTotalCount());

		List<GroupResponse> allGroups = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/groups/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, GroupListResponse.class);
			allGroups.addAll(restResponse.getData());
		}
		Assert.assertEquals("Somehow not all groups were loaded when loading all pages.", totalGroups, allGroups.size());

		// Verify that extra group is not part of the response
		final String extraGroupName = extraGroupWithNoPerm.getName();
		List<GroupResponse> filteredUserList = allGroups.parallelStream().filter(restGroup -> restGroup.getName().equals(extraGroupName))
				.collect(Collectors.toList());
		assertTrue("Extra group should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/groups/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/groups/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/groups/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/groups/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":3,\"total_count\":65}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		Group group = info.getGroup();

		// Add a child group to group of the user
		Group subGroup = new Group("sub group");
		// group.addGroup(subGroup);
		subGroup = groupService.save(subGroup);
		group = groupService.save(group);

		roleService.addPermission(info.getRole(), group, PermissionType.READ);

		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		test.assertGroup(group, JsonUtils.readValue(response, GroupResponse.class));

	}

	@Test
	public void testReadGroupByUUIDWithNoPermission() throws Exception {
		Group group = info.getGroup();

		// Add a child group to group of the user
		Group subGroup = new Group("sub group");
		// group.addGroup(subGroup);
		subGroup = groupService.save(subGroup);
		group = groupService.save(group);

		roleService.addPermission(info.getRole(), group, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), group, PermissionType.CREATE);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		roleService.revokePermission(info.getRole(), group, PermissionType.READ);

		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + group.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, group.getUuid());
	}

	@Test
	public void testReadGroupWithBogusUUID() throws Exception {
		final String bogusUuid = "sadgasdasdg";
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + bogusUuid, 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, bogusUuid);
	}

	// Update Tests

	@Test
	public void testUpdateGroup() throws HttpStatusCodeErrorException, Exception {
		Group group = info.getGroup();

		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
		final String name = "New Name";
		GroupUpdateRequest request = new GroupUpdateRequest();
		request.setUuid(group.getUuid());
		request.setName(name);

		String response = request(info, HttpMethod.PUT, "/api/v1/groups/" + group.getUuid(), 200, "OK", JsonUtils.toJson(request));
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"New Name\",\"roles\":[\"joe1_role\"],\"users\":[\"joe1\"],\"perms\":[]}";
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
		String json = "{\"message\":\"The name must be set.\"}";
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
		String json = "{\"message\":\"Object with uuid \\\"bogus\\\" could not be found.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		Group reloadedGroup = groupService.reload(group);
		Assert.assertEquals("The group should not have been updated", group.getName(), reloadedGroup.getName());
	}

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
		roleService.revokePermission(info.getRole(), group, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permissions on object \\\"" + group.getUuid() + "\\\"\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The group should not have been deleted", groupService.findByUUID(group.getUuid()));
	}

	// Group Role Testcases - PUT / Add

	@Test
	public void testAddRoleToGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		Role extraRole = new Role("extraRole");
		extraRole = roleService.save(extraRole);
		extraRole = roleService.reload(extraRole);

		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"joe1_group\",\"roles\":[\"joe1_role\",\"extraRole\"],\"users\":[\"joe1\"],\"perms\":[]}";
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
		roleService.revokePermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permissions on object \\\"" + group.getUuid() + "\\\"\"}";
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
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"],\"perms\":[]}";
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

		roleService.addPermission(info.getRole(), extraRole, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);
		roleService.revokePermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/roles/" + extraRole.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permissions on object \\\"" + group.getUuid() + "\\\"\"}";
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
		String json = "{\"message\":\"Object with uuid \\\"bogus\\\" could not be found.\"}";
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
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"joe1_group\",\"roles\":[\"joe1_role\"],\"users\":[\"extraUser\",\"joe1\"],\"perms\":[]}";
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

		roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);
		roleService.revokePermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permissions on object \\\"" + group.getUuid() + "\\\"\"}";
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
		String json = "{\"message\":\"Missing permissions on object \\\"" + extraUser.getUuid() + "\\\"\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.READ);
		roleService.revokePermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permissions on object \\\"" + group.getUuid() + "\\\"\"}";
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
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[],\"perms\":[]}";
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

	// @Test
	// public void testAddGroupToGroupWithPerm() throws Exception {
	// Group group = info.getGroup();
	//
	// Group extraGroup = new Group("extraGroup");
	// extraGroup = groupService.save(extraGroup);
	// extraGroup = groupService.reload(extraGroup);
	//
	// // TODO check with cp whether perms are ok that way.
	// roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
	// roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
	//
	// String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 200, "OK");
	// String json =
	// "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"groups\":[\"extraGroup\"],\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// group = groupService.reload(group);
	// assertTrue("Group should be child of the group.", group.hasGroup(extraGroup));
	// }

	// @Test
	// public void testAddGroupToGroupWithoutGroupPerm() throws Exception {
	// Group group = info.getGroup();
	//
	// Group extraGroup = new Group("extraGroup");
	// extraGroup = groupService.save(extraGroup);
	// extraGroup = groupService.reload(extraGroup);
	//
	// // TODO check with cp whether perms are ok that way.
	// roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
	// roleService.addPermission(info.getRole(), group, PermissionType.READ);
	//
	// String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 403, "Forbidden");
	// String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// group = groupService.reload(group);
	// assertFalse("Group should not be a child of the group.", group.hasGroup(extraGroup));
	// }

	// Group SubGroup Testcases - DELETE / Remove

	// @Test
	// public void testRemoveGroupFromGroupWithPerm() throws Exception {
	// Group group = info.getGroup();
	//
	// Group extraGroup = new Group("extraGroup");
	// extraGroup = groupService.save(extraGroup);
	// extraGroup = groupService.reload(extraGroup);
	// group.addGroup(extraGroup);
	// group = groupService.save(group);
	//
	// // TODO check with cp whether perms are ok that way.
	// roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
	// roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);
	//
	// String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 200, "OK");
	// String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// group = groupService.reload(group);
	// assertFalse("Group should no longer be a child of the group.", group.hasGroup(extraGroup));
	// }
	//
	// @Test
	// public void testRemoveGroupFromGroupWithoutPerm() throws Exception {
	// Group group = info.getGroup();
	//
	// Group extraGroup = new Group("extraGroup");
	// extraGroup = groupService.save(extraGroup);
	// extraGroup = groupService.reload(extraGroup);
	// group.addGroup(extraGroup);
	// group = groupService.save(group);
	//
	// // TODO check with cp whether perms are ok that way.
	// roleService.addPermission(info.getRole(), extraGroup, PermissionType.READ);
	// roleService.addPermission(info.getRole(), group, PermissionType.READ);
	//
	// String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/groups/" + extraGroup.getUuid(), 403, "Forbidden");
	// String json = "{\"message\":\"Missing permission on object {" + group.getUuid() + "}\"}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// group = groupService.reload(group);
	// assertTrue("Group should still be a child of the group.", group.hasGroup(extraGroup));
	// }

}
