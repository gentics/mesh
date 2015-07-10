package com.gentics.mesh.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.ext.web.RoutingContext;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.Permission;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.gentics.mesh.util.RoutingContextHelper;

public class GroupTest extends AbstractBasicObjectTest {

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
		Page<? extends Group> page = boot.groupRoot().findAll(requestUser, new PagingInfo(1, 19));

		assertEquals(data().getGroups().size(), page.getTotalElements());
		assertEquals(data().getGroups().size(), page.getSize());

		page = boot.groupRoot().findAll(requestUser, new PagingInfo(1, 3));
		assertEquals(data().getGroups().size(), page.getTotalElements());
		assertEquals(3, page.getSize());
	}

	@Test
	@Override
	public void testFindAll() {
		List<? extends Group> groups = boot.groupRoot().findAll();
		for (Group group : groups) {
			System.out.println("G: " + group.getName());
		}
		assertEquals(data().getGroups().size(), groups.size());

	}

	@Test
	@Override
	public void testRootNode() {
		GroupRoot root = data().getMeshRoot().getGroupRoot();
		int nGroupsBefore = root.findAll().size();
		GroupRoot groupRoot = data().getMeshRoot().getGroupRoot();
		assertNotNull(groupRoot.create("test group2"));

		int nGroupsAfter = root.findAll().size();
		assertEquals(nGroupsBefore + 1, nGroupsAfter);
	}

	@Test
	@Override
	public void testFindByName() {
		System.out.println(boot.groupRoot().getClass().getName());
		assertNotNull(boot.groupRoot().findByName("guests"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		assertNotNull(boot.groupRoot().findByUuid(getGroup().getUuid()));
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
		Group group = getMeshRoot().getGroupRoot().create("newGroup");
		assertNotNull(group);
		String uuid = group.getUuid();
		group.delete();
		assertNull(getMeshRoot().getGroupRoot().findByUuid(uuid));
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = getMeshRoot();
		User user = getUser();
		Group group = root.getGroupRoot().create("newGroup");
		assertFalse(user.hasPermission(group, Permission.CREATE_PERM));
		user.addCRUDPermissionOnRole(root.getGroupRoot(), Permission.CREATE_PERM, group);
		assertTrue(user.hasPermission(group, Permission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() {
		Group group = getGroup();
		assertEquals("joe1_group", group.getName());
		assertNotNull(group.getUsers());
		assertEquals(1, group.getUsers().size());
		assertNotNull(group.getUuid());
	}

	@Test
	@Override
	public void testCreate() {
		Group group = getMeshRoot().getGroupRoot().create("newGroup");
		assertNotNull(group);
		assertEquals("newGroup", group.getName());
	}

	@Test
	@Override
	public void testDelete() {
		Group group = getMeshRoot().getGroupRoot().create("newGroup");
		assertNotNull(group);
		assertEquals("newGroup", group.getName());
		String uuid = group.getUuid();
		//TODO add users to group?
		group.delete();
		assertNull(getMeshRoot().getGroupRoot().findByUuid(uuid));
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
		testPermission(Permission.READ_PERM, getGroup());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(Permission.DELETE_PERM, getGroup());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(Permission.UPDATE_PERM, getGroup());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(Permission.CREATE_PERM, getGroup());
	}

}
