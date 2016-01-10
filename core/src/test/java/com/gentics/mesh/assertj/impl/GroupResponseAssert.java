package com.gentics.mesh.assertj.impl;

import static org.junit.Assert.assertEquals;

import org.assertj.core.api.AbstractAssert;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.rest.group.GroupResponse;

public class GroupResponseAssert extends AbstractAssert<GroupResponseAssert, GroupResponse> {

	public GroupResponseAssert(GroupResponse actual) {
		super(actual, GroupResponseAssert.class);
	}
	
	public GroupResponseAssert matches(Group group) {
		assertEquals("The uuid of the rest model does not match the given group node",group.getUuid(), actual.getUuid());
		assertEquals("The name of the rest model group does not match the given group node", group.getName(), actual.getName());
		// for (User user : group.getUsers()) {
		// assertTrue(restGroup.getUsers().contains(user.getUsername()));
		// }
		// TODO roles
		// group.getRoles()
		// TODO perms
		
		return this;
	}


}
