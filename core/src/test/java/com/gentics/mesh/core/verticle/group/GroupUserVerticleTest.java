package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.core.Future;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.GroupVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class GroupUserVerticleTest

extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle groupsVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return groupsVerticle;
	}

	// Group User Testcases - PUT / Add

	@Test
	public void testGetUsersByGroup() throws Exception {
		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser");
		info.getGroup().addUser(extraUser);
		info.getRole().addPermissions(extraUser, READ_PERM);
		String uuid = info.getGroup().getUuid();

		Future<UserListResponse> future = getClient().findUsersOfGroup(uuid, new PagingInfo());
		latchFor(future);
		assertSuccess(future);
		UserListResponse userList = future.result();
		assertEquals(2, userList.getMetainfo().getTotalCount());
		assertEquals(2, userList.getData().size());
		Iterator<UserResponse> userIt = userList.getData().iterator();
		UserResponse userB = userIt.next();
		UserResponse userA = userIt.next();
		Map<String, UserResponse> map = new HashMap<>();
		map.put(userA.getUuid(), userA);
		map.put(userB.getUuid(), userB);
		assertEquals(2, map.size());
		assertNotNull(map.get(info.getUser().getUuid()));
		assertNotNull(map.get(extraUser.getUuid()));
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {
		UserRoot userRoot = meshRoot().getUserRoot();

		User extraUser = userRoot.create("extraUser");
		info.getRole().addPermissions(extraUser, READ_PERM);

		Future<GroupResponse> future = getClient().addUserToGroup("bogus", extraUser.getUuid());
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		Group group = info.getGroup();
		UserRoot userRoot = meshRoot().getUserRoot();

		User extraUser = userRoot.create("extraUser");
		info.getRole().addPermissions(extraUser, READ_PERM);

		assertFalse("User should not be member of the group.", group.hasUser(extraUser));

		Future<GroupResponse> future = getClient().addUserToGroup(group.getUuid(), extraUser.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		test.assertGroup(group, restGroup);
		assertTrue("User should be member of the group.", group.hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		Group group = info.getGroup();
		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser");
		info.getRole().addPermissions(extraUser, READ_PERM);
		info.getRole().revokePermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().addUserToGroup(group.getUuid(), extraUser.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		Group group = info.getGroup();
		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser");
		info.getRole().addPermissions(extraUser, DELETE_PERM);

		Future<GroupResponse> future = getClient().addUserToGroup(group.getUuid(), extraUser.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraUser.getUuid());
		assertFalse("User should not be member of the group.", group.hasUser(extraUser));
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();
		assertTrue("User should be a member of the group.", group.hasUser(user));

		info.getRole().revokePermissions(group, UPDATE_PERM);
		Future<GroupResponse> future = getClient().removeUserFromGroup(group.getUuid(), user.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group.getUuid());

		assertTrue("User should still be a member of the group.", group.hasUser(user));
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		Future<GroupResponse> future = getClient().removeUserFromGroup(group.getUuid(), user.getUuid());
		latchFor(future);
		assertSuccess(future);

		GroupResponse restGroup = future.result();
		test.assertGroup(group, restGroup);
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

		Future<GroupResponse> future = getClient().removeUserFromGroup(group.getUuid(), user.getUuid());
		latchFor(future);
		assertSuccess(future);
		assertFalse("User should no longer be member of the group.", group.hasUser(user));
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {
		User user = info.getUser();
		Group group = info.getGroup();

		Future<GroupResponse> future = getClient().removeUserFromGroup(group.getUuid(), "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
		assertTrue("User should still be member of the group.", group.hasUser(user));
	}
}
