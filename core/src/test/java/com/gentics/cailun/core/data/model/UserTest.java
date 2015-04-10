package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class UserTest extends AbstractDBTest {

	@Autowired
	UserRepository userRepository;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testUserCreation() {
		final String USERNAME = "test";
		final String EMAIL = "joe@nowhere.org";
		final String FIRSTNAME = "joe";
		final String LASTNAME = "doe";
		final String PASSWDHASH = "RANDOM";

		User user = new User(USERNAME);
		user.setEmailAddress(EMAIL);
		user.setFirstname(FIRSTNAME);
		user.setLastname(LASTNAME);
		user.setPasswordHash(PASSWDHASH);

		try (Transaction tx = graphDb.beginTx()) {
			user = userRepository.save(user);
			tx.success();
		}

		User reloadedUser = userRepository.findOne(user.getId());
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
		int nUserBefore = userRepository.findRoot().getUsers().size();

		try (Transaction tx = graphDb.beginTx()) {
			User user = new User("dummy12345");
			user = userRepository.save(user);
			tx.success();
		}

		int nUserAfter = userRepository.findRoot().getUsers().size();
		assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);

	}
}
