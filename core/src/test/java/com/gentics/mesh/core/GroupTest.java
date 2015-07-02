package com.gentics.mesh.core;

import static com.gentics.mesh.util.RoutingContextHelper.getUser;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractDBTest;
import com.gentics.mesh.util.InvalidArgumentException;

public class GroupTest extends AbstractDBTest implements BasicObjectTestcases {

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

		User user = userRoot.create("testuser");
		Group group = groupRoot.create("test group");
		group.addUser(user);

		assertEquals("The group should contain one member.", 1, group.getUsers().size());

		User userOfGroup = group.getUsers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		MeshAuthUser requestUser = getUser(rc);
		Page<? extends Group> page = groupService.findAll(requestUser, new PagingInfo(1, 10));
		assertEquals(data().getUsers().size(), page.getTotalElements());
		assertEquals(10, page.getSize());

		page = groupService.findAll(requestUser, new PagingInfo(1, 15));
		assertEquals(data().getUsers().size(), page.getTotalElements());
		assertEquals(15, page.getSize());
	}

	@Test
	@Override
	public void testFindAll() {
		List<? extends Group> groups = groupService.findAll();
		assertNotNull(groups);
		assertEquals(data().getGroups().size(), groups.size());
	}

	@Test
	@Override
	public void testRootNode() {
		GroupRoot root = data().getMeshRoot().getGroupRoot();
		int nGroupsBefore = root.getGroups().size();
		GroupRoot groupRoot = data().getMeshRoot().getGroupRoot();
		assertNotNull(groupRoot.create("test group2"));

		int nGroupsAfter = root.getGroups().size();
		assertEquals(nGroupsBefore + 1, nGroupsAfter);
	}

	@Test
	@Override
	public void testFindByName() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testFindByUUID() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testTransformation() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreateDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testRead() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDelete() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdate() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testReadPermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testDeletePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testUpdatePermission() {
		// TODO Auto-generated method stub

	}

	@Test
	@Override
	public void testCreatePermission() {
		// TODO Auto-generated method stub

	}

}
