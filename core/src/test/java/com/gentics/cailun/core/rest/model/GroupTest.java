package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.GlobalGroupRepository;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.test.AbstractDBTest;

public class GroupTest extends AbstractDBTest {

	@Autowired
	GlobalGroupRepository groupRepository;

	@Test
	public void testUserGroup() {
		User user = new User("testuser");
		Group group = new Group();
		group.addUser(user);
		groupRepository.save(group);

		Group reloadedGroup = groupRepository.findOne(group.getId());
		assertEquals("The group should contain one member.", 1, reloadedGroup.getMembers().size());
		User userOfGroup = reloadedGroup.getMembers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}
}
