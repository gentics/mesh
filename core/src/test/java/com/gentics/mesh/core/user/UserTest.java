package com.gentics.mesh.core.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.mock.Mocks.getMockedInternalActionContext;
import static com.gentics.mesh.mock.Mocks.getMockedRoutingContext;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.test.AbstractBasicIsolatedObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserTest extends AbstractBasicIsolatedObjectTest {

	@Test
	public void testCreatedUser() {
		try (NoTx noTx = db.noTx()) {
			assertNotNull("The uuid of the user should not be null since the entity was reloaded.", user().getUuid());
		}
	}

	@Test
	@Override
	public void testTransformToReference() throws Exception {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			UserReference reference = user.transformToReference();
			assertNotNull(reference);
			assertEquals(user.getUuid(), reference.getUuid());
			assertEquals(user.getFirstname(), reference.getFirstName());
			assertEquals(user.getLastname(), reference.getLastName());
		}
	}

	@Test
	public void testETag() {
		InternalActionContext ac = getMockedInternalActionContext();
		try (NoTx noTx = db.noTx()) {
			User user = user();
			String eTag = user.getETag(ac);
			System.out.println(eTag);
		}
	}

	@Test
	@Override
	public void testRootNode() {
		try (NoTx noTx = db.noTx()) {
			UserRoot root = meshRoot().getUserRoot();
			int nUserBefore = root.findAll().size();
			assertNotNull(root.create("dummy12345", user()));
			int nUserAfter = root.findAll().size();
			assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);
		}
	}

	@Test
	public void testHasPermission() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			long start = System.currentTimeMillis();
			int nChecks = 9000;
			int runs = 90;
			for (int e = 0; e < runs; e++) {
				for (int i = 0; i < nChecks; i++) {
					assertTrue(user.hasPermission(content(), READ_PERM));
				}
			}
			long duration = System.currentTimeMillis() - start;
			System.out.println("Duration: " + duration);
			System.out.println("Duration per check: " + ((double) duration / (double) (nChecks * runs)));
		}
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);

			Page<? extends User> page = boot.userRoot().findAll(ac, new PagingParametersImpl(1, 6));
			assertEquals(users().size(), page.getTotalElements());
			assertEquals(3, page.getSize());

			page = boot.userRoot().findAll(ac, new PagingParametersImpl(1, 15));
			assertEquals(users().size(), page.getTotalElements());
			assertEquals(users().size(), page.getSize());
		}
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			Page<? extends User> page = boot.userRoot().findAll(getMockedInternalActionContext(null, user()), new PagingParametersImpl(1, 25));
			assertNotNull(page);
			assertEquals(users().size(), page.getTotalElements());
		}
	}

	@Test
	public void testGetPrincipal() {
		try (NoTx noTx = db.noTx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			io.vertx.ext.auth.User user = rc.user();
			assertNotNull(user);
			JsonObject json = user.principal();
			assertNotNull(json);
			try (Tx tx = db.tx()) {
				assertEquals(user().getUuid(), json.getString("uuid"));
				assertEquals(user().getUsername(), json.getString("username"));
				assertEquals(user().getFirstname(), json.getString("firstname"));
				assertEquals(user().getLastname(), json.getString("lastname"));
				assertEquals(user().getEmailAddress(), json.getString("emailAddress"));

				assertNotNull(json.getJsonArray("roles"));
				assertEquals(user().getRoles().size(), json.getJsonArray("roles").size());
				assertNotNull(json.getJsonArray("groups"));
				assertEquals(user().getGroups().size(), json.getJsonArray("groups").size());
			}
		}
	}

	@Test
	public void testSetNameAlias() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			user.setName("test123");
			assertEquals("test123", user.getName());
			assertEquals("test123", user.getUsername());
		}
	}

	@Test
	public void testGetPermissions() {
		try (NoTx noTx = db.noTx()) {
			String[] perms = { "create", "update", "delete", "read", "readpublished", "publish" };
			long start = System.currentTimeMillis();
			int nChecks = 10000;
			for (int i = 0; i < nChecks; i++) {
				String[] loadedPerms = user().getPermissionNames(content());
				assertThat(loadedPerms).containsOnly(perms);
				// assertNotNull(ac.data().get("permissions:" + language.getUuid()));
			}
			System.out.println("Duration: " + (System.currentTimeMillis() - start));
			System.out.println("Duration per Check: " + (System.currentTimeMillis() - start) / (double) nChecks);
		}
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {
		try (NoTx noTx = db.noTx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create("extraUser", user());
			group().addUser(extraUser);
			role().grantPermissions(extraUser, READ_PERM);
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);
			MeshAuthUser requestUser = ac.getUser();
			Page<? extends User> userPage = group().getVisibleUsers(requestUser, new PagingParametersImpl(1, 10));

			assertEquals(2, userPage.getTotalElements());
		}
	}

	@Test
	@Override
	public void testFindByName() {
		try (NoTx noTx = db.noTx()) {
			assertNull(boot.userRoot().findByUsername("bogus"));
			boot.userRoot().findByUsername(user().getUsername());
		}
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			String uuid = user().getUuid();
			User foundUser = boot.userRoot().findByUuid(uuid);
			assertNotNull(foundUser);
			assertEquals(uuid, foundUser.getUuid());
		}
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		try (NoTx noTx = db.noTx()) {
			RoutingContext rc = getMockedRoutingContext(user());
			InternalActionContext ac = InternalActionContext.create(rc);

			UserResponse restUser = user().transformToRest(ac, 0).toBlocking().value();

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
		try (NoTx noTx = db.noTx()) {
			MeshRoot root = meshRoot();
			User user = root.getUserRoot().create("Anton", user());
			assertTrue(user.isEnabled());
			assertNotNull(user);
			String uuid = user.getUuid();
			SearchQueueBatch batch = createBatch();
			user.delete(batch);
			User foundUser = root.getUserRoot().findByUuid(uuid);
			assertNull(foundUser);
		}
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		try (NoTx noTx = db.noTx()) {
			MeshRoot root = meshRoot();
			User user = user();
			User newUser = root.getUserRoot().create("Anton", user());
			assertFalse(user.hasPermission(newUser, GraphPermission.CREATE_PERM));
			user.addCRUDPermissionOnRole(root.getUserRoot(), GraphPermission.CREATE_PERM, newUser);
			assertTrue(user.hasPermission(newUser, GraphPermission.CREATE_PERM));
		}
	}

	@Test
	public void testInheritPermissions() {
		try (NoTx noTx = db.noTx()) {
			Node sourceNode = folder("news");
			Node targetNode = folder("2015");
			User newUser;

			Role roleWithDeletePerm;
			Role roleWithReadPerm;
			Role roleWithUpdatePerm;
			Role roleWithAllPerm;
			Role roleWithNoPerm;
			Role roleWithCreatePerm;

			InternalActionContext ac = getMockedInternalActionContext(null, user());

			Group newGroup = meshRoot().getGroupRoot().create("extraGroup", user());
			newUser = meshRoot().getUserRoot().create("Anton", user());
			newGroup.addUser(newUser);

			// Create test roles
			roleWithDeletePerm = meshRoot().getRoleRoot().create("roleWithDeletePerm", newUser);
			newGroup.addRole(roleWithDeletePerm);
			roleWithDeletePerm.grantPermissions(sourceNode, DELETE_PERM);

			roleWithReadPerm = meshRoot().getRoleRoot().create("roleWithReadPerm", newUser);
			newGroup.addRole(roleWithReadPerm);
			roleWithReadPerm.grantPermissions(sourceNode, READ_PERM);

			roleWithUpdatePerm = meshRoot().getRoleRoot().create("roleWithUpdatePerm", newUser);
			newGroup.addRole(roleWithUpdatePerm);
			roleWithUpdatePerm.grantPermissions(sourceNode, UPDATE_PERM);

			roleWithAllPerm = meshRoot().getRoleRoot().create("roleWithAllPerm", newUser);
			newGroup.addRole(roleWithAllPerm);
			roleWithAllPerm.grantPermissions(sourceNode, CREATE_PERM, UPDATE_PERM, DELETE_PERM, READ_PERM, READ_PUBLISHED_PERM, PUBLISH_PERM);

			roleWithCreatePerm = meshRoot().getRoleRoot().create("roleWithCreatePerm", newUser);
			newGroup.addRole(roleWithCreatePerm);
			roleWithCreatePerm.grantPermissions(sourceNode, CREATE_PERM);

			roleWithNoPerm = meshRoot().getRoleRoot().create("roleWithNoPerm", newUser);
			newGroup.addRole(roleWithNoPerm);
			user().addCRUDPermissionOnRole(sourceNode, CREATE_PERM, targetNode);
			ac.data().clear();
			newUser.reload();
			for (GraphPermission perm : GraphPermission.values()) {
				assertTrue(
						"The new user should have all permissions to CRUD the target node since he is member of a group that has been assigned to roles with various permissions that cover CRUD. Failed for permission {"
								+ perm.name() + "}",
						newUser.hasPermission(targetNode, perm));
			}

			// roleWithAllPerm
			roleWithAllPerm.reload();
			for (GraphPermission perm : GraphPermission.values()) {
				assertTrue("The role should grant all permissions to the target node. Failed for permission {" + perm.name() + "}",
						roleWithAllPerm.hasPermission(perm, targetNode));
			}

			// roleWithNoPerm
			roleWithNoPerm.reload();
			for (GraphPermission perm : GraphPermission.values()) {
				assertFalse(
						"No extra permissions should be assigned to the role that did not have any permissions on the source element. Failed for permission {"
								+ perm.name() + "}",
						roleWithNoPerm.hasPermission(perm, targetNode));
			}

			// roleWithDeletePerm
			roleWithDeletePerm.reload();
			assertFalse("The role should only have delete permissions on the object", roleWithDeletePerm.hasPermission(CREATE_PERM, targetNode));
			assertFalse("The role should only have delete permissions on the object", roleWithDeletePerm.hasPermission(READ_PERM, targetNode));
			assertFalse("The role should only have delete permissions on the object", roleWithDeletePerm.hasPermission(UPDATE_PERM, targetNode));
			assertTrue("The role should only have delete permissions on the object", roleWithDeletePerm.hasPermission(DELETE_PERM, targetNode));

			// roleWithReadPerm
			roleWithReadPerm.reload();
			assertFalse("The role should only have read permissions on the object", roleWithReadPerm.hasPermission(CREATE_PERM, targetNode));
			assertTrue("The role should only have read permissions on the object", roleWithReadPerm.hasPermission(READ_PERM, targetNode));
			assertFalse("The role should only have read permissions on the object", roleWithReadPerm.hasPermission(UPDATE_PERM, targetNode));
			assertFalse("The role should only have read permissions on the object", roleWithReadPerm.hasPermission(DELETE_PERM, targetNode));

			// roleWithUpdatePerm
			roleWithUpdatePerm.reload();
			assertFalse("The role should only have update permissions on the object", roleWithUpdatePerm.hasPermission(CREATE_PERM, targetNode));
			assertFalse("The role should only have update permissions on the object", roleWithUpdatePerm.hasPermission(READ_PERM, targetNode));
			assertTrue("The role should only have update permissions on the object", roleWithUpdatePerm.hasPermission(UPDATE_PERM, targetNode));
			assertFalse("The role should only have update permissions on the object", roleWithUpdatePerm.hasPermission(DELETE_PERM, targetNode));

			// roleWithCreatePerm
			roleWithCreatePerm.reload();
			for (GraphPermission perm : GraphPermission.values()) {
				assertTrue(
						"The role should have all permission on the object since addCRUDPermissionOnRole has been invoked using CREATE_PERM parameter. Failed for permission {"
								+ perm.name() + "}",
						roleWithCreatePerm.hasPermission(perm, targetNode));
			}
		}

	}

	@Test
	@Override
	public void testRead() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			assertEquals("joe1", user.getUsername());
			assertNotNull(user.getPasswordHash());
			assertEquals("Joe", user.getFirstname());
			assertEquals("Doe", user.getLastname());
			assertEquals("j.doe@spam.gentics.com", user.getEmailAddress());

			assertNotNull(user.getLastEditedTimestamp());
			assertNotNull(user.getCreator());
			assertNotNull(user.getEditor());
			assertNotNull(user.getCreationTimestamp());
			assertEquals(1, user.getGroups().size());
			assertNotNull(user);
		}
	}

	@Test
	public void testUserGroup() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			assertEquals(1, user.getGroups().size());

			for (int i = 0; i < 10; i++) {
				Group extraGroup = meshRoot().getGroupRoot().create("group_" + i, user());
				// Multiple calls should not affect the result
				extraGroup.addUser(user);
				extraGroup.addUser(user);
				extraGroup.addUser(user);
				extraGroup.addUser(user);
			}

			assertEquals(11, user().getGroups().size());
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		try (NoTx noTx = db.noTx()) {
			final String USERNAME = "test";
			final String EMAIL = "joe@nowhere.org";
			final String FIRSTNAME = "joe";
			final String LASTNAME = "doe";
			final String PASSWDHASH = "RANDOM";

			UserRoot userRoot = meshRoot().getUserRoot();
			User user = userRoot.create(USERNAME, user());
			user.setEmailAddress(EMAIL);
			user.setFirstname(FIRSTNAME);
			user.setLastname(LASTNAME);
			user.setPasswordHash(PASSWDHASH);
			assertTrue(user.isEnabled());

			User reloadedUser = userRoot.findByUuid(user.getUuid());
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
		try (NoTx noTx = db.noTx()) {
			User user = user();
			String uuid = user.getUuid();
			assertEquals(1, user.getGroups().size());
			assertTrue(user.isEnabled());
			SearchQueueBatch batch = createBatch();
			user.delete(batch);
			User foundUser = meshRoot().getUserRoot().findByUuid(uuid);
			assertNull(foundUser);
		}
	}

	@Test
	public void testOwnRolePerm() {
		try (NoTx noTx = db.noTx()) {
			assertTrue("The user should have update permissions on his role", user().hasPermission(role(), GraphPermission.UPDATE_PERM));
		}
	}

	@Test
	@Override
	public void testUpdate() {
		try (NoTx noTx = db.noTx()) {
			User newUser = meshRoot().getUserRoot().create("newUser", user());

			User user = user();

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
			assertEquals(newUser, user.getCreator());

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
		try (NoTx noTx = db.noTx()) {
			User user = meshRoot().getUserRoot().create("Anton", user());
			testPermission(GraphPermission.READ_PERM, user);
		}
	}

	@Test
	@Override
	public void testDeletePermission() {
		try (NoTx noTx = db.noTx()) {
			User user = meshRoot().getUserRoot().create("Anton", user());
			testPermission(GraphPermission.DELETE_PERM, user);
		}
	}

	@Test
	@Override
	public void testUpdatePermission() {
		try (NoTx noTx = db.noTx()) {
			User user = meshRoot().getUserRoot().create("Anton", user());
			testPermission(GraphPermission.UPDATE_PERM, user);
		}
	}

	@Test
	@Override
	public void testCreatePermission() {
		try (NoTx noTx = db.noTx()) {
			User user = meshRoot().getUserRoot().create("Anton", user());
			testPermission(GraphPermission.CREATE_PERM, user);
		}
	}
}
