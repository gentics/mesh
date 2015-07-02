package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.service.GroupService;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.paging.PagingInfo;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class GroupTest extends AbstractBasicObjectTest {

	@Autowired
	private GroupService groupService;

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
		MeshAuthUser requestUser = RoutingContextHelper.getUser(rc);
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
		assertNotNull(groupService.findByName("guests"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		assertNotNull(groupService.findByUUID(getGroup().getUuid()));
	}

	@Test
	@Override
	public void testTransformation() {
		GroupResponse response = getGroup().transformToRest(getRequestUser());
		assertNotNull(response);
		assertEquals(getGroup().getUuid(), response.getUuid());
		assertEquals(getGroup().getName(), response.getName());
	}

	@Test
	@Override
	public void testCreateDelete() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testPermissionsOnObject() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testRead() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreate() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDelete() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdate() {
		getGroup().setName("changed");
		assertEquals("changed", getGroup().getName());
	}

	@Test
	@Override
	public void testReadPermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testDeletePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testUpdatePermission() {
		fail("Not yet implemented");
	}

	@Test
	@Override
	public void testCreatePermission() {
		fail("Not yet implemented");
	}

}
