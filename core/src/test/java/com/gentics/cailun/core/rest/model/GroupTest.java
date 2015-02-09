package com.gentics.cailun.core.rest.model;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.core.rest.model.auth.Group;
import com.gentics.cailun.core.rest.model.auth.User;
import com.gentics.cailun.test.Neo4jSpringTestConfiguration;

@ContextConfiguration(classes = { Neo4jSpringTestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class GroupTest {

	@Autowired
	GroupRepository groupRepository;

	@Test
	public void testUserGroup() {
		User user = new User("testuser");
		Group group = new Group();
		// TODO maybe add a addUser method?
		group.getMembers().add(user);
		groupRepository.save(group);

		Group reloadedGroup = groupRepository.findOne(group.getId());
		assertEquals("The group should contain one member.", 1, reloadedGroup.getMembers().size());
		User userOfGroup = reloadedGroup.getMembers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}
}
