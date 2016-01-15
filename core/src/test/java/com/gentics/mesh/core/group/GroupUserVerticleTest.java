package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
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

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.group.GroupVerticle;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;

public class GroupUserVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private GroupVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}
	// Group User Testcases - PUT / Add

	@Test
	public void testGetUsersByGroup() throws Exception {
		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser", user());
		group().addUser(extraUser);
		String extraUserUuid = extraUser.getUuid();
		role().grantPermissions(extraUser, READ_PERM);
		String groupUuid = group().getUuid();

		Future<UserListResponse> future = getClient().findUsersOfGroup(groupUuid, new PagingParameter());
		latchFor(future);
		assertSuccess(future);

		ListResponse<UserResponse> userList = future.result();
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

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {
		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser", user());
		String userUuid = extraUser.getUuid();
		role().grantPermissions(extraUser, READ_PERM);

		Future<GroupResponse> future = getClient().addUserToGroup("bogus", userUuid);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		Group group = group();
		UserRoot userRoot = meshRoot().getUserRoot();

		User extraUser = userRoot.create("extraUser", user());
		role().grantPermissions(extraUser, READ_PERM);

		assertFalse("User should not be member of the group.", group.hasUser(extraUser));

		Future<GroupResponse> future;
		future = getClient().addUserToGroup(group().getUuid(), extraUser.getUuid());
		latchFor(future);
		assertSuccess(future);
		GroupResponse restGroup = future.result();
		assertThat(restGroup).matches(group());

		assertTrue("User should be member of the group.", group().hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		String groupUuid;
		String extraUserUuid;
		User extraUser;
		Group group = group();
		groupUuid = group.getUuid();
		UserRoot userRoot = meshRoot().getUserRoot();
		extraUser = userRoot.create("extraUser", user());
		extraUserUuid = extraUser.getUuid();
		role().grantPermissions(extraUser, READ_PERM);
		role().revokePermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().addUserToGroup(groupUuid, extraUserUuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", groupUuid);
		assertFalse("User should not be member of the group.", group().hasUser(extraUser));
	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		User extraUser;
		UserRoot userRoot = meshRoot().getUserRoot();
		extraUser = userRoot.create("extraUser", user());
		role().grantPermissions(extraUser, DELETE_PERM);

		Future<GroupResponse> future = getClient().addUserToGroup(group().getUuid(), extraUser.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", extraUser.getUuid());
		assertFalse("User should not be member of the group.", group().hasUser(extraUser));
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		User user = user();
		Group group = group();
		assertTrue("User should be a member of the group.", group.hasUser(user));
		role().revokePermissions(group, UPDATE_PERM);

		Future<GroupResponse> future = getClient().removeUserFromGroup(group().getUuid(), user().getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", group().getUuid());

		assertTrue("User should still be a member of the group.", group().hasUser(user()));
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		Future<GroupResponse> future;
		future = getClient().removeUserFromGroup(group().getUuid(), user().getUuid());
		latchFor(future);
		assertSuccess(future);

		GroupResponse restGroup = future.result();
		assertThat(restGroup).matches(group());
		assertFalse("User should not be member of the group.", group().hasUser(user()));
	}

	@Test
	@Ignore("Not yet implemented")
	public void testRemoveSameUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromLastGroupWithPerm() throws Exception {
		Future<GroupResponse> future = getClient().removeUserFromGroup(group().getUuid(), user().getUuid());
		latchFor(future);
		assertSuccess(future);
		assertFalse("User should no longer be member of the group.", group().hasUser(user()));
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {
		Future<GroupResponse> future = getClient().removeUserFromGroup(group().getUuid(), "bogus");
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
		assertTrue("User should still be member of the group.", group().hasUser(user()));
	}
}
