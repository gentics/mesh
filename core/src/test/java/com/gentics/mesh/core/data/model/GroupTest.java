package com.gentics.mesh.core.data.model;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.model.root.GroupRoot;
import com.gentics.mesh.core.data.model.root.UserRoot;
import com.gentics.mesh.core.data.model.tinkerpop.Group;
import com.gentics.mesh.core.data.model.tinkerpop.MeshUser;
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
		UserRoot userRoot = data().getMeshRoot().getUserRoot();
		GroupRoot groupRoot = data().getMeshRoot().getGroupRoot();

		MeshUser user = userRoot.create("testuser");
		Group group = groupRoot.create("test group");
		group.addUser(user);

		assertEquals("The group should contain one member.", 1, group.getUsers().size());

		MeshUser userOfGroup = group.getUsers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}

	@Test
	public void testRootGroupNode() {
		int nGroupsBefore = groupService.findRoot().getGroups().size();
		GroupRoot groupRoot = data().getMeshRoot().getGroupRoot();
		Group group = groupRoot.create("test group2");

		int nGroupsAfter = groupService.findRoot().getGroups().size();
		assertEquals(nGroupsBefore + 1, nGroupsAfter);
	}

}
