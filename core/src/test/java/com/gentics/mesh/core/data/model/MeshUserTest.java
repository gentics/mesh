package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import io.vertx.ext.web.RoutingContext;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.Language;
import com.gentics.mesh.core.data.model.tinkerpop.MeshAuthUser;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
import com.gentics.mesh.core.data.model.tinkerpop.Role;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class MeshUserTest extends AbstractDBTest {

	private UserInfo info;

	@Before
	public void setup() throws Exception {
		setupData();
		info = data().getUserInfo();
	}

	@Test
	public void testUserCreation() {
		final String USERNAME = "test";
		final String EMAIL = "joe@nowhere.org";
		final String FIRSTNAME = "joe";
		final String LASTNAME = "doe";
		final String PASSWDHASH = "RANDOM";

		UserRoot userRoot = data().getMeshRoot().getUserRoot();
		MeshUser user = userRoot.create(USERNAME);
		user.setEmailAddress(EMAIL);
		user.setFirstname(FIRSTNAME);
		user.setLastname(LASTNAME);
		user.setPasswordHash(PASSWDHASH);

		MeshUser reloadedUser = userService.findOne(user.getId());
		assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
		assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
		assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
		assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
		assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());
	}

	@Test
	public void testCreatedUser() {
		assertNotNull("The uuid of the user should not be null since the entity was reloaded.", data().getUserInfo().getUser().getUuid());
	}

	@Test
	public void testUserRoot() {
		int nUserBefore = userService.findRoot().getUsers().size();
		UserRoot userRoot = data().getMeshRoot().getUserRoot();

		MeshUser user = userRoot.create("dummy12345");

		int nUserAfter = userService.findRoot().getUsers().size();
		assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);

	}

	@Test
	public void testHasPermission() {
		MeshUser user = info.getUser();
		Language language = data().getEnglish();

		assertTrue(user.hasPermission(language, READ_PERM));
	}

	@Test
	public void testGetPermissions() {
		MeshUser user = info.getUser();
		Language language = data().getEnglish();
		String[] perms = { "CREATE_PERM", "UPDATE_PERM", "DELETE_PERM", "READ_PERM" };
		String[] loadedPerms = user.getPermissionNames(language);
		Arrays.sort(perms);
		Arrays.sort(loadedPerms);
		assertArrayEquals("Permissions do not match", perms,  loadedPerms);
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {

		UserRoot userRoot = data().getMeshRoot().getUserRoot();
		MeshUser extraUser = userRoot.create("extraUser");
		Group group = info.getGroup();
		Role role = info.getRole();
		group.addUser(extraUser);

		role.addPermissions(extraUser, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends MeshUser> userPage = group.getVisibleUsers(requestUser, new PagingInfo(1, 10));

		assertEquals(2, userPage.getTotalElements());
	}
}
