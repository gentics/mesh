package com.gentics.mesh.core.role;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.schema.MicroschemaContainer;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModel;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.syncleus.ferma.tx.Tx;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class RoleEndpointPermissionsTest extends AbstractMeshTest {

	@Test
	public void testRevokeAllPermissionFromProject() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		try (Tx tx = tx()) {
			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(true);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "projects/" + project().getUuid(), request));
			assertThat(message).matches("role_updated_permission", role().getName());
			assertFalse(role().hasPermission(GraphPermission.READ_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testRevokeAllPermissionFromProjectByName() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		try (Tx tx = tx()) {
			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(true);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "projects/" + PROJECT_NAME, request));
			assertThat(message).matches("role_updated_permission", role().getName());
			assertFalse(role().hasPermission(GraphPermission.READ_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToProjectTagFamily() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		try (Tx tx = tx()) {
			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(),
				"projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid(), request));
			assertThat(message).matches("role_updated_permission", role().getName());

			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToMicroschema() {
		try (Tx tx = tx()) {

			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			MicroschemaContainer vcard = microschemaContainer("vcard");

			// Revoke all permissions to vcard microschema
			role().revokePermissions(vcard, GraphPermission.values());
			tx.success();
		}

		try (Tx tx = tx()) {
			// Validate revocation
			MicroschemaContainer vcard = microschemaContainer("vcard");
			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, vcard));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "microschemas/" + vcard.getUuid(), request));
			assertThat(message).matches("role_updated_permission", role().getName());

			assertFalse(role().hasPermission(GraphPermission.DELETE_PERM, vcard));
			assertTrue(role().hasPermission(GraphPermission.UPDATE_PERM, vcard));
			assertTrue(role().hasPermission(GraphPermission.CREATE_PERM, vcard));
			assertTrue(role().hasPermission(GraphPermission.READ_PERM, vcard));
		}
	}

	@Test
	public void testSetOnlyCreatePerm() {
		String pathToElement = "groups";

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().add(CREATE);

		tx(() -> {
			assertTrue("The role should have read permission on the group.", role().hasPermission(GraphPermission.READ_PERM, group()));
		});

		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));
		assertThat(message).matches("role_updated_permission", tx(() -> role().getName()));

		tx(() -> {
			assertFalse("The role should no longer have read permission on the group.", role().hasPermission(GraphPermission.READ_PERM, group()));
		});
	}

	@Test
	public void testAddPermissionsOnGroup() {
		String pathToElement = "groups";
		String roleName = tx(() -> role().getName());

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().add(READ);
		request.getPermissions().add(UPDATE);
		request.getPermissions().add(CREATE);

		try (Tx tx = tx()) {
			assertTrue("The role should have delete permission on the group.", role().hasPermission(GraphPermission.DELETE_PERM, group()));
		}

		expect(ROLE_PERMISSIONS_CHANGED).match(1, PermissionChangedEventModel.class, event -> {
			assertEquals("The role name in the event did not match.", roleName, event.getName());
			assertEquals("The role uuid in the event did not match.", roleUuid(), event.getUuid());
			return true;
		});

		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));
		assertThat(message).matches("role_updated_permission", roleName);

		awaitEvents();

		try (Tx tx = tx()) {
			assertFalse("The role should no longer have delete permission on the group.", role().hasPermission(GraphPermission.DELETE_PERM, group()));
		}

	}

	@Test
	public void testGrantPermToProjectByName() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
		}

		String pathToElement = PROJECT_NAME + "/tagFamilies/" + tx(() -> tagFamily("colors").getUuid());
		RolePermissionResponse response = call(() -> client().readRolePermissions(roleUuid(), pathToElement));
		assertThat(response).hasPerm(Permission.values());

		response = call(() -> client().readRolePermissions(roleUuid(), "/" + PROJECT_NAME));
		assertThat(response).hasPerm(Permission.values());

		tx(() -> role().revokePermissions(project(), DELETE_PERM));

		response = call(() -> client().readRolePermissions(roleUuid(), "/" + PROJECT_NAME));
		assertThat(response).hasNoPerm(DELETE);

		ProjectResponse projectResponse = call(() -> client().findProjectByUuid(projectUuid()));
		assertFalse(projectResponse.getPermissions().hasPerm(DELETE));

	}

	@Test
	public void testReadPermissionsOnProjectTagFamily() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		String pathToElement = tx(() -> "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid());
		RolePermissionResponse response = call(() -> client().readRolePermissions(roleUuid(), pathToElement));
		assertNotNull(response);
		assertThat(response).hasPerm(Permission.values());
	}

	@Test
	public void testApplyPermissionsOnTag() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			role().revokePermissions(tag("red"), DELETE_PERM);
			tx.success();
		}

		String pathToElement = tx(
			() -> "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/tags/" + tag("red").getUuid());
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().setDelete(true);
		call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));

		try (Tx tx = tx()) {
			assertTrue(role().hasPermission(DELETE_PERM, tag("red")));
		}
	}

	@Test
	public void testApplyPermissionsOnTags() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.DELETE_PERM, tagFamily("colors")));
			role().revokePermissions(tag("red"), DELETE_PERM);
			tx.success();
		}

		// TODO - This action will currently only affect the tag family. We need to decide how we want to change this behaviour:
		// https://github.com/gentics/mesh/issues/154
		String pathToElement = tx(() -> "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/tags");
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().setDelete(true);
		call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));
		try (Tx tx = tx()) {
			assertFalse("The perm of the tag should not change since the action currently only affects the tag family itself",
				role().hasPermission(DELETE_PERM, tag("red")));
			assertTrue("The tag family perm did not change", role().hasPermission(DELETE_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testApplyCreatePermissionsOnTagFamily() {
		try (Tx tx = tx()) {
			// Add permission on own role
			role().grantPermissions(role(), GraphPermission.UPDATE_PERM);
			role().revokePermissions(tagFamily("colors"), CREATE_PERM);
			role().revokePermissions(tag("red"), CREATE_PERM);
			assertFalse(role().hasPermission(GraphPermission.CREATE_PERM, tagFamily("colors")));
			tx.success();
		}

		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		TagFamilyResponse tagFamilyResponse = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagFamilyUuid));
		assertFalse(tagFamilyResponse.getPermissions().hasPerm(CREATE));

		String pathToElement = tx(() -> "projects/" + project().getUuid() + "/tagFamilies/" + tagFamilyUuid);
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().setOthers(true);
		call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));
		try (Tx tx = tx()) {
			assertFalse("The perm of the tag should not change since the action currently only affects the tag family itself",
				role().hasPermission(CREATE_PERM, tag("red")));
			assertTrue("The tag family perm did not change", role().hasPermission(CREATE_PERM, tagFamily("colors")));
		}

		tagFamilyResponse = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagFamilyUuid));
		assertTrue(tagFamilyResponse.getPermissions().hasPerm(CREATE));
	}

	@Test
	public void testAddRecursivePermissionsToNodes() {

		String roleUuid;
		try (Tx tx = tx()) {
			Group testGroup = boot().groupRoot().create("testGroup", user());
			Role testRole = boot().roleRoot().create("testRole", user());
			User testUser = boot().userRoot().create("test", user());
			testUser.setPassword("dummy");

			testGroup.addRole(testRole);
			testGroup.addUser(testUser);
			roleUuid = testRole.getUuid();
			role().grantPermissions(testRole, GraphPermission.values());
			tx.success();
		}

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().setRead(true);
		request.getPermissions().setOthers(false);

		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid, "projects/" + projectUuid() + "/nodes", request));
		assertThat(message).matches("role_updated_permission", "testRole");

		request.getPermissions().setUpdate(true);
		message = call(() -> client().updateRolePermissions(roleUuid, "projects/" + projectUuid() + "/nodes", request));
		assertThat(message).matches("role_updated_permission", "testRole");

		client().logout().blockingGet();
		client().setLogin("test", "dummy");
		client().login().blockingGet();

		NodeListResponse nodeList = call(() -> client().findNodes(PROJECT_NAME));
		System.out.println(nodeList.toJson());
		for (NodeResponse node : nodeList.getData()) {
			assertThat(node.getPermissions()).as("Node uuid: " + node.getUuid()).hasPerm(READ, UPDATE).hasNoPerm(CREATE, DELETE)
				.hasPerm(READ_PUBLISHED);
		}

		NodeResponse response = call(() -> client().findNodeByUuid(PROJECT_NAME, contentUuid()));
		assertThat(response.getPermissions()).hasPerm(READ, UPDATE).hasNoPerm(CREATE, DELETE).hasPerm(READ_PUBLISHED);

	}

	@Test
	public void testAddPermissionToNode() {
		try (Tx tx = tx()) {
			Node node = folder("2015");
			role().revokePermissions(node, GraphPermission.UPDATE_PERM);
			assertFalse(role().hasPermission(GraphPermission.UPDATE_PERM, node));
			assertTrue(user().hasPermission(role(), GraphPermission.UPDATE_PERM));
			tx.success();
		}

		try (Tx tx = tx()) {
			Node node = folder("2015");
			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(
				() -> client().updateRolePermissions(role().getUuid(), "projects/" + project().getUuid() + "/nodes/" + node.getUuid(), request));
			assertThat(message).matches("role_updated_permission", role().getName());

			assertTrue(role().hasPermission(GraphPermission.UPDATE_PERM, node));
			assertTrue(role().hasPermission(GraphPermission.CREATE_PERM, node));
			assertTrue(role().hasPermission(GraphPermission.READ_PERM, node));
		}

	}

	@Test
	public void testAddPermissionToNonExistingProject() {
		try (Tx tx = tx()) {
			RolePermissionRequest request = new RolePermissionRequest();
			request.getPermissions().add(READ);
			String path = "projects/bogus1234/nodes";
			call(() -> client().updateRolePermissions(role().getUuid(), path, request), NOT_FOUND, "error_element_for_path_not_found", path);
		}
	}
}
