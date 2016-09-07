package com.gentics.mesh.core.role;

import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractIsolatedRestVerticleTest;

import io.vertx.core.AbstractVerticle;

public class RoleVerticlePermissionsTest extends AbstractIsolatedRestVerticleTest {

	@Override
	public List<AbstractVerticle> getAdditionalVertices() {
		List<AbstractVerticle> list = new ArrayList<>();
		list.add(meshDagger.roleVerticle());
		return list;
	}

	@Test
	public void testRevokeAllPermissionFromProject() {
		try (NoTx noTx = db.noTx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(true);
			MeshResponse<GenericMessageResponse> future = getClient().updateRolePermissions(role().getUuid(), "projects/" + project().getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "role_updated_permission", role().getName());

			assertFalse(role().hasPermission(GraphPermission.READ_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToProjectTagFamily() {
		try (NoTx noTx = db.noTx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add("read");
			request.getPermissions().add("update");
			request.getPermissions().add("create");
			MeshResponse<GenericMessageResponse> future = getClient().updateRolePermissions(role().getUuid(),
					"projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "role_updated_permission", role().getName());

			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToMicroschema() {
		try (NoTx noTx = db.noTx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			MicroschemaContainer vcard = microschemaContainer("vcard");
			// Revoke all permissions to vcard microschema
			role().revokePermissions(vcard, GraphPermission.values());
			// Validate revocation
			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, vcard));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add("read");
			request.getPermissions().add("update");
			request.getPermissions().add("create");
			MeshResponse<GenericMessageResponse> future = getClient().updateRolePermissions(role().getUuid(), "microschemas/" + vcard.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "role_updated_permission", role().getName());

			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, vcard));
			assertTrue(role().hasPermission(GraphPermission.UPDATE_PERM, vcard));
			assertTrue(role().hasPermission(GraphPermission.CREATE_PERM, vcard));
			assertTrue(role().hasPermission(GraphPermission.READ_PERM, vcard));
		}
	}

	@Test
	public void testAddPermissionsOnGroup() {
		try (NoTx noTx = db.noTx()) {
			String pathToElement = "groups";

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(true);
			request.getPermissions().add("read");
			request.getPermissions().add("update");
			request.getPermissions().add("create");
			assertTrue("The role should have delete permission on the group.", role().hasPermission(GraphPermission.DELETE_PERM, group()));

			MeshResponse<GenericMessageResponse> future = getClient().updateRolePermissions(role().getUuid(), pathToElement, request).invoke();
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "role_updated_permission", role().getName());
			assertFalse("The role should no longer have delete permission on the group.", role().hasPermission(GraphPermission.DELETE_PERM, group()));
		}

	}

	@Test
	public void testReadPermissionsOnProjectTagFamily() {
		try (NoTx noTx = db.noTx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));

			String pathToElement = "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid();
			MeshResponse<RolePermissionResponse> future = getClient().readRolePermissions(role().getUuid(), pathToElement).invoke();
			latchFor(future);
			assertSuccess(future);
			RolePermissionResponse response = future.result();
			assertNotNull(response.getPermissions());
			assertEquals(6, response.getPermissions().size());
		}
	}

	@Test
	public void testAddPermissionToNode() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			role().revokePermissions(node, GraphPermission.UPDATE_PERM);
			assertFalse(role().hasPermission(GraphPermission.UPDATE_PERM, node));
			assertTrue(user().hasPermission(role(), GraphPermission.UPDATE_PERM));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add("read");
			request.getPermissions().add("update");
			request.getPermissions().add("create");

			MeshResponse<GenericMessageResponse> future = getClient().updateRolePermissions(role().getUuid(),
					"projects/" + project().getUuid() + "/nodes/" + node.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
			expectResponseMessage(future, "role_updated_permission", role().getName());

			assertTrue(role().hasPermission(GraphPermission.UPDATE_PERM, node));
		}
	}
}
