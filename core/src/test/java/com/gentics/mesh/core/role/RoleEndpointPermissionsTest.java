package com.gentics.mesh.core.role;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_PERMISSIONS_CHANGED;
import static com.gentics.mesh.core.rest.MeshEvent.ROLE_UPDATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.TRACKING;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.gentics.mesh.ElementType;
import com.gentics.mesh.core.data.Tx;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.schema.HibMicroschema;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.event.role.PermissionChangedEventModelImpl;
import com.gentics.mesh.core.rest.node.NodeListResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.core.rest.role.RolePermissionRequest;
import com.gentics.mesh.core.rest.role.RolePermissionResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

@MeshTestSetting(elasticsearch = TRACKING, testSize = FULL, startServer = true)
public class RoleEndpointPermissionsTest extends AbstractMeshTest {

	@Test
	public void testRevokeAllPermissionFromProject() {

		final String roleName = tx(() -> role().getName());

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();

			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		// All elements in the project should be affected.
		// +2 for the project and the branch
		int totalEvents = getNodeCount() + tagFamilies().size() + tags().size() + 2;

		expect(ROLE_PERMISSIONS_CHANGED).match(totalEvents, PermissionChangedEventModelImpl.class, event -> {
			RoleReference roleRef = event.getRole();
			assertEquals("The uuid of the role did not match for the event.", roleUuid(), roleRef.getUuid());
			assertEquals("The name of the role did not match for the event.", roleName, roleRef.getName());
		}).total(totalEvents);

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().setOthers(false);
		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid(), "projects/" + projectUuid(), request));

		awaitEvents();
		waitForSearchIdleEvent();

		long nodecontainerCount = tx(() -> getAllContents().count());

		// +1 for Project (Branch is not indexed)
		long updateEvents = nodecontainerCount + tagFamilies().size() + tags().size() + 1;

		assertThat(trackingSearchProvider()).hasEvents(0, updateEvents, 0, 0, 0);

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertThat(message).matches("role_updated_permission", role().getName());
			assertFalse(roleDao.hasPermission(role(), InternalPermission.READ_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testRevokeAllPermissionFromProjectByName() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(true);
			request.getPermissions().setOthers(false);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "projects/" + PROJECT_NAME, request));
			assertThat(message).matches("role_updated_permission", role().getName());
			assertFalse(roleDao.hasPermission(role(), InternalPermission.READ_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToProjectTagFamily() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().add(READ);
		request.getPermissions().add(UPDATE);
		request.getPermissions().add(CREATE);
		request.getPermissions().setOthers(false);
		String tagFamilyUuid = tx(() -> tagFamily("colors").getUuid());
		String roleName = tx(() -> role().getName());
		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid(),
			"projects/" + projectUuid() + "/tagFamilies/" + tagFamilyUuid, request));
		assertThat(message).matches("role_updated_permission", roleName);

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertFalse(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testAddPermissionToMicroschema() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();

			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			HibMicroschema vcard = microschemaContainer("vcard");

			// Revoke all permissions to vcard microschema
			roleDao.revokePermissions(role(), vcard, InternalPermission.values());
			tx.success();
		}

		HibMicroschema vcard;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();

			// Validate revocation
			vcard = microschemaContainer("vcard");
			assertFalse(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, vcard));

			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(() -> client().updateRolePermissions(role().getUuid(), "microschemas/" + vcard.getUuid(), request));
			assertThat(message).matches("role_updated_permission", role().getName());
		}

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertFalse(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, vcard));
			assertTrue(roleDao.hasPermission(role(), InternalPermission.UPDATE_PERM, vcard));
			assertTrue(roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, vcard));
			assertTrue(roleDao.hasPermission(role(), InternalPermission.READ_PERM, vcard));
		}
	}

	@Test
	public void testSetOnlyCreatePerm() {
		String pathToElement = "groups";

		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(true);
		request.getPermissions().add(CREATE);
		request.getPermissions().setOthers(false);

		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			assertTrue("The role should have read permission on the group.", roleDao.hasPermission(role(), InternalPermission.READ_PERM, group()));
		});

		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));
		assertThat(message).matches("role_updated_permission", tx(() -> role().getName()));

		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			assertFalse("The role should no longer have read permission on the group.",
				roleDao.hasPermission(role(), InternalPermission.READ_PERM, group()));
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
		request.getPermissions().setOthers(false);

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertTrue("The role should have delete permission on the group.", roleDao.hasPermission(role(), DELETE_PERM, group()));
		}

		expect(ROLE_PERMISSIONS_CHANGED).match(9, PermissionChangedEventModelImpl.class, event -> {
			RoleReference roleRef = event.getRole();
			assertEquals("The role name in the event did not match.", roleName, roleRef.getName());
			assertEquals("The role uuid in the event did not match.", roleUuid(), roleRef.getUuid());
			ElementType type = event.getType();
			switch (type) {
			case ROLE:
				assertThat(event.getName()).as("The listed roles should have been affected.").containsPattern("anonymous|joe1_role");
				break;
			case USER:
				assertThat(event.getName()).as("All users in the groups should be affected due to recursive true.")
					.containsPattern("joe1|anonymous|guest|admin");
				break;
			case GROUP:
				assertThat(event.getName()).as("All groups should be affected.")
					.containsPattern("anonymous|joe1_group|extra_group|guests|admin");
				break;
			default:
				fail("Unexpected event for type {" + type + "}");
			}

		}).total(9);
		expect(ROLE_UPDATED).none();
		expect(USER_UPDATED).none();
		expect(GROUP_UPDATED).none();

		GenericMessageResponse message = call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));
		assertThat(message).matches("role_updated_permission", roleName);

		awaitEvents();

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertFalse("The role should no longer have delete permission on the group.", roleDao.hasPermission(role(), DELETE_PERM, group()));
		}

	}

	@Test
	public void testGrantPermToProjectByName() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
		}

		String pathToElement = PROJECT_NAME + "/tagFamilies/" + tx(() -> tagFamily("colors").getUuid());
		RolePermissionResponse response = call(() -> client().readRolePermissions(roleUuid(), pathToElement));
		assertThat(response).hasPerm(Permission.basicPermissions());

		response = call(() -> client().readRolePermissions(roleUuid(), "/" + PROJECT_NAME));
		assertThat(response).hasPerm(Permission.basicPermissions());

		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), project(), DELETE_PERM);
		});

		response = call(() -> client().readRolePermissions(roleUuid(), "/" + PROJECT_NAME));
		assertThat(response).hasNoPerm(DELETE);

		ProjectResponse projectResponse = call(() -> client().findProjectByUuid(projectUuid()));
		assertFalse(projectResponse.getPermissions().hasPerm(DELETE));

	}

	@Test
	public void testReadPermissionsOnProjectTagFamily() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
			tx.success();
		}

		String pathToElement = tx(() -> "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid());
		RolePermissionResponse response = call(() -> client().readRolePermissions(roleUuid(), pathToElement));
		assertNotNull(response);
		assertThat(response).hasPerm(Permission.basicPermissions());
	}

	@Test
	public void testApplyPermissionsOnTag() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
			roleDao.revokePermissions(role(), tag("red"), DELETE_PERM);
			tx.success();
		}

		String pathToElement = tx(
			() -> "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily("colors").getUuid() + "/tags/" + tag("red").getUuid());
		RolePermissionRequest request = new RolePermissionRequest();
		request.setRecursive(false);
		request.getPermissions().setDelete(true);
		call(() -> client().updateRolePermissions(roleUuid(), pathToElement, request));

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertTrue(roleDao.hasPermission(role(), DELETE_PERM, tag("red")));
		}
	}

	@Test
	public void testApplyPermissionsOnTags() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.DELETE_PERM, tagFamily("colors")));
			roleDao.revokePermissions(role(), tag("red"), DELETE_PERM);
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
			RoleDao roleDao = tx.roleDao();
			assertFalse("The perm of the tag should not change since the action currently only affects the tag family itself",
				roleDao.hasPermission(role(), DELETE_PERM, tag("red")));
			assertTrue("The tag family perm did not change", roleDao.hasPermission(role(), DELETE_PERM, tagFamily("colors")));
		}
	}

	@Test
	public void testApplyCreatePermissionsOnTagFamily() {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Add permission on own role
			roleDao.grantPermissions(role(), role(), InternalPermission.UPDATE_PERM);
			roleDao.revokePermissions(role(), tagFamily("colors"), CREATE_PERM);
			roleDao.revokePermissions(role(), tag("red"), CREATE_PERM);
			assertFalse(roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, tagFamily("colors")));
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
			RoleDao roleDao = tx.roleDao();
			assertFalse("The perm of the tag should not change since the action currently only affects the tag family itself",
				roleDao.hasPermission(role(), CREATE_PERM, tag("red")));
			assertTrue("The tag family perm did not change", roleDao.hasPermission(role(), CREATE_PERM, tagFamily("colors")));
		}

		tagFamilyResponse = call(() -> client().findTagFamilyByUuid(PROJECT_NAME, tagFamilyUuid));
		assertTrue(tagFamilyResponse.getPermissions().hasPerm(CREATE));
	}

	@Test
	public void testAddRecursivePermissionsToNodes() {

		String roleUuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			GroupDao groupDao = tx.groupDao();
			HibGroup testGroup = groupDao.create("testGroup", user());
			HibRole testRole = tx.roleDao().create("testRole", user());
			HibUser testUser = tx.userDao().create("test", user());
			tx.userDao().setPassword(testUser, "dummy");

			groupDao.addRole(testGroup, testRole);
			groupDao.addUser(testGroup, testUser);
			roleUuid = testRole.getUuid();
			roleDao.grantPermissions(role(), testRole, InternalPermission.values());
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
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibNode node = folder("2015");
			roleDao.revokePermissions(role(), node, InternalPermission.UPDATE_PERM);
			assertFalse(roleDao.hasPermission(role(), InternalPermission.UPDATE_PERM, node));
			assertTrue(userDao.hasPermission(user(), role(), InternalPermission.UPDATE_PERM));
			tx.success();
		}

		HibNode node;
		try (Tx tx = tx()) {
			node = folder("2015");
			RolePermissionRequest request = new RolePermissionRequest();
			request.setRecursive(false);
			request.getPermissions().add(READ);
			request.getPermissions().add(UPDATE);
			request.getPermissions().add(CREATE);
			GenericMessageResponse message = call(
				() -> client().updateRolePermissions(role().getUuid(), "projects/" + project().getUuid() + "/nodes/" + node.getUuid(), request));
			assertThat(message).matches("role_updated_permission", role().getName());
		}

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			assertTrue(roleDao.hasPermission(role(), InternalPermission.UPDATE_PERM, node));
			assertTrue(roleDao.hasPermission(role(), InternalPermission.CREATE_PERM, node));
			assertTrue(roleDao.hasPermission(role(), InternalPermission.READ_PERM, node));
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
