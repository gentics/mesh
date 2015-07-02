package com.gentics.mesh.core;

import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.ext.web.RoutingContext;

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class UserTest extends AbstractDBTest implements BasicObjectTestcases {

	private UserInfo info;

	private User user;

	@Before
	public void setup() throws Exception {
		setupData();
		info = data().getUserInfo();
		user = info.getUser();
	}

	@Test
	public void testCreatedUser() {
		assertNotNull("The uuid of the user should not be null since the entity was reloaded.", data().getUserInfo().getUser().getUuid());
	}

	@Test
	@Override
	public void testRootNode() {
		UserRoot root = data().getMeshRoot().getUserRoot();
		int nUserBefore = root.getUsers().size();
		assertNotNull(root.create("dummy12345"));
		int nUserAfter = root.getUsers().size();
		assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);
	}

	@Test
	public void testHasPermission() {
		Language language = data().getEnglish();
		assertTrue(user.hasPermission(language, READ_PERM));
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends User> page = userService.findAllVisible(requestUser, new PagingInfo(1, 10));
		assertEquals(data().getUsers().size(), page.getTotalElements());
		assertEquals(10, page.getSize());

		page = userService.findAllVisible(requestUser, new PagingInfo(1, 15));
		assertEquals(data().getUsers().size(), page.getTotalElements());
		assertEquals(15, page.getSize());
	}

	@Test
	public void testGetPermissions() {
		Language language = data().getEnglish();
		String[] perms = { "CREATE_PERM", "UPDATE_PERM", "DELETE_PERM", "READ_PERM" };
		String[] loadedPerms = user.getPermissionNames(language);
		Arrays.sort(perms);
		Arrays.sort(loadedPerms);
		assertArrayEquals("Permissions do not match", perms, loadedPerms);
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {

		UserRoot userRoot = data().getMeshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser");
		Group group = info.getGroup();
		Role role = info.getRole();
		group.addUser(extraUser);

		role.addPermissions(extraUser, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends User> userPage = group.getVisibleUsers(requestUser, new PagingInfo(1, 10));

		assertEquals(2, userPage.getTotalElements());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindByName() {
		assertNull(userService.findByUsername("bogus"));
		userService.findByUsername(user.getUsername());
	}

	@Test
	@Override
	public void testFindByUUID() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testTransformation() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRead() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreate() {
		final String USERNAME = "test";
		final String EMAIL = "joe@nowhere.org";
		final String FIRSTNAME = "joe";
		final String LASTNAME = "doe";
		final String PASSWDHASH = "RANDOM";

		UserRoot userRoot = data().getMeshRoot().getUserRoot();
		User user = userRoot.create(USERNAME);
		user.setEmailAddress(EMAIL);
		user.setFirstname(FIRSTNAME);
		user.setLastname(LASTNAME);
		user.setPasswordHash(PASSWDHASH);

		User reloadedUser = userService.findByUUID(user.getUuid());
		assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
		assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
		assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
		assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
		assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());

	}

	@Test
	@Override
	public void testDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadPermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub

	}
}
