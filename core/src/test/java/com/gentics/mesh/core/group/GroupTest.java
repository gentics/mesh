package com.gentics.mesh.core.group;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.test.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.dao.OrientDBGroupDao;
import com.gentics.mesh.core.data.dao.OrientDBUserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.user.HibUser;
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
			OrientDBUserDao userDao = tx.userDao();
			OrientDBGroupDao groupDao = tx.groupDao();

			HibGroup group = groupDao.create("test group", user());
			HibUser user = userDao.create("testuser", user());
			groupDao.addUser(group, user);
			groupDao.addUser(group, user);
			groupDao.addUser(group, user);

			assertEquals("The group should contain one member.", 1, groupDao.getUsers(group).count());

			HibUser userOfGroup = groupDao.getUsers(group).iterator().next();
			assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		int groupCount = groups().size();
		try (Tx tx = tx()) {
			OrientDBGroupDao groupDao = tx.groupDao();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			Page<? extends HibGroup> page = groupDao.findAll(ac, new PagingParametersImpl(1, 19L));

			assertEquals(groupCount, page.getTotalElements());
			assertEquals(groupCount, page.getSize());

			page = groupDao.findAll(ac, new PagingParametersImpl(1, 3L));
			assertEquals(groupCount, page.getTotalElements());
			assertEquals("We expected one page per group.", groupCount, page.getSize());
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (Tx tx = tx()) {
			long size = tx.groupDao().globalCount();
			assertEquals(groups().size(), size);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			OrientDBGroupDao groupDao = tx.groupDao();
			long nGroupsBefore = groupDao.globalCount();
			assertNotNull(groupDao.create("test group2", user()));

			long nGroupsAfter = groupDao.globalCount();
			assertEquals(nGroupsBefore + 1, nGroupsAfter);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			assertNotNull(tx.groupDao().findByName(group().getName()));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (Tx tx = tx()) {
			HibGroup group = tx.groupDao().findByUuid(group().getUuid());
			assertNotNull(group);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			OrientDBGroupDao groupDao = tx.groupDao();
			GroupResponse response = groupDao.transformToRestSync(group(), ac, 0);

			assertNotNull(response);
			assertEquals(group().getUuid(), response.getUuid());
			assertEquals(group().getName(), response.getName());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			OrientDBGroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.create("newGroup", user());
			assertNotNull(group);
			String uuid = group.getUuid();
			groupDao.delete(group, createBulkContext());
			group = meshRoot().getGroupRoot().findByUuid(uuid);
			assertNull(group);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			MeshRoot root = meshRoot();
			OrientDBUserDao userDao = tx.userDao();
			OrientDBGroupDao groupDao = tx.groupDao();
			HibUser user = user();
			InternalActionContext ac = mockActionContext();
			HibGroup group = groupDao.create("newGroup", user);
			assertFalse(userDao.hasPermission(user, group, InternalPermission.CREATE_PERM));
			userDao.inheritRolePermissions(user, root.getGroupRoot(), group);
			ac.data().clear();
			assertTrue(userDao.hasPermission(user, group, InternalPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			OrientDBGroupDao groupDao = tx.groupDao();
			HibGroup group = group();
			assertEquals("joe1_group", group.getName());
			assertNotNull(groupDao.getUsers(group));
			assertEquals(1, groupDao.getUsers(group).count());
			assertNotNull(group.getUuid());
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (Tx tx = tx()) {
			OrientDBGroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.create("newGroup", user());
			assertNotNull(group);
			assertEquals("newGroup", group.getName());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (Tx tx = tx()) {
			OrientDBGroupDao groupDao = tx.groupDao();
			HibGroup group = groupDao.create("newGroup", user());

			assertNotNull(group);
			assertEquals("newGroup", group.getName());
			String uuid = group.getUuid();
			String userUuid = user().getUuid();
			groupDao.addUser(group(), user());

			// TODO add users to group?
			BulkActionContext bac = createBulkContext();
			groupDao.delete(group, bac);
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
			testPermission(InternalPermission.READ_PERM, group());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.DELETE_PERM, group());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.UPDATE_PERM, group());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			testPermission(InternalPermission.CREATE_PERM, group());
		}
	}

}
