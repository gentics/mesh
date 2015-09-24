package com.gentics.mesh.core.group;

import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class GroupTest extends AbstractBasicObjectTest {

	@Test
	public void testUserGroup() {
		UserRoot userRoot = meshRoot().getUserRoot();
		GroupRoot groupRoot = meshRoot().getGroupRoot();

		Group group = groupRoot.create("test group", user());
		User user = userRoot.create("testuser", group, user());

		assertEquals("The group should contain one member.", 1, group.getUsers().size());

		User userOfGroup = group.getUsers().iterator().next();
		assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();
		Page<? extends Group> page = boot.groupRoot().findAll(requestUser, new PagingInfo(1, 19));

		assertEquals(groups().size(), page.getTotalElements());
		assertEquals(groups().size(), page.getSize());

		page = boot.groupRoot().findAll(requestUser, new PagingInfo(1, 3));
		assertEquals(groups().size(), page.getTotalElements());
		assertEquals(3, page.getSize());
	}

	@Test
	@Override
	public void testFindAll() {
		List<? extends Group> groups = boot.groupRoot().findAll();
		for (Group group : groups) {
			System.out.println("G: " + group.getName());
		}
		assertEquals(groups().size(), groups.size());

	}

	@Test
	@Override
	public void testRootNode() {
		GroupRoot root = meshRoot().getGroupRoot();
		int nGroupsBefore = root.findAll().size();
		GroupRoot groupRoot = meshRoot().getGroupRoot();
		assertNotNull(groupRoot.create("test group2", user()));

		int nGroupsAfter = root.findAll().size();
		assertEquals(nGroupsBefore + 1, nGroupsAfter);
	}

	@Test
	@Override
	public void testFindByName() {
		assertNotNull(boot.groupRoot().findByName("guests"));
	}

	@Test
	@Override
	public void testFindByUUID() {
		boot.groupRoot().findByUuid(group().getUuid(), rh -> {
			assertNotNull(rh.result());
		});
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		group().transformToRest(ac, rh -> {
			GroupResponse response = rh.result();
			assertNotNull(response);
			assertEquals(group().getUuid(), response.getUuid());
			assertEquals(group().getName(), response.getName());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		Group group = meshRoot().getGroupRoot().create("newGroup", user());
		assertNotNull(group);
		String uuid = group.getUuid();
		group.delete();
		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getGroupRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		User user = user();
		InternalActionContext ac = getMockedInternalActionContext("");
		Group group = root.getGroupRoot().create("newGroup", user);
		assertFalse(user.hasPermission(ac, group, GraphPermission.CREATE_PERM));
		user.addCRUDPermissionOnRole(root.getGroupRoot(), GraphPermission.CREATE_PERM, group);
		assertTrue(user.hasPermission(ac, group, GraphPermission.CREATE_PERM));
	}

	@Test
	@Override
	public void testRead() {
		Group group = group();
		assertEquals("joe1_group", group.getName());
		assertNotNull(group.getUsers());
		assertEquals(1, group.getUsers().size());
		assertNotNull(group.getUuid());
	}

	@Test
	@Override
	public void testCreate() {
		Group group = meshRoot().getGroupRoot().create("newGroup", user());
		assertNotNull(group);
		assertEquals("newGroup", group.getName());
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		Group group = meshRoot().getGroupRoot().create("newGroup", user());
		assertNotNull(group);
		assertEquals("newGroup", group.getName());
		String uuid = group.getUuid();
		// TODO add users to group?
		group.delete();
		CountDownLatch latch = new CountDownLatch(1);
		meshRoot().getGroupRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testUpdate() {
		group().setName("changed");
		assertEquals("changed", group().getName());
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(GraphPermission.READ_PERM, group());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(GraphPermission.DELETE_PERM, group());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(GraphPermission.UPDATE_PERM, group());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(GraphPermission.CREATE_PERM, group());
	}

}
