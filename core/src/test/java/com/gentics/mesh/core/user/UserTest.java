package com.gentics.mesh.core.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.Page;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Language;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.MeshRoot;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.test.AbstractBasicObjectTest;
import com.gentics.mesh.util.InvalidArgumentException;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public class UserTest extends AbstractBasicObjectTest {

	@Test
	public void testCreatedUser() {
		assertNotNull("The uuid of the user should not be null since the entity was reloaded.", user().getUuid());
	}

	@Test
	@Override
	public void testRootNode() {
		UserRoot root = meshRoot().getUserRoot();
		int nUserBefore = root.findAll().size();
		assertNotNull(root.create("dummy12345", null, user()));
		int nUserAfter = root.findAll().size();
		assertEquals("The root node should now list one more user", nUserBefore + 1, nUserAfter);
	}

	@Test
	public void testHasPermission() {
		InternalActionContext ac = getMockedInternalActionContext("");
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100000; i++) {
			assertTrue(user().hasPermission(ac, english(), READ_PERM));
			ac.data().clear();
		}
		long duration = System.currentTimeMillis() - start;
		System.out.println("Duration: " + duration);
	}

	@Test
	@Override
	public void testFindAll() throws InvalidArgumentException {
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();

		Page<? extends User> page = boot.userRoot().findAll(requestUser, new PagingInfo(1, 6));
		assertEquals(users().size(), page.getTotalElements());
		assertEquals(3, page.getSize());

		page = boot.userRoot().findAll(requestUser, new PagingInfo(1, 15));
		assertEquals(users().size(), page.getTotalElements());
		assertEquals(users().size(), page.getSize());
	}

	@Test
	@Override
	public void testFindAllVisible() throws InvalidArgumentException {
		Page<? extends User> page = boot.userRoot().findAll(getRequestUser(), new PagingInfo(1, 25));
		assertNotNull(page);
		assertEquals(users().size(), page.getTotalElements());
	}

	@Test
	public void testGetPrincipal() {
		RoutingContext rc = getMockedRoutingContext("");
		io.vertx.ext.auth.User user = rc.user();
		assertNotNull(user);
		JsonObject json = user.principal();
		assertNotNull(json);
		try (Trx tx = db.trx()) {
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

	@Test
	public void testGetPermissionNamesViaHandler() throws InterruptedException, ExecutionException, TimeoutException {
		User user = user();
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		Node node = content();

		int max = 20000;
		CountDownLatch latch = new CountDownLatch(max);
		long now = System.currentTimeMillis();
		for (int i = 0; i < max; i++) {

			//ac.data().clear();
			//CompletableFuture<String[]> permissionFuture = new CompletableFuture<>();

			if (1 != 1) {
				user.getPermissionNames(ac, node);
				latch.countDown();
			} else {
				user.getPermissionNames(ac, node, rh -> {
					//String[] names = rh.result().toArray(new String[rh.result().size()]);
					//permissionFuture.complete(names);
					latch.countDown();
				});
			}
			//assertNotNull(permissionFuture.get(5, TimeUnit.SECONDS));
		}

		latch.await();
		long dur = System.currentTimeMillis() - now;
		System.out.println("Duration:" + dur);
		//		for (String name : permissionFuture.get()) {
		//			System.out.println(name);
		//		}
	}

	@Test
	public void testGetPermissions() {
		Language language = english();
		String[] perms = { "create", "update", "delete", "read" };
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		long start = System.currentTimeMillis();
		int nChecks = 10000;
		for (int i = 0; i < nChecks; i++) {
			String[] loadedPerms = user().getPermissionNames(ac, language);
			Arrays.sort(perms);
			Arrays.sort(loadedPerms);
			assertArrayEquals("Permissions do not match", perms, loadedPerms);
			assertNotNull(ac.data().get("permissions:" + language.getUuid()));
		}
		System.out.println("Duration: " + (System.currentTimeMillis() - start));
		System.out.println("Duration per Check: " + (System.currentTimeMillis() - start) / (double) nChecks);
	}

	@Test
	public void testFindUsersOfGroup() throws InvalidArgumentException {
		UserRoot userRoot = meshRoot().getUserRoot();
		User extraUser = userRoot.create("extraUser", group(), user());
		role().grantPermissions(extraUser, READ_PERM);
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		MeshAuthUser requestUser = ac.getUser();
		Page<? extends User> userPage = group().getVisibleUsers(requestUser, new PagingInfo(1, 10));

		assertEquals(2, userPage.getTotalElements());
	}

	@Test
	@Override
	public void testFindByName() {
		assertNull(boot.userRoot().findByUsername("bogus"));
		boot.userRoot().findByUsername(user().getUsername());
	}

	@Test
	@Override
	public void testFindByUUID() throws Exception {
		String uuid = user().getUuid();
		CountDownLatch latch = new CountDownLatch(1);
		boot.userRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			assertEquals(uuid, rh.result().getUuid());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testTransformation() throws Exception {
		CountDownLatch latch = new CountDownLatch(1);
		RoutingContext rc = getMockedRoutingContext("");
		InternalActionContext ac = InternalActionContext.create(rc);
		CompletableFuture<UserResponse> cf = new CompletableFuture<>();
		user().transformToRest(ac, rh -> {
			cf.complete(rh.result());

			latch.countDown();
		});
		failingLatch(latch);
		UserResponse restUser = cf.get();
		assertNotNull(restUser);
		assertEquals(user().getUsername(), restUser.getUsername());
		assertEquals(user().getUuid(), restUser.getUuid());
		assertEquals(user().getLastname(), restUser.getLastname());
		assertEquals(user().getFirstname(), restUser.getFirstname());
		assertEquals(user().getEmailAddress(), restUser.getEmailAddress());
		assertEquals(1, restUser.getGroups().size());
	}

	@Test
	@Override
	public void testCreateDelete() throws Exception {
		MeshRoot root = meshRoot();
		User user = root.getUserRoot().create("Anton", null, user());
		assertTrue(user.isEnabled());
		assertNotNull(user);
		String uuid = user.getUuid();
		user.delete();
		CountDownLatch latch = new CountDownLatch(1);
		root.getUserRoot().findByUuid(uuid, rh -> {
			User foundUser = rh.result();
			assertNotNull(foundUser);
			assertFalse(foundUser.isEnabled());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testCRUDPermissions() {
		MeshRoot root = meshRoot();
		User user = user();
		InternalActionContext ac = getMockedInternalActionContext("");
		User newUser = root.getUserRoot().create("Anton", null, user());
		assertFalse(user.hasPermission(ac, newUser, GraphPermission.CREATE_PERM));
		user.addCRUDPermissionOnRole(root.getUserRoot(), GraphPermission.CREATE_PERM, newUser);
		ac.data().clear();
		assertTrue(user.hasPermission(ac, newUser, GraphPermission.CREATE_PERM));
	}

	@Test
	public void testInheritPermissions() {
		Node sourceNode = folder("news");
		Node targetNode = folder("2015");
		User newUser;

		Role roleWithDeletePerm;
		Role roleWithReadPerm;
		Role roleWithUpdatePerm;
		Role roleWithAllPerm;
		Role roleWithNoPerm;
		Role roleWithCreatePerm;

		InternalActionContext ac = getMockedInternalActionContext("");

		Group newGroup = meshRoot().getGroupRoot().create("extraGroup", user());
		newUser = meshRoot().getUserRoot().create("Anton", newGroup, user());

		// Create test roles
		roleWithDeletePerm = meshRoot().getRoleRoot().create("roleWithDeletePerm", newGroup, newUser);
		roleWithDeletePerm.grantPermissions(sourceNode, DELETE_PERM);

		roleWithReadPerm = meshRoot().getRoleRoot().create("roleWithReadPerm", newGroup, newUser);
		roleWithReadPerm.grantPermissions(sourceNode, READ_PERM);

		roleWithUpdatePerm = meshRoot().getRoleRoot().create("roleWithUpdatePerm", newGroup, newUser);
		roleWithUpdatePerm.grantPermissions(sourceNode, UPDATE_PERM);

		roleWithAllPerm = meshRoot().getRoleRoot().create("roleWithAllPerm", newGroup, newUser);
		roleWithAllPerm.grantPermissions(sourceNode, CREATE_PERM, UPDATE_PERM, DELETE_PERM, READ_PERM);

		roleWithCreatePerm = meshRoot().getRoleRoot().create("roleWithCreatePerm", newGroup, newUser);
		roleWithCreatePerm.grantPermissions(sourceNode, CREATE_PERM);

		roleWithNoPerm = meshRoot().getRoleRoot().create("roleWithNoPerm", newGroup, newUser);
		user().addCRUDPermissionOnRole(sourceNode, CREATE_PERM, targetNode);
		ac.data().clear();
		newUser.reload();
		for (GraphPermission perm : GraphPermission.values()) {
			assertTrue(
					"The new user should have all permissions to CRUD the target node since he is member of a group that has been assigned to roles with various permissions that cover CRUD. Failed for permission {"
							+ perm.name() + "}",
					newUser.hasPermission(ac, targetNode, perm));
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

	@Test
	@Override
	public void testRead() {
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
		assertNotNull(user.getImpl());
	}

	@Test
	public void testUserGroup() {
		User user = user();
		assertEquals(1, user.getGroups().size());

		for (int i = 0; i < 10; i++) {
			Group extraGroup = meshRoot().getGroupRoot().create("group_" + i, user());
			// Multiple calls should not affect the result
			user().addGroup(extraGroup);
			user().addGroup(extraGroup);
			user().addGroup(extraGroup);
			user().addGroup(extraGroup);
		}

		assertEquals(11, user().getGroups().size());
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		final String USERNAME = "test";
		final String EMAIL = "joe@nowhere.org";
		final String FIRSTNAME = "joe";
		final String LASTNAME = "doe";
		final String PASSWDHASH = "RANDOM";

		UserRoot userRoot = meshRoot().getUserRoot();
		User user = userRoot.create(USERNAME, null, user());
		user.setEmailAddress(EMAIL);
		user.setFirstname(FIRSTNAME);
		user.setLastname(LASTNAME);
		user.setPasswordHash(PASSWDHASH);
		assertTrue(user.isEnabled());

		CountDownLatch latch = new CountDownLatch(1);
		userRoot.findByUuid(user.getUuid(), rh -> {
			User reloadedUser = rh.result();
			assertEquals("The username did not match.", USERNAME, reloadedUser.getUsername());
			assertEquals("The lastname did not match.", LASTNAME, reloadedUser.getLastname());
			assertEquals("The firstname did not match.", FIRSTNAME, reloadedUser.getFirstname());
			assertEquals("The email address did not match.", EMAIL, reloadedUser.getEmailAddress());
			assertEquals("The password did not match.", PASSWDHASH, reloadedUser.getPasswordHash());
			latch.countDown();
		});
		failingLatch(latch);
	}

	@Test
	@Override
	public void testDelete() {
		try (Trx tx = db.trx()) {
			User user = user();
			assertEquals(1, user.getGroups().size());
			assertTrue(user.isEnabled());
			user.delete();
			assertFalse(user.isEnabled());
			assertEquals(0, user.getGroups().size());
			tx.success();
		}
	}

	@Test
	@Override
	public void testUpdate() {
		User newUser = meshRoot().getUserRoot().create("newUser", null, user());

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

	@Test
	@Override
	public void testReadPermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(GraphPermission.READ_PERM, user);
	}

	@Test
	@Override
	public void testDeletePermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(GraphPermission.DELETE_PERM, user);
	}

	@Test
	@Override
	public void testUpdatePermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(GraphPermission.UPDATE_PERM, user);
	}

	@Test
	@Override
	public void testCreatePermission() {
		User user = meshRoot().getUserRoot().create("Anton", null, user());
		testPermission(GraphPermission.CREATE_PERM, user);
	}
}
