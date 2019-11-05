package com.gentics.mesh.core.group;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = PROJECT, startServer = false)
public class GroupTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			GroupReference reference = group().transformToReference();
			assertNotNull(reference);
			assertEquals(group().getUuid(), reference.getUuid());
			assertEquals(group().getName(), reference.getName());
		}
	}

	@Test
	public void testUserGroup() {
		try (Tx tx = tx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			GroupRoot groupRoot = meshRoot().getGroupRoot();

			Group group = groupRoot.create("test group", user());
			User user = userRoot.create("testuser", user());
			group.addUser(user);
			group.addUser(user);
			group.addUser(user);

			assertEquals("The group should contain one member.", 1, group.getUsers().count());

			User userOfGroup = group.getUsers().iterator().next();
			assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		int groupCount = groups().size();
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			Page<? extends Group> page = boot().groupRoot().findAll(ac, new PagingParametersImpl(1, 19L));

			assertEquals(groupCount, page.getTotalElements());
			assertEquals(groupCount, page.getSize());

			page = boot().groupRoot().findAll(ac, new PagingParametersImpl(1, 3L));
			assertEquals(groupCount, page.getTotalElements());
			assertEquals("We expected one page per group.", groupCount, page.getSize());
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (Tx tx = tx()) {
			long size = boot().groupRoot().computeCount();
			assertEquals(groups().size(), size);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			GroupRoot root = meshRoot().getGroupRoot();
			long nGroupsBefore = root.computeCount();
			GroupRoot groupRoot = meshRoot().getGroupRoot();
			assertNotNull(groupRoot.create("test group2", user()));

			long nGroupsAfter = root.computeCount();
			assertEquals(nGroupsBefore + 1, nGroupsAfter);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			assertNotNull(boot().groupRoot().findByName(group().getName()));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (Tx tx = tx()) {
			Group group = boot().groupRoot().findByUuid(group().getUuid());
			assertNotNull(group);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);

			GroupResponse response = group().transformToRest(ac, 0).blockingGet();

			assertNotNull(response);
			assertEquals(group().getUuid(), response.getUuid());
			assertEquals(group().getName(), response.getName());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			Group group = meshRoot().getGroupRoot().create("newGroup", user());
			assertNotNull(group);
			String uuid = group.getUuid();
			group.delete(createBulkContext());
			group = meshRoot().getGroupRoot().findByUuid(uuid);
			assertNull(group);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			MeshRoot root = meshRoot();
			User user = user();
			InternalActionContext ac = mockActionContext();
			Group group = root.getGroupRoot().create("newGroup", user);
			assertFalse(user.hasPermission(group, GraphPermission.CREATE_PERM));
			user.inheritRolePermissions(root.getGroupRoot(), group);
			ac.data().clear();
			assertTrue(user.hasPermission(group, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			Group group = group();
			assertEquals("joe1_group", group.getName());
			assertNotNull(group.getUsers());
			assertEquals(1, group.getUsers().count());
			assertNotNull(group.getUuid());
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (Tx tx = tx()) {
			Group group = meshRoot().getGroupRoot().create("newGroup", user());
			assertNotNull(group);
			assertEquals("newGroup", group.getName());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			Group group = meshRoot().getGroupRoot().create("newGroup", user());

			assertNotNull(group);
			assertEquals("newGroup", group.getName());
			String uuid = group.getUuid();
			String userUuid = user().getUuid();
			group().addUser(user());

			// TODO add users to group?
			BulkActionContext bac = createBulkContext();
			group.delete(bac);
			assertElement(meshRoot().getGroupRoot(), uuid, false);
			assertElement(meshRoot().getUserRoot(), userUuid, true);
			assertEquals(1, bac.batch().getEntries().size());
		}

	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			group().setName("changed");
			assertEquals("changed", group().getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.READ_PERM, group());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.DELETE_PERM, group());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.UPDATE_PERM, group());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(GraphPermission.CREATE_PERM, group());
		}
	}

}
