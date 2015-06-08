package com.gentics.mesh.core.verticle.group;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import java.util.Iterator;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.data.service.UserService;
import com.gentics.mesh.core.rest.group.response.GroupResponse;
import com.gentics.mesh.core.rest.user.response.UserListResponse;
import com.gentics.mesh.core.rest.user.response.UserResponse;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.DataHelper;
import com.gentics.mesh.util.JsonUtils;

public class GroupUserVerticleTest

extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Autowired
	private GroupService groupService;

	@Autowired
	private UserService userService;

	@Autowired
	private DataHelper helper;

	@Override
	public AbstractRestVerticle getVerticle() {
		return groupsVerticle;
	}

	// Group User Testcases - PUT / Add

	@Test
	public void testGetUsersByGroup() throws Exception {
		User extraUser = userService.create("extraUser");
//		try (Transaction tx = graphDb.beginTx()) {
			extraUser = userService.save(extraUser);
			info.getGroup().addUser(extraUser);
			groupService.save(info.getGroup());
			roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);
//			tx.success();
//		}

		String uuid = info.getGroup().getUuid();
		String response = request(info, HttpMethod.GET, "/api/v1/groups/" + uuid + "/users", 200, "OK");
		UserListResponse userList = JsonUtils.readValue(response, UserListResponse.class);
		assertEquals(2, userList.getMetainfo().getTotalCount());
		assertEquals(2, userList.getData().size());
		Iterator<UserResponse> userIt = userList.getData().iterator();
		UserResponse userB = userIt.next();
		UserResponse userA = userIt.next();
		assertEquals(info.getUser().getUuid(), userA.getUuid());
		assertEquals(extraUser.getUuid(), userB.getUuid());
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {

		User extraUser = helper.addUser("extraUser", info.getRole(), PermissionType.READ);
		String response = request(info, HttpMethod.POST, "/api/v1/groups/bogus/users/" + extraUser.getUuid(), 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");

	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		Group group = info.getGroup();

		User extraUser = helper.addUser("extraUser", info.getRole(), PermissionType.READ);


		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 200, "OK");
		GroupResponse restGroup = JsonUtils.readValue(response, GroupResponse.class);
		test.assertGroup(group, restGroup);

		group = groupService.reload(group);
		assertTrue("User should be member of the group.", group.hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		Group group = info.getGroup();

		User extraUser =  userService.create("extraUser");
//		try (Transaction tx = graphDb.beginTx()) {
			extraUser = userService.save(extraUser);
			roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);
			roleService.revokePermission(info.getRole(), group, PermissionType.UPDATE);
//			tx.success();
//		}

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, group.getUuid());

		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		Group group = info.getGroup();

		User extraUser = userService.create("extraUser");
//		try (Transaction tx = graphDb.beginTx()) {
			extraUser = userService.save(extraUser);
			roleService.addPermission(info.getRole(), extraUser, PermissionType.DELETE);
//			tx.success();
//		}

		String response = request(info, HttpMethod.POST, "/api/v1/groups/" + group.getUuid() + "/users/" + extraUser.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, extraUser.getUuid());

		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

//		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), group, PermissionType.UPDATE);
//			tx.success();
//		}

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, group.getUuid());

		group = groupService.reload(group);
		assertTrue("User should still be a member of the group.", group.hasUser(user));
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/" + user.getUuid(), 200, "OK");
		GroupResponse restGroup = JsonUtils.readValue(response, GroupResponse.class);
		test.assertGroup(group, restGroup);

		group = groupService.reload(group);
		assertFalse("User should not be member of the group.", group.hasUser(user));
	}

	@Test
	@Ignore("Not yet implemented")
	public void testRemoveSameUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromLastGroupWithPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

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

		String response = request(info, HttpMethod.DELETE, "/api/v1/groups/" + group.getUuid() + "/users/bogus", 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");

		group = groupService.reload(group);
		assertTrue("User should still be member of the group.", group.hasUser(user));
	}
}
