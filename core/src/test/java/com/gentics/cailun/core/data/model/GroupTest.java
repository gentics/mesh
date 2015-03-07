package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class GroupTest extends AbstractDBTest {

	@Autowired
	GroupRepository groupRepository;

	@Before
	public void setup() {
		setupData();
	}

	@Test
	public void testUserGroup() {
		User user = new User("testuser");
		Group group = new Group("test group");
		group.addUser(user);
		groupRepository.save(group);

		Group reloadedGroup = groupRepository.findOne(group.getId());
		assertEquals("The group should contain one member.", 1, reloadedGroup.getUsers().size());
		User userOfGroup = reloadedGroup.getUsers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}
}
