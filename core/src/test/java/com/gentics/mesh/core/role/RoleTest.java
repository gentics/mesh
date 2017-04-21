package com.gentics.mesh.core.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.test.TestSize.PROJECT;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(useElasticsearch = false, testSize = PROJECT, startServer = false)
public class RoleTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTx noTx = db().noTx()) {
			RoleReference reference = role().transformToReference();
			assertNotNull(reference);
			assertEquals(role().getUuid(), reference.getUuid());
			assertEquals(role().getName(), reference.getName());
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (NoTx noTx = db().noTx()) {
			String roleName = "test";
			RoleRoot root = meshRoot().getRoleRoot();
			Role createdRole = root.create(roleName, user());
			assertNotNull(createdRole);
			String uuid = createdRole.getUuid();
			Role role = boot().roleRoot().findByUuid(uuid);
			assertNotNull(role);
			assertEquals(roleName, role.getName());
		}
	}

	@Test
	public void testGrantPermission() {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			Node node = folder("news");
			role.grantPermissions(node, CREATE_PERM, READ_PERM, UPDATE_PERM, DELETE_PERM);

			// node2
			Node parentNode = folder("2015");
			Node node2 = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project());
			// NodeFieldContainer englishContainer = node2.getFieldContainer(english());
			// englishContainer.setI18nProperty("content", "Test");
			role.grantPermissions(node2, READ_PERM, DELETE_PERM);
			role.grantPermissions(node2, CREATE_PERM);
			Set<GraphPermission> permissions = role.getPermissions(node2);

			assertNotNull(permissions);
			assertTrue(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertFalse(permissions.contains(UPDATE_PERM));
			role.grantPermissions(role, CREATE_PERM);
		}
	}

	@Test
	public void testGrantDuplicates() {
		try (NoTx noTx = db().noTx()) {
			Role role = meshRoot().getRoleRoot().create("testRole", user());
			group().addRole(role);
			NodeImpl extraNode = noTx.getGraph().addFramedVertex(NodeImpl.class);
			assertEquals(0, countEdges(role, READ_PERM.label(), Direction.OUT));
			role.grantPermissions(extraNode, READ_PERM);
			assertEquals(1, countEdges(role, READ_PERM.label(), Direction.OUT));
			role.grantPermissions(extraNode, READ_PERM);
			assertEquals("We already got a permission edge. No additional edge should have been created.", 1,
					countEdges(role, READ_PERM.label(), Direction.OUT));
		}
	}

	private long countEdges(MeshVertex vertex, String label, Direction direction) {
		long count = 0;
		Iterator<Edge> it = vertex.getElement().getEdges(direction, label).iterator();
		while (it.hasNext()) {
			it.next();
			count++;
		}
		return count;
	}

	@Test
	public void testIsPermitted() throws Exception {
		try (NoTx noTx = db().noTx()) {
			User user = user();
			int nRuns = 2000;
			for (int i = 0; i < nRuns; i++) {
				user.hasPermission(folder("news"), READ_PERM);
			}
		}
	}

	@Test
	public void testGrantPermissionTwice() {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			Node node = folder("news");

			role.grantPermissions(node, CREATE_PERM);
			role.grantPermissions(node, CREATE_PERM);

			Set<GraphPermission> permissions = role.getPermissions(node);
			assertNotNull(permissions);
			assertTrue(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertTrue(permissions.contains(UPDATE_PERM));
		}
	}

	@Test
	public void testGetPermissions() {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			Node node = folder("news");
			assertEquals(6, role.getPermissions(node).size());
		}
	}

	@Test
	public void testRevokePermission() {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			Node node = folder("news");
			role.revokePermissions(node, CREATE_PERM);

			Set<GraphPermission> permissions = role.getPermissions(node);
			assertNotNull(permissions);
			assertFalse(permissions.contains(CREATE_PERM));
			assertTrue(permissions.contains(DELETE_PERM));
			assertTrue(permissions.contains(UPDATE_PERM));
			assertTrue(permissions.contains(READ_PERM));
		}
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {
		try (NoTx noTx = db().noTx()) {
			role().revokePermissions(meshRoot().getGroupRoot(), CREATE_PERM);
			User user = user();
			assertFalse("The create permission to the groups root node should have been revoked.",
					user.hasPermission(meshRoot().getGroupRoot(), CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (NoTx noTx = db().noTx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			int nRolesBefore = root.findAll().size();

			final String roleName = "test2";
			Role role = root.create(roleName, user());
			assertNotNull(role);
			int nRolesAfter = root.findAll().size();
			assertEquals(nRolesBefore + 1, nRolesAfter);
		}
	}

	@Test
	public void testRoleAddCrudPermissions() {
		try (NoTx noTx = db().noTx()) {
			MeshAuthUser requestUser = user().reframe(MeshAuthUserImpl.class);
			// userRoot.findMeshAuthUserByUsername(requestUser.getUsername())
			Node parentNode = folder("news");
			assertNotNull(parentNode);

			parentNode.reload();
			// Grant all permissions to all roles
			for (Role role : roles().values()) {
				role.reload();
				for (GraphPermission perm : GraphPermission.values()) {
					role.grantPermissions(parentNode, perm);
				}
			}

			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			Node node = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project());
			assertEquals(0, requestUser.getPermissions(node).size());
			requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
			ac.data().clear();
			assertEquals(6, requestUser.getPermissions(node).size());

			try (Tx tx = db().tx()) {
				for (Role role : roles().values()) {
					for (GraphPermission permission : GraphPermission.values()) {
						assertTrue(
								"The role {" + role.getName() + "} does not grant perm {"
										+ permission.getRestPerm().getName() + "} to the node {" + node.getUuid()
										+ "} but it should since the parent object got this role permission.",
								role.hasPermission(permission, node));
					}
				}
			}
		}
	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {

		try (NoTx noTx = db().noTx()) {
			RoleRoot root = meshRoot().getRoleRoot();
			Role extraRole = root.create("extraRole", user());
			group().addRole(extraRole);

			// Multiple add role calls should not affect the result
			group().addRole(extraRole);
			group().addRole(extraRole);
			group().addRole(extraRole);
			group().addRole(extraRole);

			role().grantPermissions(extraRole, READ_PERM);
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			MeshAuthUser requestUser = ac.getUser();
			Page<? extends Role> roles = group().getRoles(requestUser, new PagingParametersImpl(1, 10));
			assertEquals(2, roles.getSize());
			assertEquals(1, extraRole.getGroups().size());

			// assertEquals(2, roles.getTotalElements());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		int roleCount = roles().size();
		try (NoTx noTx = db().noTx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			Page<? extends Role> page = boot().roleRoot().findAll(ac, new PagingParametersImpl(1, 5));
			assertEquals(roleCount, page.getTotalElements());
			assertEquals(roleCount, page.getSize());

			page = boot().roleRoot().findAll(ac, new PagingParametersImpl(1, 15));
			assertEquals(roleCount, page.getTotalElements());
			assertEquals(roleCount, page.getSize());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTx noTx = db().noTx()) {
			assertNotNull(boot().roleRoot().findByName(role().getName()));
			assertNull(boot().roleRoot().findByName("bogus"));
		}
	}

	@Test
	@Override
	public void testFindByUUID() {
		try (NoTx noTx = db().noTx()) {
			Role role = boot().roleRoot().findByUuid(role().getUuid());
			assertNotNull(role);
			role = boot().roleRoot().findByUuid("bogus");
			assertNull(role);
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			RoleResponse restModel = role.transformToRest(ac, 0).toBlocking().value();

			assertNotNull(restModel);
			assertEquals(role.getName(), restModel.getName());
			assertEquals(role.getUuid(), restModel.getUuid());
		}

	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (NoTx noTx = db().noTx()) {
			String roleName = "test";
			RoleRoot root = meshRoot().getRoleRoot();

			Role role = root.create(roleName, user());
			String uuid = role.getUuid();
			role = boot().roleRoot().findByUuid(uuid);
			assertNotNull(role);
			SearchQueueBatch batch = createBatch();
			role.delete(batch);
			Role foundRole = boot().roleRoot().findByUuid(uuid);
			assertNull(foundRole);
		}

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (NoTx noTx = db().noTx()) {
			MeshRoot root = meshRoot();
			InternalActionContext ac = mockActionContext();
			Role role = root.getRoleRoot().create("SuperUser", user());
			assertFalse(user().hasPermission(role, GraphPermission.CREATE_PERM));
			user().addCRUDPermissionOnRole(root.getUserRoot(), GraphPermission.CREATE_PERM, role);
			ac.data().clear();
			assertTrue(user().hasPermission(role, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (NoTx noTx = db().noTx()) {
			List<? extends Role> roles = boot().roleRoot().findAll();
			assertNotNull(roles);
			assertEquals(roles().size(), roles.size());
		}
	}

	@Test
	@Override
	public void testRead() {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			assertEquals("joe1_role", role.getName());
			assertNotNull(role.getUuid());

			assertNotNull(role.getCreationTimestamp());
			assertNotNull(role.getCreator());

			assertNotNull(role.getEditor());
			assertNotNull(role.getLastEditedTimestamp());
		}
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		try (NoTx noTx = db().noTx()) {
			String uuid;
			SearchQueueBatch batch = createBatch();
			try (Tx tx = db().tx()) {
				Role role = role();
				uuid = role.getUuid();
				role.delete(batch);
				tx.success();
			}
			assertElement(boot().roleRoot(), uuid, false);

			// Check role entry
			// Optional<? extends SearchQueueEntry> roleEntry = batch.findEntryByUuid(uuid);
			// assertThat(roleEntry).isPresent();
			// assertEquals(DELETE_ACTION, roleEntry.get().getElementAction());
			//
			// Optional<? extends SearchQueueEntry> groupEntry = batch.findEntryByUuid(group().getUuid());
			// assertThat(groupEntry).isPresent();
			// assertEquals(STORE_ACTION, groupEntry.get().getElementAction());

			assertEquals(2, batch.getEntries().size());
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (NoTx noTx = db().noTx()) {
			Role role = role();
			role.setName("newName");
			assertEquals("newName", role.getName());
			// assertEquals(1,role.getProjects());
			// TODO test project assignments
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (NoTx noTx = db().noTx()) {
			testPermission(GraphPermission.READ_PERM, role());
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (NoTx noTx = db().noTx()) {
			testPermission(GraphPermission.DELETE_PERM, role());
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (NoTx noTx = db().noTx()) {
			testPermission(GraphPermission.UPDATE_PERM, role());
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (NoTx noTx = db().noTx()) {
			testPermission(GraphPermission.CREATE_PERM, role());
		}
	}
}
