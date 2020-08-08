package com.gentics.mesh.core.group;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_ASSIGNED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_USER_UNASSIGNED;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
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
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT, startServer = true)
public class GroupUserEndpointTest extends AbstractMeshTest {

	@Test
	public void testGetUsersByGroup() throws Exception {
		String extraUserUuid;
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			GroupDaoWrapper groupDao = tx.data().groupDao();
			UserDaoWrapper userDao = tx.data().userDao();

			User extraUser = userDao.create("extraUser", user());
			groupDao.addUser(group(), extraUser);
			extraUserUuid = extraUser.getUuid();
			roleDao.grantPermissions(role(), extraUser, READ_PERM);
			tx.success();
		}

		ListResponse<UserResponse> userList = call(() -> client().findUsersOfGroup(groupUuid(), new PagingParametersImpl()));
		assertEquals(2, userList.getMetainfo().getTotalCount());
		assertEquals(2, userList.getData().size());
		Iterator<UserResponse> userIt = userList.getData().iterator();
		UserResponse userB = userIt.next();
		UserResponse userA = userIt.next();
		Map<String, UserResponse> map = new HashMap<>();
		map.put(userA.getUuid(), userA);
		map.put(userB.getUuid(), userB);
		assertEquals(2, map.size());
		assertNotNull(map.get(userUuid()));
		assertNotNull(map.get(extraUserUuid));

	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() throws Exception {
		String userUuid;
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			UserDaoWrapper userDao = tx.data().userDao();

			User extraUser = userDao.create("extraUser", user());
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
		User extraUser = tx(tx -> {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			UserDaoWrapper userDao = tx.data().userDao();
			GroupDaoWrapper groupDao = tx.data().groupDao();

			User user = userDao.create("extraUser", user());
			user.setFirstname(userFirstname);
			user.setLastname(userLastname);
			roleDao.grantPermissions(role(), user, READ_PERM);
			GroupRoot groupRoot = boot().groupRoot();
			assertFalse("User should not be member of the group.", groupRoot.hasUser(group(), user));
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
			GroupDaoWrapper groupDao = tx.data().groupDao();
			assertThat(restGroup).matches(group());
			assertThat(trackingSearchProvider()).hasStore(User.composeIndexName(), extraUserUuid);
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
		User extraUser;
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			UserDaoWrapper userDao = tx.data().userDao();

			Group group = group();
			extraUser = userDao.create("extraUser", user());
			roleDao.grantPermissions(role(), extraUser, READ_PERM);
			roleDao.revokePermissions(role(), group, UPDATE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupRoot groupRoot = tx.data().groupDao();
			call(() -> client().addUserToGroup(groupUuid(), extraUser.getUuid()), FORBIDDEN, "error_missing_perm", groupUuid(),
				UPDATE_PERM.getRestPerm().getName());
			assertFalse("User should not be member of the group.", groupRoot.hasUser(group(), extraUser));
		}

	}

	@Test
	public void testAddUserToGroupWithoutPermOnUser() throws Exception {
		User extraUser;
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			UserDaoWrapper userDao = tx.data().userDao();

			extraUser = userDao.create("extraUser", user());
			roleDao.grantPermissions(role(), extraUser, DELETE_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			GroupDaoWrapper groupDao = tx.data().groupDao();
			call(() -> client().addUserToGroup(group().getUuid(), extraUser.getUuid()), FORBIDDEN, "error_missing_perm", extraUser.getUuid(),
				READ_PERM.getRestPerm().getName());
			assertFalse("User should not be member of the group.", groupDao.hasUser(group(), extraUser));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithoutPerm() throws Exception {
		try (Tx tx = tx()) {
			RoleRoot roleDao = tx.data().roleDao();
			GroupRoot groupRoot = tx.data().groupDao();
			assertTrue("User should be a member of the group.", groupRoot.hasUser(group(), user()));
			roleDao.revokePermissions(role(), group(), UPDATE_PERM);
			tx.success();
		}

		call(() -> client().removeUserFromGroup(groupUuid(), userUuid()), FORBIDDEN, "error_missing_perm", groupUuid(),
			UPDATE_PERM.getRestPerm().getName());
		try (Tx tx = tx()) {
			GroupRoot groupRoot = tx.data().groupDao();
			assertTrue("User should still be a member of the group.", groupRoot.hasUser(group(), user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithPerm() throws Exception {
		final String groupUuid = groupUuid();
		final String groupName = tx(() -> group().getName());
		final String userFirstname = "Albert";
		final String userLastname = "Einstein";

		User extraUser = tx(tx -> {
			RoleDaoWrapper roleDao = tx.data().roleDao();
			UserDaoWrapper userDao = tx.data().userDao();
			GroupDaoWrapper groupRoot = tx.data().groupDao();

			User user = userDao.create("extraUser", user());
			user.setFirstname(userFirstname);
			user.setLastname(userLastname);
			roleDao.grantPermissions(role(), user, READ_PERM);
			groupRoot.addUser(group(), user);
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
			GroupRoot groupRoot = tx.data().groupDao();
			assertThat(trackingSearchProvider()).hasStore(User.composeIndexName(), extraUserUuid);
			assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);
			assertFalse("User should not be member of the group.", groupRoot.hasUser(group(), extraUser));
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
			GroupRoot groupRoot = tx.data().groupDao();
			assertFalse("User should no longer be member of the group.", groupRoot.hasUser(group(), user()));
		}
	}

	@Test
	public void testRemoveUserFromGroupWithBogusUserUuid() throws Exception {
		call(() -> client().removeUserFromGroup(groupUuid(), "bogus"), NOT_FOUND, "object_not_found_for_uuid", "bogus");
		try (Tx tx = tx()) {
			GroupRoot groupRoot = tx.data().groupDao();
			assertTrue("User should still be member of the group.", groupRoot.hasUser(group(), user()));
		}
	}
}
