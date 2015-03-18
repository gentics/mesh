package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.TestUtil;

public class GroupVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Autowired
	private GroupService groupService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	@Test
	public void testReadGroupByUUID() throws Exception {
		Group group = info.getGroup();

		// Add a child group to group of the user
		Group subGroup = new Group("sub group");
		group.addGroup(subGroup);
		subGroup = groupService.save(subGroup);
		group = groupService.save(group);

		assertNotNull("The UUID of the group must not be null.", group.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + group.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"childGroups\":[\"sub group\"],\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"]}";
		TestUtil.assertEqualsSanitizedJson(json, response, GroupResponse.class);
	}

	@Test
	public void testCreateGroup() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteGroupByUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteGroupByName() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateGroup() {
		fail("Not yet implemented");
	}

	// Group Role Testcases - PUT / Add

	@Test
	public void testAddRoleToGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddRoleToGroupWithoutPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddRoleToGroupWithBogusRoleUUID() {
		fail("Not yet implemented");
	}

	// Group Role Testcases - DELETE / Remove

	@Test
	public void testRemoveRoleFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveRoleFromGroupWithoutPerm() {
		fail("Not yet implemented");
	}

	// Group User Testcases - PUT / Add

	@Test
	public void testAddUserToGroupWithNoPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUserToGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUserToGroupWithoutPerm() {
		fail("Not yet implemented");
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		// TODO check with cp whether perms are ok that way.
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), group, PermissionType.UPDATE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 200, "OK");
		String json = "OK";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(user));
	}

	@Test
	public void testRemoveSameUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromLastGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromGroupWithBogusID() {
		fail("Not yet implemented");
	}

	// Group SubGroup Testcases - PUT / Add

	@Test
	public void testAddGroupToGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddGroupToGroupWithoutPerm() {
		fail("Not yet implemented");
	}

	// Group SubGroup Testcases - DELETE / Remove

	@Test
	public void testRemoveGroupToGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveGroupToGroupWithoutPerm() {
		fail("Not yet implemented");
	}

}
