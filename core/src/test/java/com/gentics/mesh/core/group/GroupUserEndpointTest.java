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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class GroupUserEndpointTest extends AbstractRestEndpointTest {

	// Group User Testcases - PUT / Add

	@Test
	public void testGetUsersByGroup() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", user());
			group().addUser(extraUser);
			String extraUserUuid = extraUser.getUuid();
			role().grantPermissions(extraUser, READ_PERM);
			String groupUuid = group().getUuid();

			MeshResponse<UserListResponse> future = client().findUsersOfGroup(groupUuid, new PagingParametersImpl()).invoke();
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
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", user());
			String userUuid = extraUser.getUuid();
			role().grantPermissions(extraUser, READ_PERM);

			MeshResponse<GroupResponse> future = client().addUserToGroup("bogus", userUuid).invoke();
			latchFor(future);
			expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
		}
	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserRoot userRoot = meshRoot().getUserRoot();

			User extraUser = userRoot.create("extraUser", user());
			role().grantPermissions(extraUser, READ_PERM);

			assertFalse("User should not be member of the group.", group().hasUser(extraUser));

			GroupResponse restGroup = call(() -> client().addUserToGroup(group().getUuid(), extraUser.getUuid()));
			assertThat(restGroup).matches(group());
			assertThat(dummySearchProvider).hasStore(User.composeIndexName(), User.composeIndexType(), user().getUuid());
			assertThat(dummySearchProvider).hasStore(User.composeIndexName(), User.composeIndexType(), extraUser.getUuid());
			assertThat(dummySearchProvider).hasStore(Group.composeIndexName(), Group.composeIndexType(), group().getUuid());
			assertThat(dummySearchProvider).hasEvents(3, 0, 0, 0);
			dummySearchProvider.clear();

			group().reload();
			assertTrue("User should be member of the group.", group().hasUser(extraUser));
		}

	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		try (NoTx noTx = db.noTx()) {
			Group group = group();
			String groupUuid = group.getUuid();
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", user());
			String extraUserUuid = extraUser.getUuid();
			role().grantPermissions(extraUser, READ_PERM);
			role().revokePermissions(group, UPDATE_PERM);

			MeshResponse<GroupResponse> future = client().addUserToGroup(groupUuid, extraUserUuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", groupUuid);
			assertFalse("User should not be member of the group.", group().hasUser(extraUser));
		}
	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", user());
			role().grantPermissions(extraUser, DELETE_PERM);

			MeshResponse<GroupResponse> future = client().addUserToGroup(group().getUuid(), extraUser.getUuid()).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", extraUser.getUuid());
			assertFalse("User should not be member of the group.", group().hasUser(extraUser));
		}
	}

	// Group User Testcases - DELETE / Remove
	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			Group group = group();
			assertTrue("User should be a member of the group.", group.hasUser(user));
			role().revokePermissions(group, UPDATE_PERM);
			call(() -> client().removeUserFromGroup(group().getUuid(), user().getUuid()), FORBIDDEN, "error_missing_perm", group().getUuid());
			assertTrue("User should still be a member of the group.", group().hasUser(user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			call(() -> client().removeUserFromGroup(group().getUuid(), user().getUuid()));
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
		try (NoTx noTx = db.noTx()) {
			call(() -> client().removeUserFromGroup(group().getUuid(), user().getUuid()));
			assertFalse("User should no longer be member of the group.", group().hasUser(user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {
		try (NoTx noTx = db.noTx()) {
			call(() -> client().removeUserFromGroup(group().getUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
			assertTrue("User should still be member of the group.", group().hasUser(user()));
		}
	}
}
