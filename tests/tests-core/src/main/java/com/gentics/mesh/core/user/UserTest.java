package com.gentics.mesh.core.user;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.TestSize.FULL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.BulkActionContext;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.context.impl.InternalRoutingActionContextImpl;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.service.BasicObjectTestcases;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.common.PermissionInfo;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.error.InvalidArgumentException;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.util.TestUtils;
import com.google.common.collect.Iterables;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

@MeshTestSetting(testSize = FULL, startServer = false)
public class UserTest extends AbstractMeshTest implements BasicObjectTestcases {

	@Test
	public void testCreatedUser() {
		try (Tx tx = tx()) {
			assertNotNull("The uuid of the user should not be null since the entity was reloaded.", user().getUuid());
		}
	}

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (Tx tx = tx()) {
			HibUser user = user();
			UserReference reference = user.transformToReference();
			assertNotNull(reference);
			assertEquals(user.getUuid(), reference.getUuid());
			assertEquals(user.getFirstname(), reference.getFirstName());
			assertEquals(user.getLastname(), reference.getLastName());
		}
	}

	@Test
	public void testLoadPrincipalWithoutTx() {
		MeshAuthUser user = tx(() -> getRequestMeshAuthUser());

		JsonObject json = user.principal();
		assertNotNull(json);
		assertEquals(userUuid(), json.getString("uuid"));
		assertEquals(tx(() -> user.getDelegate().getEmailAddress()), json.getString("emailAddress"));
		assertEquals(tx(() -> user.getDelegate().getLastname()), json.getString("lastname"));
		assertEquals(tx(() -> user.getDelegate().getFirstname()), json.getString("firstname"));
		assertEquals(tx(() -> user.getDelegate().getUsername()), json.getString("username"));

		JsonArray roles = json.getJsonArray("roles");
		for (int i = 0; i < roles.size(); i++) {
			JsonObject role = roles.getJsonObject(i);
			assertNotNull(role.getString("uuid"));
			assertNotNull(role.getString("name"));
		}
		assertEquals("The principal should contain two roles.", 1, roles.size());
		JsonArray groups = json.getJsonArray("groups");
		for (int i = 0; i < roles.size(); i++) {
			JsonObject group = groups.getJsonObject(i);
			assertNotNull(group.getString("uuid"));
			assertNotNull(group.getString("name"));
		}
		assertEquals("The principal should contain two groups.", 1, groups.size());
	}

	@Test
	public void testETag() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			InternalActionContext ac = mockActionContext();
			String eTag = userDao.getETag(user(), ac);
			System.out.println(eTag);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao= tx.userDao();
			int nUserBefore = Iterables.size(userDao.findAllGlobal());
			assertNotNull(userDao.create("dummy12345", user()));
			int nUserAfter = Iterables.size(userDao.findAllGlobal());
			assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);
		}
	}

	@Test
	public void testHasPermission() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = user();
			long start = System.currentTimeMillis();
			int nChecks = 9000;
			int runs = 90;
			for (int e = 0; e < runs; e++) {
				for (int i = 0; i < nChecks; i++) {
					assertTrue(userDao.hasPermission(user, content(), READ_PERM));
				}
			}
			long duration = System.currentTimeMillis() - start;
			System.out.println("Duration: " + duration);
			System.out.println("Duration per check: 	" + ((double) duration / (double) (nChecks * runs)));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);

			UserDaoWrapper userDao = tx.userDao();
			Page<? extends HibUser> page = userDao.findAll(ac, new PagingParametersImpl(1, 6L));
			assertEquals(users().size(), page.getTotalElements());
			assertEquals(users().size(), page.getSize());

			page = userDao.findAll(ac, new PagingParametersImpl(1, 15L));
			assertEquals(users().size(), page.getTotalElements());
			assertEquals(users().size(), page.getSize());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			Page<? extends HibUser> page = tx.userDao().findAll(mockActionContext(), new PagingParametersImpl(1, 25L));
			assertNotNull(page);
			assertEquals(users().size(), page.getTotalElements());
		}
	}

	@Test
	public void testGetPrincipal() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			RoutingContext rc = mockRoutingContext();
			io.vertx.ext.auth.User user = rc.user();
			assertNotNull(user);
			JsonObject json = user.principal();
			assertNotNull(json);
			try (Tx tx2 = tx()) {
				assertEquals(user().getUuid(), json.getString("uuid"));
				assertEquals(user().getUsername(), json.getString("username"));
				assertEquals(user().getFirstname(), json.getString("firstname"));
				assertEquals(user().getLastname(), json.getString("lastname"));
				assertEquals(user().getEmailAddress(), json.getString("emailAddress"));

				assertNotNull(json.getJsonArray("roles"));
				assertEquals(TestUtils.size(userDao.getRoles(user())), json.getJsonArray("roles").size());
				assertNotNull(json.getJsonArray("groups"));
				assertEquals(userDao.getGroups(user()).count(), json.getJsonArray("groups").size());
			}
		}
	}

	@Test
	public void testGetPermissions() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			Permission[] perms = { CREATE, UPDATE, DELETE, READ, READ_PUBLISHED, PUBLISH };
			long start = System.currentTimeMillis();
			int nChecks = 10000;
			for (int i = 0; i < nChecks; i++) {
				PermissionInfo loadedPermInfo = userDao.getPermissionInfo(user(), content());
				assertThat(loadedPermInfo).hasPerm(perms);
				// assertNotNull(ac.data().get("permissions:" + language.getUuid()));
			}
			System.out.println("Duration: " + (System.currentTimeMillis() - start));
			System.out.println("Duration per Check: " + (System.currentTimeMillis() - start) / (double) nChecks);
		}
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao= tx.userDao();
			RoleDaoWrapper roleDao = tx.roleDao();
			GroupDaoWrapper groupDao = tx.groupDao();

			HibUser extraUser = userDao.create("extraUser", user());
			groupDao.addUser(group(), extraUser);
			roleDao.grantPermissions(role(), extraUser, InternalPermission.READ_PERM);
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);
			HibUser requestUser = ac.getUser();
			Page<? extends HibUser> userPage = groupDao.getVisibleUsers(group(), requestUser, new PagingParametersImpl(1, 10L));

			assertEquals(2, userPage.getTotalElements());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			assertNull(userDao.findByUsername("bogus"));
			userDao.findByUsername(user().getUsername());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			String uuid = user().getUuid();
			HibUser foundUser = userDao.findByUuidGlobal(uuid);
			assertNotNull(foundUser);
			assertEquals(uuid, foundUser.getUuid());
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (Tx tx = tx()) {
			RoutingContext rc = mockRoutingContext();
			InternalActionContext ac = new InternalRoutingActionContextImpl(rc);

			UserDaoWrapper userDao= tx.userDao();
			UserResponse restUser = userDao.transformToRestSync(user(), ac, 0);

			assertNotNull(restUser);
			assertEquals(user().getUsername(), restUser.getUsername());
			assertEquals(user().getUuid(), restUser.getUuid());
			assertEquals(user().getLastname(), restUser.getLastname());
			assertEquals(user().getFirstname(), restUser.getFirstname());
			assertEquals(user().getEmailAddress(), restUser.getEmailAddress());
			assertEquals(1, restUser.getGroups().size());
		}
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = userDao.create("Anton", user());
			assertTrue(user.isEnabled());
			assertNotNull(user);
			String uuid = user.getUuid();
			BulkActionContext bac = createBulkContext();
			userDao.delete(user, bac);
			HibUser foundUser = userDao.findByUuidGlobal(uuid);
			assertNull(foundUser);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			UserRoot userRoot = boot().meshRoot().getUserRoot();

			HibUser user = user();
			HibUser newUser = userDao.create("Anton", user());
			assertFalse(userDao.hasPermission(user, newUser, InternalPermission.CREATE_PERM));
			userDao.inheritRolePermissions(user, userRoot, newUser);
			assertTrue(userDao.hasPermission(user, newUser, InternalPermission.CREATE_PERM));
		}
	}

	@Test
	public void testInheritPermissions() {
		try (Tx tx = tx()) {
			RoleDaoWrapper roleDao = tx.roleDao();
			UserDaoWrapper userDao = tx.userDao();
			GroupDaoWrapper groupDao = tx.groupDao();

			HibNode sourceNode = folder("news");
			HibNode targetNode = folder("2015");
			HibUser newUser;

			HibRole roleWithDeletePerm;
			HibRole roleWithReadPerm;
			HibRole roleWithUpdatePerm;
			HibRole roleWithAllPerm;
			HibRole roleWithNoPerm;
			HibRole roleWithCreatePerm;

			InternalActionContext ac = mockActionContext();

			HibGroup newGroup = groupDao.create("extraGroup", user());
			newUser = userDao.create("Anton", user());
			groupDao.addUser(newGroup, newUser);

			// Create test roles
			roleWithDeletePerm = roleDao.create("roleWithDeletePerm", newUser);
			groupDao.addRole(newGroup, roleWithDeletePerm);
			roleDao.grantPermissions(roleWithDeletePerm, sourceNode, InternalPermission.DELETE_PERM);

			roleWithReadPerm = roleDao.create("roleWithReadPerm", newUser);
			groupDao.addRole(newGroup, roleWithReadPerm);
			roleDao.grantPermissions(roleWithReadPerm, sourceNode, InternalPermission.READ_PERM);

			roleWithUpdatePerm = roleDao.create("roleWithUpdatePerm", newUser);
			groupDao.addRole(newGroup, roleWithUpdatePerm);
			roleDao.grantPermissions(roleWithUpdatePerm, sourceNode, InternalPermission.UPDATE_PERM);

			roleWithAllPerm = roleDao.create("roleWithAllPerm", newUser);
			groupDao.addRole(newGroup, roleWithAllPerm);
			roleDao.grantPermissions(roleWithAllPerm, sourceNode, InternalPermission.CREATE_PERM, InternalPermission.UPDATE_PERM, InternalPermission.DELETE_PERM,
				InternalPermission.READ_PERM, InternalPermission.READ_PUBLISHED_PERM, PUBLISH_PERM);

			roleWithCreatePerm = roleDao.create("roleWithCreatePerm", newUser);
			groupDao.addRole(newGroup, roleWithCreatePerm);
			roleDao.grantPermissions(roleWithCreatePerm, sourceNode, InternalPermission.CREATE_PERM);

			roleWithNoPerm = roleDao.create("roleWithNoPerm", newUser);
			groupDao.addRole(newGroup, roleWithNoPerm);
			userDao.inheritRolePermissions(user(), sourceNode, targetNode);
			ac.data().clear();
			for (InternalPermission perm : InternalPermission.values()) {
				assertTrue(
					"The new user should have all permissions to CRUD the target node since he is member of a group that has been assigned to roles with various permissions that cover CRUD. Failed for permission {"
						+ perm.name() + "}",
					userDao.hasPermission(newUser, targetNode, perm));
			}

			// roleWithAllPerm
			for (InternalPermission perm : InternalPermission.values()) {
				assertTrue("The role should grant all permissions to the target node. Failed for permission {" + perm.name() + "}", roleDao.hasPermission(roleWithAllPerm, perm, targetNode));
			}

			// roleWithNoPerm
			for (InternalPermission perm : InternalPermission.values()) {
				assertFalse(
					"No extra permissions should be assigned to the role that did not have any permissions on the source element. Failed for permission {"
						+ perm.name() + "}",
					roleDao.hasPermission(roleWithNoPerm, perm, targetNode));
			}

			// roleWithDeletePerm
			assertFalse("The role should only have delete permissions on the object", roleDao.hasPermission(roleWithDeletePerm, CREATE_PERM, targetNode));
			assertFalse("The role should only have delete permissions on the object", roleDao.hasPermission(roleWithDeletePerm, READ_PERM, targetNode));
			assertFalse("The role should only have delete permissions on the object", roleDao.hasPermission(roleWithDeletePerm, UPDATE_PERM, targetNode));
			assertTrue("The role should only have delete permissions on the object", roleDao.hasPermission(roleWithDeletePerm, DELETE_PERM, targetNode));

			// roleWithReadPerm
			assertFalse("The role should only have read permissions on the object", roleDao.hasPermission(roleWithReadPerm, CREATE_PERM, targetNode));
			assertTrue("The role should only have read permissions on the object", roleDao.hasPermission(roleWithReadPerm, READ_PERM, targetNode));
			assertFalse("The role should only have read permissions on the object", roleDao.hasPermission(roleWithReadPerm, UPDATE_PERM, targetNode));
			assertFalse("The role should only have read permissions on the object", roleDao.hasPermission(roleWithReadPerm, DELETE_PERM, targetNode));

			// roleWithUpdatePerm
			assertFalse("The role should only have update permissions on the object", roleDao.hasPermission(roleWithUpdatePerm, CREATE_PERM, targetNode));
			assertFalse("The role should only have update permissions on the object", roleDao.hasPermission(roleWithUpdatePerm, READ_PERM, targetNode));
			assertTrue("The role should only have update permissions on the object", roleDao.hasPermission(roleWithUpdatePerm, UPDATE_PERM, targetNode));
			assertFalse("The role should only have update permissions on the object", roleDao.hasPermission(roleWithUpdatePerm, DELETE_PERM, targetNode));
		}

	}

	@Test
	@Override
	public void testRead() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = user();
			assertEquals("joe1", user.getUsername());
			assertNotNull(user.getPasswordHash());
			assertEquals("Joe", user.getFirstname());
			assertEquals("Doe", user.getLastname());
			assertEquals("j.doe@spam.gentics.com", user.getEmailAddress());

			assertNotNull(user.getLastEditedTimestamp());
			assertNotNull(user.getCreator());
			assertNotNull(user.getEditor());
			assertNotNull(user.getCreationTimestamp());
			assertEquals(1, userDao.getGroups(user).count());
			assertNotNull(user);
		}
	}

	@Test
	public void testUserGroup() {
		try (Tx tx = tx()) {
			GroupDaoWrapper groupDao = tx.groupDao();
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = user();
			assertEquals(1, userDao.getGroups(user).count());

			for (int i = 0; i < 10; i++) {
				HibGroup extraGroup = groupDao.create("group_" + i, user());
				// Multiple calls should not affect the result
				groupDao.addUser(extraGroup, user);
				groupDao.addUser(extraGroup, user);
				groupDao.addUser(extraGroup, user);
				groupDao.addUser(extraGroup, user);
			}

			assertEquals(11, userDao.getGroups(user()).count());
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (Tx tx = tx()) {
			final String USERNAME = "test";
			final String EMAIL = "joe@nowhere.org";
			final String FIRSTNAME = "joe";
			final String LASTNAME = "doe";
			final String PASSWDHASH = "RANDOM";

			UserDaoWrapper userDao = tx.userDao();
			HibUser user = userDao.create(USERNAME, user());
			user.setEmailAddress(EMAIL);
			user.setFirstname(FIRSTNAME);
			user.setLastname(LASTNAME);
			user.setPasswordHash(PASSWDHASH);
			assertTrue(user.isEnabled());

			HibUser reloadedUser = userDao.findByUuidGlobal(user.getUuid());
			assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
			assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
			assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
			assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
			assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());
		}
	}

	@Test
	@Override
	public void testDelete() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = user();

			String uuid = user.getUuid();
			assertEquals(1, userDao.getGroups(user).count());
			assertTrue(user.isEnabled());
			BulkActionContext bac = createBulkContext();
			userDao.delete(user, bac);
			User foundUser = meshRoot().getUserRoot().findByUuid(uuid);
			assertNull(foundUser);
		}
	}

	@Test
	public void testOwnRolePerm() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			assertTrue("The user should have update permissions on his role", userDao.hasPermission(user(), role(), InternalPermission.UPDATE_PERM));
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser newUser = userDao.create("newUser", user());

			HibUser user = user();

			user.setEmailAddress("changed");
			assertEquals("changed", user.getEmailAddress());

			user.setFirstname("changed_firstname");
			assertEquals("changed_firstname", user.getFirstname());

			user.setLastname("changed_lastname");
			assertEquals("changed_lastname", user.getLastname());

			user.setEditor(newUser);
			assertNotNull(user.getEditor());
			assertEquals(newUser.getUuid(), user.getEditor().getUuid());
			user.setLastEditedTimestamp(1);
			assertEquals(1, user.getLastEditedTimestamp().longValue());

			user.setCreator(newUser);
			assertNotNull(user.getCreator());
			assertEquals(newUser.getUuid(), user.getCreator().getUuid());

			user.setCreationTimestamp(0);
			assertEquals(0, user.getCreationTimestamp().longValue());

			assertTrue(user.isEnabled());
			user.disable();
			assertFalse(user.isEnabled());

			assertNotNull(user.getPasswordHash());
		}
	}

	@Test
	@Override
	public void testReadPermission() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = userDao.create("Anton", user());
			testPermission(InternalPermission.READ_PERM, user);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = userDao.create("Anton", user());
			testPermission(InternalPermission.DELETE_PERM, user);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = userDao.create("Anton", user());
			testPermission(InternalPermission.UPDATE_PERM, user);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (Tx tx = tx()) {
			UserDaoWrapper userDao = tx.userDao();
			HibUser user = userDao.create("Anton", user());
			testPermission(InternalPermission.CREATE_PERM, user);
		}
	}

	@Test
	public void testUserRolesHashes() {
		try (Tx tx = tx()) {
			GroupDaoWrapper groupDao = tx.groupDao();
			UserDaoWrapper userDao= tx.userDao();

			HibUser oldUser = user();
			HibUser newUser = userDao.create("newuser", oldUser);
			HibGroup newGroup = groupDao.create("newgroup", oldUser);

			groupDao.getRoles(group()).forEach(role -> groupDao.addRole(newGroup, role));
			groupDao.addUser(newGroup, newUser);

			// Both groups have the same roles, so the hashes must match.
			assertEquals(oldUser.getRolesHash(), newUser.getRolesHash());

			String hash = oldUser.getRolesHash();

			// Add another role to the groups only oldUser is in.
			grantAdmin();

			// The roles have changed for oldUser ...
			assertNotEquals(hash, oldUser.getRolesHash());
			// ... but NOT for newUser.
			assertEquals(hash, newUser.getRolesHash());
		}
	}
}
