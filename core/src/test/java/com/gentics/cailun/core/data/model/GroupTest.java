package com.gentics.cailun.core.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.repository.GroupRepository;
import com.gentics.cailun.test.AbstractDBTest;

public class GroupTest extends AbstractDBTest {

	@Autowired
	GroupRepository groupRepository;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testUserGroup() {
		User user = new User("testuser");
		Group group = new Group("test group");
		try (Transaction tx = graphDb.beginTx()) {
			group.addUser(user);
			groupRepository.save(group);
			tx.success();
		}

		group = groupService.reload(group);
		assertEquals("The group should contain one member.", 1, group.getUsers().size());
		User userOfGroup = group.getUsers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}

	@Test
	public void testRootGroupNode() {
		int nGroupsBefore = groupRepository.findRoot().getGroups().size();

		Group group = new Group("test group2");
		groupRepository.save(group);

		int nGroupsAfter = groupRepository.findRoot().getGroups().size();
		assertEquals(nGroupsBefore + 1, nGroupsAfter);
	}
}
