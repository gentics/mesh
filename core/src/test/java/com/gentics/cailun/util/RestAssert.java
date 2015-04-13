package com.gentics.cailun.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.rest.group.response.GroupResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;

public final class RestAssert {

	private RestAssert() {
	}
	
	public static void assertGroup(Group group, GroupResponse restGroup) {
		//String json = "{\"uuid\":\"uuid-value\",\"name\":\"dummy_user_group\",\"roles\":[\"dummy_user_role\"],\"users\":[\"dummy_user\"],\"perms\":[]}";
		assertEquals(group.getUuid(), restGroup.getUuid());
		assertEquals(group.getName(),restGroup.getName());
		for(User user : group.getUsers() ) {
			assertTrue(restGroup.getUsers().contains(user.getUsername()));
		}
		//TODO roles
		//group.getRoles()
		//TODO perms
	}

	public static void assertUser(User user, UserResponse restUser) {
		assertEquals(user.getUsername(), restUser.getUsername());
		assertEquals(user.getEmailAddress(), restUser.getEmailAddress());
		assertEquals(user.getFirstname(), restUser.getFirstname());
		assertEquals(user.getLastname(), restUser.getLastname());
		assertEquals(user.getUuid(), restUser.getUuid());
		assertEquals(user.getGroups().size(), restUser.getGroups().size());
		//TODO groups
	}

}
