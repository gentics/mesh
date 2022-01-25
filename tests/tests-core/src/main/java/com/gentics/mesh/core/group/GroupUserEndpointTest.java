package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.PersistingGroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT, startServer = true)
public class GroupUserEndpointTest extends AbstractMeshTest {

	@Test
	public void testGetUsersByGroup() throws Exception {
		String extraUserUuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			PersistingGroupDao groupDao = tx.<CommonTx>unwrap().groupDao();
			UserDao userDao = tx.userDao();

			HibUser extraUser = userDao.create("extraUser", user());
			HibGroup group = groupDao.findByUuid(group().getUuid());
			groupDao.addUser(group, extraUser);
			groupDao.mergeIntoPersisted(group);
			extraUserUuid = extraUser.getUuid();
			roleDao.grantPermissions(role(), extraUser, READ_PERM);
			tx.success();
		}

		ListResponse<UserResponse> userList = call(() -> client().findUsersOfGroup(groupUuid(), new PagingParametersImpl().setPerPage(25L)));
		assertEquals(2, userList.getMetainfo().getTotalCount());

		UserResponse expectedTestUser = new UserResponse();
		expectedTestUser.setUuid(userUuid());
		expectedTestUser.setUsername(user().getUsername());

		UserResponse expectedExtraUser = new UserResponse();
		expectedExtraUser.setUuid(extraUserUuid);
		expectedExtraUser.setUsername("extraUser");

		assertThat(userList.getData()).as("Users of group").usingElementComparatorOnFields("uuid", "username")
				.containsOnly(expectedTestUser, expectedExtraUser);

