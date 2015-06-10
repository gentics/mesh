package com.gentics.mesh.core.data.model;

import static com.gentics.mesh.util.TinkerpopUtils.count;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.User;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.test.AbstractDBTest;

public class GroupTest extends AbstractDBTest {

	@Autowired
	private GroupService groupService;

	@Before
	public void setup() throws Exception {
		setupData();
	}

	@Test
	public void testUserGroup() {
		User user = userService.create("testuser");
		Group group = groupService.create("test group");
//		try (Transaction tx = graphDb.beginTx()) {
			group.addUser(user);
//			tx.success();
//		}

		assertEquals("The group should contain one member.", 1, count(group.getUsers()));

//		try (Transaction tx = graphDb.beginTx()) {
			User userOfGroup = group.getUsers().iterator().next();
			//			neo4jTemplate.fetch(userOfGroup);
			assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
//			tx.success();
//		}
	}

	@Test
	public void testRootGroupNode() {
		int nGroupsBefore = count(groupService.findRoot().getGroups());

		Group group = groupService.create("test group2");
//		try (Transaction tx = graphDb.beginTx()) {
//			tx.success();
//		}

		int nGroupsAfter = count(groupService.findRoot().getGroups());
		assertEquals(nGroupsBefore + 1, nGroupsAfter);
	}

}
