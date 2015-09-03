package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserTest extends AbstractBasicObjectTest {

	@Test
	public void testCreatedUser() {
		assertNotNull("The uuid of the user should not be null since the entity was reloaded.", user().getUuid());
	}

	@Test
	@Override
	public void testRootNode() {
		try (Trx tx = db.trx()) {
			UserRoot root = meshRoot().getUserRoot();
			int nUserBefore = root.findAll().size();
			assertNotNull(root.create("dummy12345", null, user()));
			int nUserAfter = root.findAll().size();
			assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);
		}
	}

	@Test
	public void testHasPermission() {
		try (Trx tx = db.trx()) {
			assertTrue(user().hasPermission(english(), READ_PERM));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		ActionContext ac = ActionContext.create(rc);
		try (Trx tx = db.trx()) {
			MeshAuthUser requestUser = ac.getUser();

			Page<? extends User> page = boot.userRoot().findAll(requestUser, new PagingInfo(1, 6));
			assertEquals(users().size(), page.getTotalElements());
			assertEquals(6, page.getSize());

			page = boot.userRoot().findAll(requestUser, new PagingInfo(1, 15));
			assertEquals(users().size(), page.getTotalElements());
			assertEquals(users().size(), page.getSize());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Trx tx = db.trx()) {
			Page<? extends User> page = boot.userRoot().findAll(getRequestUser(), new PagingInfo(1, 25));
			assertNotNull(page);
			assertEquals(users().size(), page.getTotalElements());
		}
	}

	@Test
	public void testGetPrincipal() {
		RoutingContext rc = getMockedRoutingContext("");
		io.vertx.ext.auth.User user = rc.user();
		assertNotNull(user);
		JsonObject json = user.principal();
		assertNotNull(json);
		try (Trx tx = db.trx()) {
			assertEquals(user().getUuid(), json.getString("uuid"));
			assertEquals(user().getUsername(), json.getString("username"));
			assertEquals(user().getFirstname(), json.getString("firstname"));
			assertEquals(user().getLastname(), json.getString("lastname"));
			assertEquals(user().getEmailAddress(), json.getString("emailAddress"));

			assertNotNull(json.getJsonArray("roles"));
			assertEquals(user().getRoles().size(), json.getJsonArray("roles").size());
			assertNotNull(json.getJsonArray("groups"));
			assertEquals(user().getGroups().size(), json.getJsonArray("groups").size());
		}
	}

	@Test
	public void testGetPermissions() {
		try (Trx tx = db.trx()) {
			Language language = english();
			String[] perms = { "create", "update", "delete", "read" };
			String[] loadedPerms = user().getPermissionNames(language);
			Arrays.sort(perms);
			Arrays.sort(loadedPerms);
			assertArrayEquals("Permissions do not match", perms, loadedPerms);
		}
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {
		try (Trx tx = db.trx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", group(), user());
			role().grantPermissions(extraUser, READ_PERM);
			tx.success();
		}
		try (Trx txUpdate = db.trx()) {
			RoutingContext rc = getMockedRoutingContext("");
			ActionContext ac = ActionContext.create(rc);
			MeshAuthUser requestUser = ac.getUser();
			Page<? extends User> userPage = group().getVisibleUsers(requestUser, new PagingInfo(1, 10));

			assertEquals(2, userPage.getTotalElements());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Trx tx = db.trx()) {
			assertNull(boot.userRoot().findByUsername("bogus"));
			boot.userRoot().findByUsername(user().getUsername());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws InterruptedException {
		try (Trx tx = db.trx()) {
			String uuid = user().getUuid();
			CountDownLatch latch = new CountDownLatch(1);
			boot.userRoot().findByUuid(uuid, rh -> {
				assertNotNull(rh.result());
				assertEquals(uuid, rh.result().getUuid());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException {
		try (Trx tx = db.trx()) {
			CountDownLatch latch = new CountDownLatch(1);
			RoutingContext rc = getMockedRoutingContext("");
			ActionContext ac = ActionContext.create(rc);
			user().transformToRest(ac, rh -> {
				UserResponse restUser = rh.result();
				assertNotNull(restUser);
				assertEquals(user().getUsername(), restUser.getUsername());
				assertEquals(user().getUuid(), restUser.getUuid());
				assertEquals(user().getLastname(), restUser.getLastname());
				assertEquals(user().getFirstname(), restUser.getFirstname());
				assertEquals(user().getEmailAddress(), restUser.getEmailAddress());
				assertEquals(1, restUser.getGroups().size());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws InterruptedException {
		try (Trx tx = db.trx()) {
			MeshRoot root = meshRoot();
			User user = root.getUserRoot().create("Anton", null, user());
			assertTrue(user.isEnabled());
			assertNotNull(user);
			String uuid = user.getUuid();
			user.delete();
			CountDownLatch latch = new CountDownLatch(1);
			root.getUserRoot().findByUuid(uuid, rh -> {
				User foundUser = rh.result();
				assertNotNull(foundUser);
				assertFalse(foundUser.isEnabled());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Trx tx = db.trx()) {
			MeshRoot root = meshRoot();
			User user = user();
			User newUser = root.getUserRoot().create("Anton", null, user());
			assertFalse(user.hasPermission(newUser, GraphPermission.CREATE_PERM));
			user.addCRUDPermissionOnRole(root.getUserRoot(), GraphPermission.CREATE_PERM, newUser);
			assertTrue(user.hasPermission(newUser, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Trx tx = db.trx()) {
			User user = user();
			assertEquals("joe1", user.getUsername());
			assertNotNull(user.getPasswordHash());
			assertEquals("Joe", user.getFirstname());
			assertEquals("Doe", user.getLastname());
			assertEquals("j.doe@spam.gentics.com", user.getEmailAddress());

			assertNotNull(user.getLastEditedTimestamp());
			assertNotNull(user.getCreator());
			assertNotNull(user.getEditor());
			assertNotNull(user.getCreationTimestamp());
			assertEquals(1, user.getGroups().size());
			assertNotNull(user.getImpl());
		}
	}

	@Test
	public void testUserGroup() {
		try (Trx tx = db.trx()) {

			User user = user();
			assertEquals(1, user.getGroups().size());

			for (int i = 0; i < 10; i++) {
				Group extraGroup = meshRoot().getGroupRoot().create("group_" + i, user());
				// Multiple calls should not affect the result
				user().addGroup(extraGroup);
				user().addGroup(extraGroup);
				user().addGroup(extraGroup);
				user().addGroup(extraGroup);
			}

			assertEquals(11, user().getGroups().size());
		}
	}

	@Test
	@Override
	public void testCreate() throws InterruptedException {
		try (Trx tx = db.trx()) {
			final String USERNAME = "test";
			final String EMAIL = "joe@nowhere.org";
			final String FIRSTNAME = "joe";
			final String LASTNAME = "doe";
			final String PASSWDHASH = "RANDOM";

			UserRoot userRoot = meshRoot().getUserRoot();
			User user = userRoot.create(USERNAME, null, user());
			user.setEmailAddress(EMAIL);
			user.setFirstname(FIRSTNAME);
			user.setLastname(LASTNAME);
			user.setPasswordHash(PASSWDHASH);
			assertTrue(user.isEnabled());

			CountDownLatch latch = new CountDownLatch(1);
			userRoot.findByUuid(user.getUuid(), rh -> {
				User reloadedUser = rh.result();
				assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
				assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
				assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
				assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
				assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	@Override
	public void testDelete() {
		try (Trx tx = db.trx()) {
			User user = user();
			assertEquals(1, user.getGroups().size());
			assertTrue(user.isEnabled());
			user.delete();
			assertFalse(user.isEnabled());
			assertEquals(0, user.getGroups().size());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Trx tx = db.trx()) {
			User newUser = meshRoot().getUserRoot().create("newUser", null, user());

			User user = user();

			user.setEmailAddress("changed");
			assertEquals("changed", user.getEmailAddress());

			user.setFirstname("changed_firstname");
			assertEquals("changed_firstname", user.getFirstname());

			user.setLastname("changed_lastname");
			assertEquals("changed_lastname", user.getLastname());

			user.setEditor(newUser);
			assertNotNull(user.getEditor());
			assertEquals(newUser.getUuid(), user.getEditor().getUuid());
			user.setLastEditedTimestamp(1);
			assertEquals(1, user.getLastEditedTimestamp().longValue());

			user.setCreator(newUser);
			assertNotNull(user.getCreator());
			assertEquals(newUser, user.getCreator());

			user.setCreationTimestamp(0);
			assertEquals(0, user.getCreationTimestamp().longValue());

			assertTrue(user.isEnabled());
			user.disable();
			assertFalse(user.isEnabled());

			assertNotNull(user.getPasswordHash());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		User user;
		try (Trx tx = db.trx()) {
			user = meshRoot().getUserRoot().create("Anton", null, user());
			tx.success();
		}
		testPermission(GraphPermission.READ_PERM, user);
	}

	@Test
	@Override
	public void testDeletePermission() {
		User user;
		try (Trx tx = db.trx()) {
			user = meshRoot().getUserRoot().create("Anton", null, user());
			tx.success();
		}
		testPermission(GraphPermission.DELETE_PERM, user);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		User user;
		try (Trx tx = db.trx()) {
			user = meshRoot().getUserRoot().create("Anton", null, user());
			tx.success();
		}
		testPermission(GraphPermission.UPDATE_PERM, user);
	}

	@Test
	@Override
	public void testCreatePermission() {
		User user;
		try (Trx tx = db.trx()) {
			user = meshRoot().getUserRoot().create("Anton", null, user());
			tx.success();
		}
		testPermission(GraphPermission.CREATE_PERM, user);
	}
}