		// revoke read permission on the extra user
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibUser extraUser = userDao.findByUuid(extraUserUuid);
			roleDao.revokePermissions(role(), extraUser, READ_PERM);
			tx.success();
		}

		userList = call(() -> client().findUsersOfGroup(groupUuid(), new PagingParametersImpl().setPerPage(25L)));
		assertEquals(1, userList.getMetainfo().getTotalCount());
		assertThat(userList.getData()).as("Users of group").usingElementComparatorOnFields("uuid", "username")
				.containsOnly(expectedTestUser);
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {
		String userUuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();

			HibUser extraUser = userDao.create("extraUser", user());
			userUuid = extraUser.getUuid();
			roleDao.grantPermissions(role(), extraUser, READ_PERM);
			tx.success();
		}

		call(() -> client().addUserToGroup("bogus", userUuid), NOT_FOUND, "object_not_found_for_uuid", "bogus");

	}

	@Test
	public void testAddUserToGroupWithPerm() throws Exception {
		final String userFirstname = "Albert";
		final String userLastname = "Einstein";
		final String groupUuid = groupUuid();
		final String groupName = tx(() -> group().getName());
		HibUser extraUser = tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			GroupDao groupDao = tx.groupDao();

			HibUser user = userDao.create("extraUser", user());
			user.setFirstname(userFirstname);
			user.setLastname(userLastname);
			roleDao.grantPermissions(role(), user, READ_PERM);
			assertFalse("User should not be member of the group.", groupDao.hasUser(group(), user));
			return user;
		});

		String extraUserUuid = tx(() -> extraUser.getUuid());
		expect(GROUP_USER_ASSIGNED).match(1, GroupUserAssignModel.class, event -> {
			GroupReference groupRef = event.getGroup();
			assertNotNull(groupRef);
			assertEquals("The group name was not set.", groupName, groupRef.getName());
			assertEquals("The group uuid was not set.", groupUuid, groupRef.getUuid());

			UserReference userRef = event.getUser();
			assertNotNull(userRef);
			assertEquals("The user uuid was not set.", extraUserUuid, userRef.getUuid());
			assertEquals("The user firstname was not set.", userFirstname, userRef.getFirstName());
			assertEquals("The user lastname was not set.", userLastname, userRef.getLastName());
		}).total(1);

		GroupResponse restGroup = call(() -> client().addUserToGroup(groupUuid(), extraUserUuid));
		awaitEvents();
		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertThat(restGroup).matches(group());
			assertThat(trackingSearchProvider()).hasStore(HibUser.composeIndexName(), extraUserUuid);
			assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);
			trackingSearchProvider().reset();
			assertTrue("User should be member of the group.", groupDao.hasUser(group(), extraUser));
		}
		// Test for idempotency
		expect(GROUP_USER_ASSIGNED).none();
		call(() -> client().addUserToGroup(groupUuid(), extraUserUuid));
		awaitEvents();
	}

	@Test
	public void testAddUserToGroupWithoutPermOnGroup() throws Exception {
		HibUser extraUser;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();

			HibGroup group = group();
			extraUser = userDao.create("extraUser", user());
			roleDao.grantPermissions(role(), extraUser, READ_PERM);
			roleDao.revokePermissions(role(), group, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			call(() -> client().addUserToGroup(groupUuid(), extraUser.getUuid()), FORBIDDEN, "error_missing_perm", groupUuid(),
				UPDATE_PERM.getRestPerm().getName());
			assertFalse("User should not be member of the group.", groupDao.hasUser(group(), extraUser));
		}

	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		HibUser extraUser;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();

			extraUser = userDao.create("extraUser", user());
			roleDao.grantPermissions(role(), extraUser, DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			call(() -> client().addUserToGroup(group().getUuid(), extraUser.getUuid()), FORBIDDEN, "error_missing_perm", extraUser.getUuid(),
				READ_PERM.getRestPerm().getName());
			assertFalse("User should not be member of the group.", groupDao.hasUser(group(), extraUser));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			GroupDao groupDao = tx.groupDao();
			assertTrue("User should be a member of the group.", groupDao.hasUser(group(), user()));
			roleDao.revokePermissions(role(), group(), UPDATE_PERM);
			tx.success();
		}

		call(() -> client().removeUserFromGroup(groupUuid(), userUuid()), FORBIDDEN, "error_missing_perm", groupUuid(),
			UPDATE_PERM.getRestPerm().getName());
		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertTrue("User should still be a member of the group.", groupDao.hasUser(group(), user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		final String groupUuid = groupUuid();
		final String groupName = tx(() -> group().getName());
		final String userFirstname = "Albert";
		final String userLastname = "Einstein";

		HibUser extraUser = tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			PersistingGroupDao groupDao = tx.<CommonTx>unwrap().groupDao();

			HibUser user = userDao.create("extraUser", user());
			user.setFirstname(userFirstname);
			user.setLastname(userLastname);
			roleDao.grantPermissions(role(), user, READ_PERM);

			HibGroup group = groupDao.findByUuid(group().getUuid());
			groupDao.addUser(group, user);
			groupDao.mergeIntoPersisted(group);
			return user;
		});
		final String extraUserUuid = tx(() -> extraUser.getUuid());

		expect(GROUP_USER_UNASSIGNED).match(1, GroupUserAssignModel.class, event -> {
			GroupReference groupRef = event.getGroup();
			assertNotNull(groupRef);
			assertEquals("The group name was not set.", groupName, groupRef.getName());
			assertEquals("The group uuid was not set.", groupUuid, groupRef.getUuid());

			UserReference userRef = event.getUser();
			assertNotNull(userRef);
			assertEquals("The user uuid was not set.", extraUserUuid, userRef.getUuid());
			assertEquals("The user firstname was not set.", userFirstname, userRef.getFirstName());
			assertEquals("The user lastname was not set.", userLastname, userRef.getLastName());
		}).total(1);

		call(() -> client().removeUserFromGroup(groupUuid, extraUserUuid));
		awaitEvents();
		waitForSearchIdleEvent();

		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertThat(trackingSearchProvider()).hasStore(HibUser.composeIndexName(), extraUserUuid);
			assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);
			assertFalse("User should not be member of the group.", groupDao.hasUser(group(), extraUser));
		}

		// Test for idempotency
		expect(GROUP_USER_UNASSIGNED).none();
		call(() -> client().removeUserFromGroup(groupUuid, extraUserUuid));
		awaitEvents();

	}

	@Test
	@Ignore("Not yet implemented")
	public void testRemoveSameUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromLastGroupWithPerm() throws Exception {
		call(() -> client().removeUserFromGroup(groupUuid(), userUuid()));
		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertFalse("User should no longer be member of the group.", groupDao.hasUser(group(), user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {
		call(() -> client().removeUserFromGroup(groupUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			assertTrue("User should still be member of the group.", groupDao.hasUser(group(), user()));
		}
	}
}
