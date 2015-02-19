package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.UserRepository;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.test.AbstractDBTest;

public class UserTest extends AbstractDBTest {

	@Autowired
	UserRepository userRepository;

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
		userRepository.save(user);

		User reloadedUser = userRepository.findOne(user.getId());
		assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
		assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
		assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
		assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
		assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());
	}

}
