package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.core.data.model.relationship.Permission.READ_PERM;
import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import org.junit.Before;
import org.junit.Test;

import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.demo.UserInfo;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;

public class UserTest extends AbstractDBTest {

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

		User user = userService.create(USERNAME);
		user.setEmailAddress(EMAIL);
		user.setFirstname(FIRSTNAME);
		user.setLastname(LASTNAME);
		user.setPasswordHash(PASSWDHASH);

		User reloadedUser = userService.findOne(user.getId());
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
		int nUserBefore = count(userService.findRoot().getUsers());

		User user = userService.create("dummy12345");

		int nUserAfter = count(userService.findRoot().getUsers());
		assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);

	}

	@Test
	public void testFindUsersOfGroup() {

		User extraUser = userService.create("extraUser");
		info.getGroup().addUser(extraUser);
		info.getRole().addPermissions(extraUser, READ_PERM);

		RoutingContext rc = getMockedRoutingContext("");
		Page<User> userPage = info.getGroup().getUsers().findByGroup(rc, info.getGroup(), new PagingInfo(1, 10));
		assertEquals(2, userPage.getTotalElements());
	}
}
