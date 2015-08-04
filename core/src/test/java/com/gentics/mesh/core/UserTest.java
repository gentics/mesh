package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.VerticleHelper.getUser;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
public class UserTest extends AbstractBasicObjectTest {

	@Test
	public void testCreatedUser() {
		assertNotNull("The uuid of the user should not be null since the entity was reloaded.", user().getUuid());
	}

	@Test
	@Override
	public void testRootNode() {
		UserRoot root = meshRoot().getUserRoot();
		int nUserBefore = root.findAll().size();
		assertNotNull(root.create("dummy12345", null, user()));
		int nUserAfter = root.findAll().size();
		assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);
	}

	@Test
	public void testHasPermission() {
		assertTrue(user().hasPermission(english(), READ_PERM));
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends User> page = boot.userRoot().findAll(requestUser, new PagingInfo(1, 10));
		assertEquals(users().size(), page.getTotalElements());
		assertEquals(10, page.getSize());

		page = boot.userRoot().findAll(requestUser, new PagingInfo(1, 15));
		assertEquals(users().size(), page.getTotalElements());
		assertEquals(users().size(), page.getSize());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		Page<? extends User> page = boot.userRoot().findAll(getRequestUser(), new PagingInfo(1, 25));
		assertNotNull(page);
		assertEquals(users().size(), page.getTotalElements());
	}

	@Test
	public void testGetPermissions() {
		Language language = english();
		String[] perms = { "create", "update", "delete", "read" };
		String[] loadedPerms = user().getPermissionNames(language);
		Arrays.sort(perms);
		Arrays.sort(loadedPerms);
		assertArrayEquals("Permissions do not match", perms, loadedPerms);
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {

		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser", group(), user());
		Group group = group();
		Role role = role();
		role.addPermissions(extraUser, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends User> userPage = group.getVisibleUsers(requestUser, new PagingInfo(1, 10));

		assertEquals(2, userPage.getTotalElements());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNull(boot.userRoot().findByUsername("bogus"));
		boot.userRoot().findByUsername(user().getUsername());
	}

	@Test
	@Override
	public void testFindByUUID() throws InterruptedException {
		String uuid = user().getUuid();
		CountDownLatch latch = new CountDownLatch(1);
		boot.userRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			assertEquals(uuid, rh.result().getUuid());
			latch.countDown();
		});
		latch.await();
	}

	@Test
	@Override
	public void testTransformation() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(1);
		RoutingContext rc = getMockedRoutingContext("");
		user().transformToRest(rc, rh -> {
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
		latch.await();
	}

	@Test
	@Override
	public void testCreateDelete() {
		MeshRoot root = meshRoot();
		User user = root.getUserRoot().create("Anton", null, user());
		assertTrue(user.isEnabled());
		assertNotNull(user);
		String uuid = user.getUuid();
		user.delete();
		root.getUserRoot().findByUuid(uuid, rh -> {
			User foundUser = rh.result();
			assertNotNull(foundUser);
			assertFalse(foundUser.isEnabled());
		});
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		User user = user();
		User newUser = root.getUserRoot().create("Anton", null, user());
		assertFalse(user.hasPermission(newUser, Permission.CREATE_PERM));
		user.addCRUDPermissionOnRole(root.getUserRoot(), Permission.CREATE_PERM, newUser);
		assertTrue(user.hasPermission(newUser, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() {
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

	@Test
	@Override
	public void testCreate() {
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

		userRoot.findByUuid(user.getUuid(), rh -> {
			User reloadedUser = rh.result();
			assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
			assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
			assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
			assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
			assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());
		});

	}

	@Test
	@Override
	public void testDelete() {
		User user = user();
		assertEquals(1, user.getGroupCount());
		assertTrue(user.isEnabled());
		user.delete();
		assertFalse(user.isEnabled());
		assertEquals(0, user.getGroupCount());
	}

	@Test
	@Override
	public void testUpdate() {

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
		assertEquals(newUser, user.getEditor());
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

	@Test
	@Override
	public void testReadPermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(Permission.READ_PERM, user);
	}

	@Test
	@Override
	public void testDeletePermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(Permission.DELETE_PERM, user);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(Permission.UPDATE_PERM, user);
	}

	@Test
	@Override
	public void testCreatePermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(Permission.CREATE_PERM, user);
	}
}
