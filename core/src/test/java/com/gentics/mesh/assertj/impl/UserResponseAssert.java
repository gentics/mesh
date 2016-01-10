package com.gentics.mesh.assertj.impl;

import org.assertj.core.api.AbstractAssert;
import static org.junit.Assert.*;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserResponse;

public class UserResponseAssert extends AbstractAssert<UserResponseAssert, UserResponse> {

	public UserResponseAssert(UserResponse actual) {
		super(actual, UserResponseAssert.class);
	}

	public UserResponseAssert matches(User user) {
		assertNotNull("The user must not be null.", user);
		assertNotNull("The restuser must not be null", actual);
		// user = neo4jTemplate.fetch(user);
		assertEquals(user.getUsername(), actual.getUsername());
		assertEquals(user.getEmailAddress(), actual.getEmailAddress());
		assertEquals(user.getFirstname(), actual.getFirstname());
		assertEquals(user.getLastname(), actual.getLastname());
		assertEquals(user.getUuid(), actual.getUuid());
		assertEquals(user.getGroups().size(), actual.getGroups().size());
		// TODO groups
		return this;
	}

}
