package com.gentics.mesh.assertj.impl;

import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import org.assertj.core.api.AbstractAssert;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
		assertEquals(user.getGroups().count(), actual.getGroups().size());
		// TODO groups
		return this;
	}

	public UserResponseAssert matches(UserCreateRequest request) {
		assertNotNull(request);
		assertNotNull(actual);

		assertEquals(request.getUsername(), actual.getUsername());
		assertEquals(request.getEmailAddress(), actual.getEmailAddress());
		assertEquals(request.getLastname(), actual.getLastname());
		assertEquals(request.getFirstname(), actual.getFirstname());

		// TODO check groupuuid vs groups loaded user

		return this;
	}

	public UserResponseAssert matches(UserUpdateRequest request) {
		assertNotNull(request);
		assertNotNull(actual);

		if (request.getUsername() != null) {
			assertEquals(request.getUsername(), actual.getUsername());
		}

		if (request.getEmailAddress() != null) {
			assertEquals(request.getEmailAddress(), actual.getEmailAddress());
		}

		if (request.getLastname() != null) {
			assertEquals(request.getLastname(), actual.getLastname());
		}

		if (request.getFirstname() != null) {
			assertEquals(request.getFirstname(), actual.getFirstname());
		}
		return this;
	}

	public UserResponseAssert hasUuid(String uuid) {
		assertThat(actual.getUuid()).as("User uuid").isEqualTo(uuid);
		return this;
	}

	public UserResponseAssert doesNotHaveToChangePassword() {
		assertThat(actual.getForcedPasswordChange()).as("User does not have to change their password").isFalse();
		return this;
	}

	public UserResponseAssert hasToChangePassword() {
		assertThat(actual.getForcedPasswordChange()).as("User has to change their password").isTrue();
		return this;
	}

	public UserResponseAssert isAnonymous() {
		assertThat(actual.getUsername()).as("User is anonymous").isEqualTo("anonymous");
		return this;
	}

	public UserResponseAssert hasName(String name) {
		assertThat(actual.getUsername()).as(String.format("User has name %s", name)).isEqualTo(name);
		return this;
	}
}
