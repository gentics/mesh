package com.gentics.mesh.core.role;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Set;

import org.junit.Test;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.MeshVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.MeshAuthUserImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.RoleRoot;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

import io.vertx.ext.web.RoutingContext;

public class RoleTest extends AbstractBasicObjectTest {

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		InternalActionContext ac = getMockedInternalActionContext("");
		RoleReference reference = role().transformToReference();
		assertNotNull(reference);
		assertEquals(role().getUuid(), reference.getUuid());
		assertEquals(role().getName(), reference.getName());
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		String roleName = "test";
		RoleRoot root = meshRoot().getRoleRoot();
		Role createdRole = root.create(roleName, user());
		assertNotNull(createdRole);
		String uuid = createdRole.getUuid();
		Role role = boot.roleRoot().findByUuid(uuid).toBlocking().single();
		assertNotNull(role);
		assertEquals(roleName, role.getName());
	}

	@Test
	public void testGrantPermission() {
		Role role = role();
		Node node = content("news overview");
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

	@Test
	public void testGrantDuplicates() {
		Role role = meshRoot().getRoleRoot().create("testRole", user());
		group().addRole(role);
		NodeImpl extraNode = tx.getGraph().addFramedVertex(NodeImpl.class);
		assertEquals(0, countEdges(role, READ_PERM.label(), Direction.OUT));
		role.grantPermissions(extraNode, READ_PERM);
		assertEquals(1, countEdges(role, READ_PERM.label(), Direction.OUT));
		role.grantPermissions(extraNode, READ_PERM);
		assertEquals("We already got a permission edge. No additional edge should have been created.", 1,
				countEdges(role, READ_PERM.label(), Direction.OUT));
	}

	private long countEdges(MeshVertex vertex, String label, Direction direction) {
		long count = 0;
		for (Edge edge : vertex.getImpl().getElement().getEdges(direction, label)) {
			count++;
		}
		return count;
	}

	@Test
	public void testIsPermitted() throws Exception {
		User user = user();
		InternalActionContext ac = getMockedInternalActionContext("");
		int nRuns = 2000;
		for (int i = 0; i < nRuns; i++) {
			user.hasPermissionAsync(ac, folder("news"), READ_PERM);
		}
	}

	@Test
	public void testGrantPermissionTwice() {
		Role role = role();
		Node node = content("news overview");

		role.grantPermissions(node, CREATE_PERM);
		role.grantPermissions(node, CREATE_PERM);

		Set<GraphPermission> permissions = role.getPermissions(node);
		assertNotNull(permissions);
		assertTrue(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertTrue(permissions.contains(UPDATE_PERM));
	}

	@Test
	public void testGetPermissions() {
		Role role = role();
		Node node = content("news overview");
		assertEquals(4, role.getPermissions(node).size());
	}

	@Test
	public void testRevokePermission() {
		Role role = role();
		Node node = content("news overview");
		role.revokePermissions(node, CREATE_PERM);

		Set<GraphPermission> permissions = role.getPermissions(node);
		assertNotNull(permissions);
		assertFalse(permissions.contains(CREATE_PERM));
		assertTrue(permissions.contains(DELETE_PERM));
		assertTrue(permissions.contains(UPDATE_PERM));
		assertTrue(permissions.contains(READ_PERM));
	}

	@Test
	public void testRevokePermissionOnGroupRoot() throws Exception {
		role().revokePermissions(meshRoot().getGroupRoot(), CREATE_PERM);
		InternalActionContext ac = getMockedInternalActionContext("");
		User user = user();
		assertFalse("The create permission to the groups root node should have been revoked.",
				user.hasPermissionSync(ac, meshRoot().getGroupRoot(), CREATE_PERM));
	}

	@Test
	@Override
	public void testRootNode() {
		RoleRoot root = meshRoot().getRoleRoot();
		int nRolesBefore = root.findAll().size();

		final String roleName = "test2";
		Role role = root.create(roleName, user());
		assertNotNull(role);
		int nRolesAfter = root.findAll().size();
		assertEquals(nRolesBefore + 1, nRolesAfter);
	}

	@Test
	public void testRoleAddCrudPermissions() {
		MeshAuthUser requestUser = user().getImpl().reframe(MeshAuthUserImpl.class);
		// userRoot.findMeshAuthUserByUsername(requestUser.getUsername())
		Node parentNode = folder("2015");
		assertNotNull(parentNode);

		parentNode.reload();
		// Also assign create permissions on the parent object to other roles
		for (Role role : roles().values()) {
			role.reload();
			role.grantPermissions(parentNode, CREATE_PERM);
		}

		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		Node node = parentNode.create(user(), getSchemaContainer().getLatestVersion(), project());
		assertEquals(0, requestUser.getPermissions(ac, node).size());
		requestUser.addCRUDPermissionOnRole(parentNode, CREATE_PERM, node);
		ac.data().clear();
		assertEquals(4, requestUser.getPermissions(ac, node).size());

		try (Trx tx = db.trx()) {
			for (Role role : roles().values()) {
				for (GraphPermission permission : GraphPermission.values()) {
					assertTrue(
							"The role {" + role.getName() + "} does not grant perm {" + permission.getSimpleName() + "} to the node {"
									+ node.getUuid() + "} but it should since the parent object got this role permission.",
							role.hasPermission(permission, node));
				}
			}
		}
	}

	@Test
	public void testRolesOfGroup() throws InvalidArgumentException {
		Role extraRole;
		RoleRoot root = meshRoot().getRoleRoot();
		extraRole = root.create("extraRole", user());
		group().addRole(extraRole);

		// Multiple add role calls should not affect the result
		group().addRole(extraRole);
		group().addRole(extraRole);
		group().addRole(extraRole);
		group().addRole(extraRole);

		role().grantPermissions(extraRole, READ_PERM);
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();
		PageImpl<? extends Role> roles = group().getRoles(requestUser, new PagingParameter(1, 10));
		assertEquals(2, roles.getSize());
		assertEquals(1, extraRole.getGroups().size());

		// assertEquals(2, roles.getTotalElements());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		PageImpl<? extends Role> page = boot.roleRoot().findAll(ac, new PagingParameter(1, 5));
		assertEquals(roles().size(), page.getTotalElements());
		assertEquals(4, page.getSize());

		page = boot.roleRoot().findAll(ac, new PagingParameter(1, 15));
		assertEquals(roles().size(), page.getTotalElements());
		assertEquals(4, page.getSize());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNotNull(boot.roleRoot().findByName(role().getName()).toBlocking().single());
		assertNull(boot.roleRoot().findByName("bogus").toBlocking().single());
	}

	@Test
	@Override
	public void testFindByUUID() {
		Role role = boot.roleRoot().findByUuid(role().getUuid()).toBlocking().single();
		assertNotNull(role);
		role = boot.roleRoot().findByUuid("bogus").toBlocking().single();
		assertNull(role);
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		Role role = role();
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		RoleResponse restModel = role.transformToRest(ac).toBlocking().single();

		assertNotNull(restModel);
		assertEquals(role.getName(), restModel.getName());
		assertEquals(role.getUuid(), restModel.getUuid());

	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		String roleName = "test";
		RoleRoot root = meshRoot().getRoleRoot();

		Role role = root.create(roleName, user());
		String uuid = role.getUuid();
		role = boot.roleRoot().findByUuid(uuid).toBlocking().single();
		assertNotNull(role);
		role.delete();
		Role foundRole = boot.roleRoot().findByUuid(uuid).toBlocking().single();
		assertNull(foundRole);

	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		InternalActionContext ac = getMockedInternalActionContext("");
		Role role = root.getRoleRoot().create("SuperUser", user());
		assertFalse(user().hasPermissionAsync(ac, role, GraphPermission.CREATE_PERM).toBlocking().first());
		user().addCRUDPermissionOnRole(root.getUserRoot(), GraphPermission.CREATE_PERM, role);
		ac.data().clear();
		assertTrue(user().hasPermissionAsync(ac, role, GraphPermission.CREATE_PERM).toBlocking().first());
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		List<? extends Role> roles = boot.roleRoot().findAll();
		assertNotNull(roles);
		assertEquals(roles().size(), roles.size());
	}

	@Test
	@Override
	public void testRead() {
		Role role = role();
		assertEquals("joe1_role", role.getName());
		assertNotNull(role.getUuid());

		assertNotNull(role.getCreationTimestamp());
		assertNotNull(role.getCreator());

		assertNotNull(role.getEditor());
		assertNotNull(role.getLastEditedTimestamp());
	}

	@Test
	@Override
	public void testDelete() throws Exception {
		String uuid;
		try (Trx tx = db.trx()) {
			Role role = role();
			uuid = role.getUuid();
			role.delete();
			tx.success();
		}
		assertElement(boot.roleRoot(), uuid, false);

	}

	@Test
	@Override
	public void testUpdate() {
		Role role = role();
		role.setName("newName");
		assertEquals("newName", role.getName());
		// assertEquals(1,role.getProjects());
		// TODO test project assignments
	}

	@Test
	@Override
	public void testReadPermission() {
		testPermission(GraphPermission.READ_PERM, role());
	}

	@Test
	@Override
	public void testDeletePermission() {
		testPermission(GraphPermission.DELETE_PERM, role());
	}

	@Test
	@Override
	public void testUpdatePermission() {
		testPermission(GraphPermission.UPDATE_PERM, role());
	}

	@Test
	@Override
	public void testCreatePermission() {
		testPermission(GraphPermission.CREATE_PERM, role());
	}
}
