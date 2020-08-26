package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.group.GroupUpdateRequest;

public class GroupResponseAssert extends AbstractAssert<GroupResponseAssert, GroupResponse> {

	public GroupResponseAssert(GroupResponse actual) {
		super(actual, GroupResponseAssert.class);
	}

	public GroupResponseAssert matches(HibGroup group) {
		assertEquals("The uuid of the rest model does not match the given group node", group.getUuid(), actual.getUuid());
		assertEquals("The name of the rest model group does not match the given group node", group.getName(), actual.getName());
		// for (User user : group.getUsers()) {
		// assertTrue(restGroup.getUsers().contains(user.getUsername()));
		// }
		// TODO roles
		// group.getRoles()
		// TODO perms

		return this;
	}

	public GroupResponseAssert matches(GroupCreateRequest request) {
		assertNotNull(request);
		assertNotNull(actual);
		if (request.getName() != null) {
			assertEquals("The provided group name did not match up with the name in the request.", request.getName(), actual.getName());
		}
		// assertNotNull(restGroup.getUsers());
		assertNotNull(actual.getUuid());
		return this;
	}

	public GroupResponseAssert matches(GroupUpdateRequest request) {
		assertNotNull(request);
		assertNotNull(actual);

		if (request.getName() != null) {
			assertEquals(request.getName(), actual.getName());
		}
		return this;
	}

}
