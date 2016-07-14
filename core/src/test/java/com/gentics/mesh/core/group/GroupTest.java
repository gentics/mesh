package com.gentics.mesh.core.group;

import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.GroupRoot;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.parameter.impl.PagingParameters;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.ext.web.RoutingContext;

public class GroupTest extends AbstractBasicIsolatedObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			GroupReference reference = group().transformToReference();
			assertNotNull(reference);
			assertEquals(group().getUuid(), reference.getUuid());
			assertEquals(group().getName(), reference.getName());
		}
	}

	@Test
	public void testUserGroup() {
		try (NoTrx noTx = db.noTrx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			GroupRoot groupRoot = meshRoot().getGroupRoot();

			Group group = groupRoot.create("test group", user());
			User user = userRoot.create("testuser", user());
			group.addUser(user);
			group.addUser(user);
			group.addUser(user);

			assertEquals("The group should contain one member.", 1, group.getUsers().size());

			User userOfGroup = group.getUsers().iterator().next();
			assertEquals("Username did not match the expected one.", user.getUsername(), userOfGroup.getUsername());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (NoTrx noTx = db.noTrx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);
			PageImpl<? extends Group> page = boot.groupRoot().findAll(ac, new PagingParameters(1, 19));

			assertEquals(groups().size(), page.getTotalElements());
			assertEquals(groups().size(), page.getSize());

			page = boot.groupRoot().findAll(ac, new PagingParameters(1, 3));
			assertEquals(groups().size(), page.getTotalElements());
			assertEquals(3, page.getSize());
		}
	}

	@Test
	@Override
	public void testFindAll() {
		try (NoTrx noTx = db.noTrx()) {
			List<? extends Group> groups = boot.groupRoot().findAll();
			assertEquals(groups().size(), groups.size());
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (NoTrx noTx = db.noTrx()) {
			GroupRoot root = meshRoot().getGroupRoot();
			int nGroupsBefore = root.findAll().size();
			GroupRoot groupRoot = meshRoot().getGroupRoot();
			assertNotNull(groupRoot.create("test group2", user()));

			int nGroupsAfter = root.findAll().size();
			assertEquals(nGroupsBefore + 1, nGroupsAfter);
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTrx noTx = db.noTrx()) {
			assertNotNull(boot.groupRoot().findByName("guests").toBlocking().value());
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (NoTrx noTx = db.noTrx()) {
			Group group = boot.groupRoot().findByUuid(group().getUuid()).toBlocking().value();
			assertNotNull(group);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);

			GroupResponse response = group().transformToRest(ac, 0).toBlocking().value();

			assertNotNull(response);
			assertEquals(group().getUuid(), response.getUuid());
			assertEquals(group().getName(), response.getName());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Group group = meshRoot().getGroupRoot().create("newGroup", user());
			SearchQueueBatch batch = createBatch();
			assertNotNull(group);
			String uuid = group.getUuid();
			group.delete(batch);
			group = meshRoot().getGroupRoot().findByUuid(uuid).toBlocking().value();
			assertNull(group);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (NoTrx noTx = db.noTrx()) {
			MeshRoot root = meshRoot();
			User user = user();
			InternalActionContext ac = getMockedInternalActionContext();
			Group group = root.getGroupRoot().create("newGroup", user);
			assertFalse(user.hasPermissionAsync(ac, group, GraphPermission.CREATE_PERM).toBlocking().value());
			user.addCRUDPermissionOnRole(root.getGroupRoot(), GraphPermission.CREATE_PERM, group);
			ac.data().clear();
			assertTrue(user.hasPermissionAsync(ac, group, GraphPermission.CREATE_PERM).toBlocking().value());
		}
	}

	@Test
	@Override
	public void testRead() {
		try (NoTrx noTx = db.noTrx()) {
			Group group = group();
			assertEquals("joe1_group", group.getName());
			assertNotNull(group.getUsers());
			assertEquals(1, group.getUsers().size());
			assertNotNull(group.getUuid());
		}
	}

	@Test
	@Override
	public void testCreate() {
		try (NoTrx noTx = db.noTrx()) {
			Group group = meshRoot().getGroupRoot().create("newGroup", user());
			assertNotNull(group);
			assertEquals("newGroup", group.getName());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (NoTrx noTx = db.noTrx()) {
			Group group = meshRoot().getGroupRoot().create("newGroup", user());

			assertNotNull(group);
			assertEquals("newGroup", group.getName());
			String uuid = group.getUuid();
			String userUuid = user().getUuid();
			group().addUser(user());

			// TODO add users to group?
			SearchQueueBatch batch = createBatch();
			group.delete(batch);
			assertElement(meshRoot().getGroupRoot(), uuid, false);
			assertElement(meshRoot().getUserRoot(), userUuid, true);
			assertThat(batch.findEntryByUuid(uuid)).isPresent();
			assertEquals(1, batch.getEntries().size());
		}

	}

	@Test
	@Override
	public void testUpdate() {
		try (NoTrx noTx = db.noTrx()) {
			group().setName("changed");
			assertEquals("changed", group().getName());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.READ_PERM, group());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.DELETE_PERM, group());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.UPDATE_PERM, group());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (NoTrx noTx = db.noTrx()) {
			testPermission(GraphPermission.CREATE_PERM, group());
		}
	}

}
