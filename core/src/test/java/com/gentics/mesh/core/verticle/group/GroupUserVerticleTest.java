package com.gentics.mesh.core.verticle.group;

import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
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
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class GroupUserVerticleTest

extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle verticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}
	// Group User Testcases - PUT / Add

	@Test
	public void testGetUsersByGroup() throws Exception {
		String groupUuid;
		String extraUserUuid;
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", group(), user());
			extraUserUuid = extraUser.getUuid();
			role().grantPermissions(extraUser, READ_PERM);
			groupUuid = group().getUuid();
			tx.success();
		}

		Future<UserListResponse> future = getClient().findUsersOfGroup(groupUuid, new PagingInfo());
		latchFor(future);
		assertSuccess(future);

		try (Trx tx = new Trx(db)) {
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
			assertNotNull(map.get(user().getUuid()));
			assertNotNull(map.get(extraUserUuid));
		}
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {
		String userUuid;
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", null, user());
			userUuid = extraUser.getUuid();
			role().grantPermissions(extraUser, READ_PERM);
			tx.success();
		}

		Future<GroupResponse> future = getClient().addUserToGroup("bogus", userUuid);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		User extraUser;
		try (Trx tx = new Trx(db)) {
			Group group = group();
			UserRoot userRoot = meshRoot().getUserRoot();

			extraUser = userRoot.create("extraUser", null, user());
			role().grantPermissions(extraUser, READ_PERM);

			assertFalse("User should not be member of the group.", group.hasUser(extraUser));
			tx.success();
		}

		Future<GroupResponse> future;
		try (Trx tx = new Trx(db)) {
			future = getClient().addUserToGroup(group().getUuid(), extraUser.getUuid());
			latchFor(future);
			assertSuccess(future);
			GroupResponse restGroup = future.result();
			test.assertGroup(group(), restGroup);
		}
		try (Trx tx = new Trx(db)) {
			assertTrue("User should be member of the group.", group().hasUser(extraUser));
		}
	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		String groupUuid;
		String extraUserUuid;
		User extraUser;
		try (Trx tx = new Trx(db)) {
			Group group = group();
			groupUuid = group.getUuid();
			UserRoot userRoot = meshRoot().getUserRoot();
			extraUser = userRoot.create("extraUser", null, user());
			extraUserUuid = extraUser.getUuid();
			role().grantPermissions(extraUser, READ_PERM);
			role().revokePermissions(group, UPDATE_PERM);
			tx.success();
		}

		Future<GroupResponse> future = getClient().addUserToGroup(groupUuid, extraUserUuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", groupUuid);

		try (Trx tx = new Trx(db)) {
			assertFalse("User should not be member of the group.", group().hasUser(extraUser));
		}
	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		User extraUser;
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			extraUser = userRoot.create("extraUser", null, user());
			role().grantPermissions(extraUser, DELETE_PERM);
			tx.success();
		}

		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().addUserToGroup(group().getUuid(), extraUser.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", extraUser.getUuid());
		}
		try (Trx tx = new Trx(db)) {
			assertFalse("User should not be member of the group.", group().hasUser(extraUser));
		}
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		try (Trx tx = new Trx(db)) {
			User user = user();
			Group group = group();
			assertTrue("User should be a member of the group.", group.hasUser(user));
			role().revokePermissions(group, UPDATE_PERM);
			tx.success();
		}

		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().removeUserFromGroup(group().getUuid(), user().getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());
		}

		try (Trx tx = new Trx(db)) {
			assertTrue("User should still be a member of the group.", group().hasUser(user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {

		Future<GroupResponse> future;
		try (Trx tx = new Trx(db)) {
			future = getClient().removeUserFromGroup(group().getUuid(), user().getUuid());
			latchFor(future);
			assertSuccess(future);
		}

		try (Trx tx = new Trx(db)) {
			GroupResponse restGroup = future.result();
			test.assertGroup(group(), restGroup);
			assertFalse("User should not be member of the group.", group().hasUser(user()));
		}
	}

	@Test
	@Ignore("Not yet implemented")
	public void testRemoveSameUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromLastGroupWithPerm() throws Exception {

		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().removeUserFromGroup(group().getUuid(), user().getUuid());
			latchFor(future);
			assertSuccess(future);
		}

		try (Trx tx = new Trx(db)) {
			assertFalse("User should no longer be member of the group.", group().hasUser(user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {

		try (Trx tx = new Trx(db)) {
			Future<GroupResponse> future = getClient().removeUserFromGroup(group().getUuid(), "bogus");
			latchFor(future);
			expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
		try (Trx tx = new Trx(db)) {
			assertTrue("User should still be member of the group.", group().hasUser(user()));
		}
	}
}
