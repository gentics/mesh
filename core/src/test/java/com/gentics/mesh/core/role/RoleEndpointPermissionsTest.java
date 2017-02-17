package com.gentics.mesh.core.role;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.context.MeshTestHelper.call;
import static com.gentics.mesh.test.context.MeshTestHelper.expectResponseMessage;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.AbstractRestEndpointTest;

public class RoleEndpointPermissionsTest extends AbstractRestEndpointTest {

	@Test
	public void testRevokeAllPermissionFromProject() {
		try (NoTx noTx = db.noTx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(true);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "projects/" + project().getUuid(), request));
			expectResponseMessage(message, "role_updated_permission", role().getName());

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
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(),
					"projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid(), request));
			expectResponseMessage(message, "role_updated_permission", role().getName());

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
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "microschemas/" + vcard.getUuid(), request));
			expectResponseMessage(message, "role_updated_permission", role().getName());

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
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			assertTrue("The role should have delete permission on the group.", role().hasPermission(GraphPermission.DELETE_PERM, group()));

			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), pathToElement, request));
			expectResponseMessage(message, "role_updated_permission", role().getName());
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
			RolePermissionResponse response = call(() -> client().readRolePermissions(role().getUuid(), pathToElement));
			assertNotNull(response);
			assertThat(response).hasPerm(Permission.values());
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
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);

			GenericMessageResponse message = call(
					() -> client().updateRolePermissions(role().getUuid(), "projects/" + project().getUuid() + "/nodes/" + node.getUuid(), request));
			expectResponseMessage(message, "role_updated_permission", role().getName());

			assertTrue(role().hasPermission(GraphPermission.UPDATE_PERM, node));
		}
	}

	@Test
	public void testAddPermissionToNonExistingProject() {
		try (NoTx noTx = db.noTx()) {
			RolePermissionRequest request = new RolePermissionRequest();
			request.getPermissions().add(READ);
			String path = "projects/bogus1234/nodes";
			call(() -> client().updateRolePermissions(role().getUuid(), path, request), NOT_FOUND, "error_element_for_path_not_found", path);
		}
	}
}
