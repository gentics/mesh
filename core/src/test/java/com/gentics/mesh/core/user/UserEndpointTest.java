package com.gentics.mesh.core.user;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.PUBLISH;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.READ_PUBLISHED;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserTokenResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.graphdb.Tx;
import com.gentics.mesh.parameter.impl.NodeParameters;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParameters;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.AbstractBasicCrudEndpointTest;

import io.vertx.core.http.HttpHeaders;

public class UserEndpointTest extends AbstractBasicCrudEndpointTest {

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		String uuid = db.noTx(() -> user().getUuid());
		UserResponse restUser = call(() -> client().findUserByUuid(uuid));
		try (NoTx noTx = db.noTx()) {
			assertThat(restUser).matches(user());
		}
		// TODO assert groups
		// TODO assert perms
	}

	@Test
	public void testUpdateUsingExpiredToken() {
		String uuid = db.noTx(() -> user().getUuid());
		String oldHash = db.noTx(() -> user().getPasswordHash());
		assertNull("Initially the token code should be null", db.noTx(() -> user().getResetToken()));
		assertNull("Initially the token issue timestamp should be null", db.noTx(() -> user().getResetTokenIssueTimestamp()));

		// 1. Get new token
		UserTokenResponse response = call(() -> client().getUserToken(uuid));
		assertNotNull("The user token code should now be set to a non-null value but it was not.", db.noTx(() -> user().getResetToken()));
		assertNotNull("The token code issue timestamp should be set.", db.noTx(() -> user().getResetTokenIssueTimestamp()));

		// 2. Fake an old issue timestamp
		db.noTx(() -> user().setResetTokenIssueTimestamp(System.currentTimeMillis() - 1000 * 60 * 60));

		// 2. Logout the current client user
		client().logout().toBlocking().value();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl(response.getToken())), UNAUTHORIZED,
				"user_error_provided_token_invalid");

		// 4. Assert that the password was not updated
		String newHash = db.noTx(() -> user().getPasswordHash());
		assertEquals("The password hash has not been updated.", oldHash, newHash);
		assertNull("The token code should have been set to null since it has expired.", db.noTx(() -> user().getResetToken()));
		assertNull("The token issue timestamp should have been set to null since it has expired",
				db.noTx(() -> user().getResetTokenIssueTimestamp()));

	}

	@Test
	public void testUpdateUsingToken() {
		String uuid = db.noTx(() -> user().getUuid());
		String oldHash = db.noTx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", db.noTx(() -> user().getResetToken()));

		// 1. Get new token
		UserTokenResponse response = call(() -> client().getUserToken(uuid));
		assertNotNull("The user token code should now be set to a non-null value but it was not", db.noTx(() -> user().getResetToken()));

		// 2. Logout the current client user
		client().logout().toBlocking().value();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl(response.getToken())));

		// 4. Assert that the password was updated
		String newHash = db.noTx(() -> user().getPasswordHash());
		assertNull("The token code should have been set to null since it is now used up", db.noTx(() -> user().getResetToken()));

		assertNotEquals("The password hash has not been updated.", oldHash, newHash);
	}

	@Test
	public void testUpdateUsingBogusToken() {
		String uuid = db.noTx(() -> user().getUuid());
		String oldHash = db.noTx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", db.noTx(() -> user().getResetToken()));

		// 1. Get new token
		call(() -> client().getUserToken(uuid));

		// 2. Logout the current client user
		client().logout().toBlocking().value();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl("bogusToken")), UNAUTHORIZED, "user_error_provided_token_invalid");

		// 4. Assert that the password was not updated
		String newHash = db.noTx(() -> user().getPasswordHash());
		assertEquals("The password hash should not have been updated.", oldHash, newHash);

	}

	@Test
	public void testUpdateWithNoToken() {
		String uuid = db.noTx(() -> user().getUuid());
		String oldHash = db.noTx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", db.noTx(() -> user().getResetToken()));

		// 1. Get new token
		call(() -> client().getUserToken(uuid));

		// 2. Logout the current client user
		client().logout().toBlocking().value();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request), UNAUTHORIZED, "error_not_authorized");

		// 4. Assert that the password was not updated
		String newHash = db.noTx(() -> user().getPasswordHash());
		assertEquals("The password hash should not have been updated.", oldHash, newHash);
	}

	@Test
	public void testFetchUserToken() {
		String uuid = db.noTx(() -> user().getUuid());

		UserTokenResponse response = call(() -> client().getUserToken(uuid));
		assertThat(response.getToken()).isNotEmpty();
		String storedToken = db.noTx(() -> user().getResetToken());
		assertEquals("The token that is currently stored did not match up with the returned token by the api", storedToken, response.getToken());
	}

	@Test
	public void testReadPermissions() {
		TagFamily tagFamily;
		User user;
		String pathToElement;
		try (NoTx noTx = db.noTx()) {
			user = user();
			tagFamily = tagFamily("colors");

			// Add permission on own role
			role().grantPermissions(tagFamily, GraphPermission.UPDATE_PERM);
			assertTrue(role().hasPermission(GraphPermission.UPDATE_PERM, tagFamily));
			pathToElement = "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily.getUuid();
		}

		try (NoTx noTx = db.noTx()) {
			UserPermissionResponse response = call(() -> client().readUserPermissions(user.getUuid(), pathToElement));
			assertNotNull(response);
			assertThat(response).hasPerm(Permission.values());
		}

		try (NoTx noTx = db.noTx()) {
			// Revoke single permission and check again
			role().revokePermissions(tagFamily, GraphPermission.UPDATE_PERM);
			assertFalse(role().hasPermission(GraphPermission.UPDATE_PERM, tagFamily));
		}
		try (NoTx noTx = db.noTx()) {
			UserPermissionResponse response  = call(() -> client().readUserPermissions(user.getUuid(), pathToElement));
			assertNotNull(response);
			assertThat(response).hasPerm(Permission.values());
		}
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			String uuid = user.getUuid();

			UserResponse userResponse = call(() -> client().findUserByUuid(uuid, new RolePermissionParameters().setRoleUuid(role().getUuid())));
			assertNotNull(userResponse.getRolePerms());
			assertThat(userResponse.getRolePerms()).hasPerm(READ, READ_PUBLISHED, PUBLISH, CREATE, UPDATE, DELETE);
		}
	}

	@Test
	public void testReadUserWithMultipleGroups() {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			assertEquals(1, user.getGroups().size());

			for (int i = 0; i < 10; i++) {
				Group extraGroup = meshRoot().getGroupRoot().create("group_" + i, user());
				extraGroup.addUser(user());
			}

			assertEquals(11, user().getGroups().size());
			UserResponse response = call(() -> client().findUserByUuid(user().getUuid()));
			assertEquals(11, response.getGroups().size());
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws InterruptedException {
		try (NoTx noTx = db.noTx()) {
			int nJobs = 10;
			String uuid = user().getUuid();
			// CyclicBarrier barrier = prepareBarrier(nJobs);
			Set<MeshResponse<?>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findUserByUuid(uuid).invoke());
			}
			validateSet(set, null);
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		try (NoTx noTx = db.noTx()) {
			int nJobs = 200;
			Set<MeshResponse<UserResponse>> set = new HashSet<>();
			for (int i = 0; i < nJobs; i++) {
				set.add(client().findUserByUuid(user().getUuid()).invoke());
			}
			for (MeshResponse<UserResponse> future : set) {
				latchFor(future);
				assertSuccess(future);
			}
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			String uuid = user.getUuid();
			assertNotNull("The username of the user must not be null.", user.getUsername());
			role().revokePermissions(user, READ_PERM);

			call(() -> client().findUserByUuid(uuid), FORBIDDEN, "error_missing_perm", uuid);
		}
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserRoot root = meshRoot().getUserRoot();

			int nUsers = 20;
			for (int i = 0; i < nUsers; i++) {
				String username = "testuser_" + i;
				User user = root.create(username, user());
				group().addUser(user);
				user.setLastname("should_be_listed");
				user.setFirstname("should_be_listed");
				user.setEmailAddress("should_be_listed");
				role().grantPermissions(user, READ_PERM);
			}

			User invisibleUser = root.create("should_not_be_listed", user());
			invisibleUser.setLastname("should_not_be_listed");
			invisibleUser.setFirstname("should_not_be_listed");
			invisibleUser.setEmailAddress("should_not_be_listed");
			invisibleUser.addGroup(group());

			assertEquals("We did not find the expected count of users attached to the user root vertex.", 3 + nUsers + 1, root.findAll().size());

			// Test default paging parameters
			MeshResponse<UserListResponse> future = client().findUsers().invoke();
			latchFor(future);
			assertSuccess(future);
			ListResponse<UserResponse> restResponse = future.result();
			assertEquals(25, restResponse.getMetainfo().getPerPage());
			assertEquals(1, restResponse.getMetainfo().getCurrentPage());
			// Admin User + Guest User + Dummy User = 3
			assertEquals(3 + nUsers, restResponse.getMetainfo().getTotalCount());
			assertEquals(3 + nUsers, restResponse.getData().size());

			int perPage = 2;
			int totalUsers = 3 + nUsers;
			int totalPages = ((int) Math.ceil(totalUsers / (double) perPage));
			future = client().findUsers(new PagingParametersImpl(3, perPage)).invoke();
			latchFor(future);
			assertSuccess(future);
			restResponse = future.result();

			assertEquals("The page did not contain the expected amount of items", perPage, restResponse.getData().size());
			assertEquals("We did not find the expected page in the list response.", 3, restResponse.getMetainfo().getCurrentPage());
			assertEquals("The amount of pages did not match. We have {" + totalUsers + "} users in the system and use a paging of {" + perPage + "}",
					totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals("The total amount of items does not match the expected one", totalUsers, restResponse.getMetainfo().getTotalCount());

			perPage = 11;

			List<UserResponse> allUsers = new ArrayList<>();
			for (int page = 1; page < totalPages; page++) {
				MeshResponse<UserListResponse> pageFuture = client().findUsers(new PagingParametersImpl(page, perPage)).invoke();
				latchFor(pageFuture);
				assertSuccess(pageFuture);
				restResponse = pageFuture.result();
				allUsers.addAll(restResponse.getData());
			}
			assertEquals("Somehow not all users were loaded when loading all pages.", totalUsers, allUsers.size());

			// Verify that the invisible is not part of the response
			final String extra3Username = "should_not_be_listed";
			List<UserResponse> filteredUserList = allUsers.parallelStream().filter(restUser -> restUser.getUsername().equals(extra3Username))
					.collect(Collectors.toList());
			assertTrue("User 3 should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

			future = client().findUsers(new PagingParametersImpl(1, -1)).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "error_pagesize_parameter", "-1");

			future = client().findUsers(new PagingParametersImpl(4242, 25)).invoke();
			latchFor(future);
			assertSuccess(future);

			assertEquals("The result list should not contain any item since the page parameter is out of bounds", 0,
					future.result().getData().size());
			assertEquals("The requested page should be set in the response but it was not", 4242, future.result().getMetainfo().getCurrentPage());
			assertEquals("The page count value was not correct.", 1, future.result().getMetainfo().getPageCount());
			assertEquals("We did not find the correct total count value in the response", nUsers + 3, future.result().getMetainfo().getTotalCount());
			assertEquals(25, future.result().getMetainfo().getPerPage());
		}
	}

	@Test
	public void testInvalidPageParameter() {
		UserListResponse list = call(() -> client().findUsers(new PagingParametersImpl(1, 0)));
		assertEquals(0, list.getData().size());
		assertTrue(list.getMetainfo().getTotalCount() > 0);
	}

	@Test
	public void testInvalidPageParameter2() {
		call(() -> client().findUsers(new PagingParametersImpl(-1, 25)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");
	}

	@Test
	@Override
	public void testUpdateMultithreaded() throws InterruptedException {
		String uuid = db.noTx(() -> user().getUuid());
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");

		int nJobs = 50;
		Set<MeshResponse<UserResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			updateRequest.setUsername("dummy_user_changed" + i);
			set.add(client().updateUser(uuid, updateRequest).invoke());
		}

		for (MeshResponse<UserResponse> response : set) {
			latchFor(response);
			assertSuccess(response);
		}

	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		String oldName = db.tx(() -> user().getUsername());
		String uuid = db.noTx(() -> user().getUuid());

		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");
		updateRequest.setUsername("dummy_user_changed");
		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));

		assertThat(dummySearchProvider).hasStore(User.composeIndexName(), User.composeIndexType(), uuid);
		assertThat(dummySearchProvider).hasEvents(1, 0, 0, 0);
		dummySearchProvider.clear();

		try (NoTx noTx = db.noTx()) {
			assertThat(restUser).matches(updateRequest);
			assertNull("The user node should have been updated and thus no user should be found.", boot.userRoot().findByUsername(oldName));
			User reloadedUser = boot.userRoot().findByUsername("dummy_user_changed");
			assertNotNull(reloadedUser);
			assertEquals("Epic Stark", reloadedUser.getLastname());
			assertEquals("Tony Awesome", reloadedUser.getFirstname());
			assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
			assertEquals("dummy_user_changed", reloadedUser.getUsername());
		}
	}

	@Test
	public void testUpdateWithSpecialCharacters() throws Exception {
		String uuid = db.noTx(() -> user().getUuid());
		String oldUsername = db.noTx(() -> user().getUsername());

		final char c = '\u2665';
		String email = "t.stark@stärk-industries.com" + c;
		String firstname = "Töny Awesöme" + c;
		String lastname = "Epic Stärk" + c;
		String username = "dummy_usär_chänged" + c;
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress(email);
		updateRequest.setFirstname(firstname);
		updateRequest.setLastname(lastname);
		updateRequest.setUsername(username);

		MeshResponse<UserResponse> future = client().updateUser(uuid, updateRequest).invoke();
		latchFor(future);
		assertSuccess(future);
		UserResponse restUser = future.result();
		assertThat(restUser).matches(updateRequest);
		try (Tx tx = db.tx()) {
			assertNull("The user node should have been updated and thus no user should be found.", boot.userRoot().findByUsername(oldUsername));
			User reloadedUser = boot.userRoot().findByUsername(username);
			assertNotNull(reloadedUser);
			assertEquals(lastname, reloadedUser.getLastname());
			assertEquals(firstname, reloadedUser.getFirstname());
			assertEquals(email, reloadedUser.getEmailAddress());
			assertEquals(username, reloadedUser.getUsername());
		}

	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws GenericRestException, Exception {
		UserUpdateRequest request = new UserUpdateRequest();
		request.setUsername("New Name");

		call(() -> client().updateUser("bogus", request), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testUpdateUserAndSetNodeReference() throws Exception {
		try (NoTx noTx = db.noTx()) {
			String nodeUuid = folder("2015").getUuid();
			User user = user();
			String username = user.getUsername();
			UserUpdateRequest updateRequest = new UserUpdateRequest();
			updateRequest.setEmailAddress("t.stark@stark-industries.com");
			updateRequest.setFirstname("Tony Awesome");
			updateRequest.setLastname("Epic Stark");
			updateRequest.setUsername("dummy_user_changed");

			NodeReference userNodeReference = new NodeReference();
			userNodeReference.setProjectName(PROJECT_NAME);
			userNodeReference.setUuid(nodeUuid);
			updateRequest.setNodeReference(userNodeReference);

			MeshResponse<UserResponse> future = client().updateUser(user.getUuid(), updateRequest).invoke();
			latchFor(future);
			assertSuccess(future);
			UserResponse restUser = future.result();
			user().reload();
			assertNotNull(user().getReferencedNode());
			assertNotNull(restUser.getNodeReference());
			assertEquals(PROJECT_NAME, ((NodeReference) restUser.getNodeReference()).getProjectName());
			assertEquals(nodeUuid, restUser.getNodeReference().getUuid());
			assertThat(restUser).matches(updateRequest);
			assertNull("The user node should have been updated and thus no user should be found.", boot.userRoot().findByUsername(username));
			User reloadedUser = boot.userRoot().findByUsername("dummy_user_changed");
			assertNotNull(reloadedUser);
			assertEquals("Epic Stark", reloadedUser.getLastname());
			assertEquals("Tony Awesome", reloadedUser.getFirstname());
			assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
			assertEquals("dummy_user_changed", reloadedUser.getUsername());
			assertEquals(nodeUuid, reloadedUser.getReferencedNode().getUuid());
		}
	}

	@Test
	public void testCreateUserWithNodeReference() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			assertTrue(user().hasPermission(node, READ_PERM));

			NodeReference reference = new NodeReference();
			reference.setProjectName(PROJECT_NAME);
			reference.setUuid(node.getUuid());

			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);

			UserResponse response = call(() -> client().createUser(newUser));
			assertTrue(response.isReference());
			assertNotNull(response.getNodeReference());
			assertNotNull(response.getReferencedNodeReference().getProjectName());
			assertNotNull(response.getNodeReference().getUuid());
		}
	}

	@Test
	public void testReadUserListWithExpandedNodeReference() {
		UserResponse userCreateResponse = db.noTx(() -> {
			Node node = folder("2015");

			NodeReference reference = new NodeReference();
			reference.setUuid(node.getUuid());
			reference.setProjectName(PROJECT_NAME);

			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);
			MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
			latchFor(future);
			assertSuccess(future);
			return future.result();
		});

		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			UserListResponse userResponse = call(() -> client().findUsers(new PagingParametersImpl().setPerPage(100),
					new NodeParameters().setExpandedFieldNames("nodeReference").setLanguages("en")));
			assertNotNull(userResponse);

			UserResponse foundUser = userResponse.getData().stream().filter(u -> u.getUuid().equals(userCreateResponse.getUuid())).findFirst().get();
			assertNotNull(foundUser.getNodeReference());
			assertEquals(node.getUuid(), foundUser.getNodeReference().getUuid());
			assertEquals(NodeResponse.class, foundUser.getNodeReference().getClass());
		}
	}

	@Test
	// test fails since user node references are not yet release aware
	public void testReadUserWithExpandedNodeReference() {
		String folderUuid;
		UserCreateRequest newUser;
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");
			folderUuid = node.getUuid();

			NodeReference reference = new NodeReference();
			reference.setUuid(node.getUuid());
			reference.setProjectName(PROJECT_NAME);

			UserCreateRequest request = new UserCreateRequest();
			request.setUsername("new_user");
			request.setGroupUuid(group().getUuid());
			request.setPassword("test1234");
			request.setNodeReference(reference);
			newUser = request;
		}

		MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
		latchFor(future);
		assertSuccess(future);
		UserResponse userResponse = future.result();

		MeshResponse<UserResponse> userResponseFuture = client()
				.findUserByUuid(userResponse.getUuid(), new NodeParameters().setExpandedFieldNames("nodeReference").setLanguages("en")).invoke();
		latchFor(userResponseFuture);
		assertSuccess(userResponseFuture);
		UserResponse userResponse2 = userResponseFuture.result();
		assertNotNull(userResponse2);
		assertNotNull(userResponse2.getNodeReference());
		assertEquals(folderUuid, userResponse2.getNodeReference().getUuid());
		assertEquals(NodeResponse.class, userResponse2.getNodeReference().getClass());

	}

	@Test
	public void testCreateUserWithBogusProjectNameInNodeReference() {
		try (NoTx noTx = db.noTx()) {
			Node node = folder("2015");

			NodeReference reference = new NodeReference();
			reference.setProjectName("bogus_name");
			reference.setUuid(node.getUuid());

			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);

			call(() -> client().createUser(newUser), BAD_REQUEST, "project_not_found", "bogus_name");
		}
	}

	@Test
	public void testCreateUserWithBogusUuidInNodeReference() {
		try (NoTx noTx = db.noTx()) {
			NodeReference reference = new NodeReference();
			reference.setProjectName(PROJECT_NAME);
			reference.setUuid("bogus_uuid");

			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);

			call(() -> client().createUser(newUser), NOT_FOUND, "object_not_found_for_uuid", "bogus_uuid");
		}
	}

	@Test
	public void testCreateUserWithMissingProjectNameInNodeReference() {
		try (NoTx noTx = db.noTx()) {
			NodeReference reference = new NodeReference();
			reference.setUuid("bogus_uuid");

			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);

			call(() -> client().createUser(newUser), BAD_REQUEST, "user_creation_full_node_reference_not_implemented");
		}
	}

	@Test
	public void testCreateUserWithMissingUuidNameInNodeReference() {

		try (NoTx noTx = db.noTx()) {
			NodeReference reference = new NodeReference();
			reference.setProjectName(PROJECT_NAME);
			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);

			call(() -> client().createUser(newUser), BAD_REQUEST, "user_incomplete_node_reference");
		}
	}

	// @Test
	// public void testUpdateExistingPasswordWithNoOldPassword() {
	// String uuid;
	// String oldHash;
	//
	// try (NoTx noTx = db.noTx()) {
	// uuid = user().getUuid();
	// oldHash = user().getPasswordHash();
	// }
	//
	// UserUpdateRequest updateRequest = new UserUpdateRequest();
	// updateRequest.setPassword("new_password");
	// // Old password not set
	//
	// call(() -> getClient().updateUser(uuid, updateRequest), BAD_REQUEST,
	// "user_error_missing_old_password");
	//
	// try (NoTx noTx = db.noTx()) {
	// User reloadedUser = boot.userRoot().findByUuid(uuid);
	// assertEquals("The hash should not be different since the password should
	// not have been updated.", oldHash,
	// reloadedUser.getPasswordHash());
	// }
	// }

	// @Test
	// public void testUpdateExistingPasswordWithWrongOldPassword() {
	// String uuid;
	// String oldHash;
	//
	// try (NoTx noTx = db.noTx()) {
	// uuid = user().getUuid();
	// oldHash = user().getPasswordHash();
	// }
	//
	// UserUpdateRequest updateRequest = new UserUpdateRequest();
	// updateRequest.setPassword("new_password");
	// updateRequest.setOldPassword("bogus");
	//
	// call(() -> getClient().updateUser(uuid, updateRequest), BAD_REQUEST,
	// "user_error_password_check_failed");
	//
	// try (NoTx noTx = db.noTx()) {
	// User reloadedUser = boot.userRoot().findByUuid(uuid);
	// assertEquals("The hash should not be different since the password should
	// not have been updated.", oldHash,
	// reloadedUser.getPasswordHash());
	// }
	//
	// }

	@Test
	public void testUpdatePasswordWithNoOldPassword() {
		String uuid;
		String oldHash;

		try (NoTx noTx = db.noTx()) {
			user().setPasswordHash(null);
			uuid = user().getUuid();
			oldHash = user().getPasswordHash();
		}

		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setPassword("new_password");

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));
		assertThat(restUser).matches(updateRequest);

		try (NoTx noTx = db.noTx()) {
			User reloadedUser = boot.userRoot().findByUuid(uuid);
			assertNotEquals("The hash should be different and thus the password updated.", oldHash, reloadedUser.getPasswordHash());
		}

	}

	@Test
	public void testUpdatePassword() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		String username;
		String uuid;
		String oldHash;
		try (NoTx noTx = db.noTx()) {
			User user = user();
			username = user.getUsername();
			uuid = user.getUuid();
			oldHash = user.getPasswordHash();
		}
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setPassword("new_password");
		updateRequest.setOldPassword("test123");

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));
		assertThat(restUser).matches(updateRequest);

		try (NoTx noTx = db.noTx()) {
			User reloadedUser = boot.userRoot().findByUsername(username);
			assertNotEquals("The hash should be different and thus the password updated.", oldHash, reloadedUser.getPasswordHash());
		}
	}

	@Test
	public void testUpdatePasswordWithNoPermission() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			String oldHash = user.getPasswordHash();
			role().revokePermissions(user, UPDATE_PERM);

			UserUpdateRequest request = new UserUpdateRequest();
			request.setPassword("new_password");
			call(() -> client().updateUser(user.getUuid(), request), FORBIDDEN, "error_missing_perm", user.getUuid());

			User reloadedUser = boot.userRoot().findByUuid(user.getUuid());
			assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		try (NoTx noTx = db.noTx()) {
			User user = user();
			String oldHash = user.getPasswordHash();
			role().revokePermissions(user, UPDATE_PERM);
			UserUpdateRequest updatedUser = new UserUpdateRequest();
			updatedUser.setEmailAddress("n.user@spam.gentics.com");
			updatedUser.setFirstname("Joe");
			updatedUser.setLastname("Doe");
			updatedUser.setUsername("new_user");
			// updatedUser.addGroup(group().getName());

			MeshResponse<UserResponse> future = client().updateUser(user.getUuid(), updatedUser).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", user.getUuid());
			User reloadedUser = boot.userRoot().findByUuid(user.getUuid());
			assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
			assertEquals("The firstname should not be updated.", user.getFirstname(), reloadedUser.getFirstname());
			assertEquals("The firstname should not be updated.", user.getLastname(), reloadedUser.getLastname());
		}
	}

	@Test
	public void testUpdateUserWithConflictingUsername() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// Create an user with a conflicting username
			UserRoot userRoot = meshRoot().getUserRoot();
			User user = userRoot.create("existing_username", user());
			user.addGroup(group());

			UserUpdateRequest request = new UserUpdateRequest();
			request.setUsername("existing_username");

			MeshResponse<UserResponse> future = client().updateUser(user().getUuid(), request).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "user_conflicting_username");
		}

	}

	@Test
	public void testUpdateUserWithSameUsername() throws Exception {
		try (NoTx noTx = db.noTx()) {
			User user = user();

			UserUpdateRequest request = new UserUpdateRequest();
			request.setUsername(user.getUsername());

			MeshResponse<UserResponse> future = client().updateUser(user.getUuid(), request).invoke();
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	public void testCreateUserWithConflictingUsername() throws Exception {
		try (NoTx noTx = db.noTx()) {
			// Create an user with a conflicting username
			UserRoot userRoot = meshRoot().getUserRoot();
			User user = userRoot.create("existing_username", user());
			user.addGroup(group());

			// Add update permission to group in order to create the user in
			// that group
			role().grantPermissions(group(), CREATE_PERM);
			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("existing_username");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");

			MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
			latchFor(future);
			expectException(future, CONFLICT, "user_conflicting_username");
		}

	}

	@Test
	public void testCreateUserWithNoPassword() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setEmailAddress("n.user@spam.gentics.com");
			newUser.setFirstname("Joe");
			newUser.setLastname("Doe");
			newUser.setUsername("new_user_test123");
			newUser.setGroupUuid(group().getUuid());

			MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "user_missing_password");
		}
	}

	@Test
	public void testCreateUserWithNoUsername() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setEmailAddress("n.user@spam.gentics.com");
			newUser.setFirstname("Joe");
			newUser.setLastname("Doe");
			newUser.setPassword("test123456");

			MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
			latchFor(future);
			expectException(future, BAD_REQUEST, "user_missing_username");
		}
	}

	@Test
	public void testCreateUserWithNoParentGroup() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");

		MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
		latchFor(future);
		assertSuccess(future);
		assertEquals(0, future.result().getGroups().size());
	}

	@Test
	public void testCreateUserWithBogusParentGroup() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.setGroupUuid("bogus");

		MeshResponse<UserResponse> future = client().createUser(newUser).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testCreateUpdate() {
		try (NoTx noTx = db.noTx()) {
			// Create a user with minimal properties
			UserCreateRequest request = new UserCreateRequest();
			request.setEmailAddress("n.user@spam.gentics.com");
			request.setUsername("new_user");
			request.setPassword("test123456");
			request.setGroupUuid(group().getUuid());

			UserResponse restUser = call(() -> client().createUser(request));
			assertThat(restUser).matches(request);

			UserUpdateRequest updateRequest = new UserUpdateRequest();
			final String LASTNAME = "Epic Stark";
			final String FIRSTNAME = "Tony Awesome";
			final String USERNAME = "dummy_user_changed";
			final String EMAIL = "t.stark@stark-industries.com";

			updateRequest.setEmailAddress(EMAIL);
			updateRequest.setFirstname(FIRSTNAME);
			updateRequest.setLastname(LASTNAME);
			updateRequest.setUsername(USERNAME);
			updateRequest.setPassword("newPassword");
			updateRequest.setOldPassword("test123456");
			String uuid = restUser.getUuid();
			restUser = call(() -> client().updateUser(uuid, updateRequest));
			assertEquals(LASTNAME, restUser.getLastname());
			assertEquals(FIRSTNAME, restUser.getFirstname());
			assertEquals(EMAIL, restUser.getEmailAddress());
			assertEquals(USERNAME, restUser.getUsername());
		}
	}

	@Test
	@Override
	public void testCreate() throws Exception {
		String groupUuid = db.noTx(() -> group().getUuid());
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(groupUuid);

		UserResponse restUser = call(() -> client().createUser(request));

		try (NoTx noTx2 = db.noTx()) {
			assertThat(restUser).matches(request);

			User user = boot.userRoot().findByUuid(restUser.getUuid());
			assertThat(restUser).matches(user);
		}
	}

	@Test
	@Override
	public void testCreateWithNoPerm() throws Exception {
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");

		try (NoTx noTx = db.noTx()) {
			role().revokePermissions(meshRoot().getUserRoot(), CREATE_PERM);
		}

		String userRootUuid = db.noTx(() -> meshRoot().getUserRoot().getUuid());
		call(() -> client().createUser(request), FORBIDDEN, "error_missing_perm", userRootUuid);
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			UserCreateRequest request = new UserCreateRequest();
			request.setEmailAddress("n.user@spam.gentics.com");
			request.setFirstname("Joe");
			request.setLastname("Doe");
			request.setUsername("new_user_" + i);
			request.setPassword("test123456");
			request.setGroupUuid(group().getUuid());
			set.add(client().createUser(request).invoke());
		}
		validateCreation(set, barrier);
	}

	/**
	 * Test whether the create rest call will create the correct permissions that allow removal of the object.
	 * 
	 * @throws Exception
	 */
	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		String groupUuid = db.noTx(() -> group().getUuid());

		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(groupUuid);

		UserResponse restUser = call(() -> client().createUser(request));
		assertThat(restUser).matches(request);

		call(() -> client().findUserByUuid(restUser.getUuid()));
		call(() -> client().deleteUser(restUser.getUuid()));
	}

	@Test
	@Ignore("this can't be tested using the rest client")
	public void testCreateUserWithBogusJson() throws Exception {

		// String requestJson = "bogus text";
		// Future<UserResponse> future =
		// getClient().createUser(userCreateRequest)
		// String response = request(info, HttpMethod.POST, "/api/v1/users/",
		// 400, "Bad Request", requestJson);
		// expectMessageResponse("error_parse_request_json_error", response);
	}

	// Delete tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (NoTx noTx = db.noTx()) {
			dummySearchProvider.clear();
			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setEmailAddress("n.user@spam.gentics.com");
			newUser.setFirstname("Joe");
			newUser.setLastname("Doe");
			newUser.setUsername("new_user");
			newUser.setPassword("test123456");
			newUser.setGroupUuid(group().getUuid());

			UserResponse restUser = call(() -> client().createUser(newUser));
			assertThat(dummySearchProvider).hasStore(User.composeIndexName(), User.composeIndexType(), restUser.getUuid());
			assertThat(dummySearchProvider).hasEvents(2, 0, 0, 0);
			dummySearchProvider.clear();

			assertTrue(restUser.getEnabled());
			String uuid = restUser.getUuid();

			MeshResponse<Void> future = client().deleteUser(uuid).invoke();
			latchFor(future);
			assertSuccess(future);

			try (Tx tx = db.tx()) {
				User loadedUser = boot.userRoot().findByUuid(uuid);
				assertNull("The user should have been deleted.", loadedUser);
			}

			// Load the user again and check whether it is disabled
			call(() -> client().findUserByUuid(uuid), NOT_FOUND, "object_not_found_for_uuid", uuid);

			assertThat(dummySearchProvider).hasDelete(User.composeIndexName(), User.composeIndexType(), uuid);
			assertThat(dummySearchProvider).hasEvents(0, 1, 0, 0);
		}

	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testDeleteByUUIDMultithreaded() throws InterruptedException {
		int nJobs = 3;
		String uuid = user().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<MeshResponse<Void>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(client().deleteUser(uuid).invoke());
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		try (NoTx noTx = db.noTx()) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User user = userRoot.create("extraUser", user());
			user.addGroup(group());
			String uuid = user.getUuid();
			assertNotNull(uuid);
			role().grantPermissions(user, UPDATE_PERM);
			role().grantPermissions(user, CREATE_PERM);
			role().grantPermissions(user, READ_PERM);

			MeshResponse<Void> future = client().deleteUser(uuid).invoke();
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", uuid);
			userRoot = meshRoot().getUserRoot();
			assertNotNull("The user should not have been deleted", userRoot.findByUuid(uuid));
		}
	}

	@Test(expected = NullPointerException.class)
	public void testDeleteWithUuidNull() throws Exception {
		MeshResponse<Void> future = client().deleteUser(null).invoke();
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "null");
	}

	@Test
	@Ignore
	public void testReadOwnCreatedUser() {

	}

	@Test
	public void testDeleteByUUID2() throws Exception {
		try (NoTx noTx = db.noTx()) {
			String name = "extraUser";
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create(name, user());
			extraUser.addGroup(group());
			String uuid = extraUser.getUuid();
			role().grantPermissions(extraUser, DELETE_PERM);

			assertTrue(role().hasPermission(DELETE_PERM, extraUser));

			User user = userRoot.findByUuid(uuid);
			assertEquals(1, user.getGroups().size());
			assertTrue("The user should be enabled", user.isEnabled());

			MeshResponse<Void> future = client().deleteUser(uuid).invoke();
			latchFor(future);
			assertSuccess(future);
			userRoot.reload();
			assertNull("The user was not deleted.", userRoot.findByUuid(uuid));

			// // Check whether the user was correctly disabled
			// try (NoTrx noTx = db.noTrx()) {
			// User user2 = userRoot.findByUuidBlocking(uuid);
			// user2.reload();
			// assertNotNull(user2);
			// assertFalse("The user should have been disabled",
			// user2.isEnabled());
			// assertEquals(0, user2.getGroups().size());
			// }
		}
	}

	@Test
	@Ignore("Not yet implemented")
	public void testDeleteOwnUser() {

		// String response = request(info, HttpMethod.DELETE, "/api/v1/users/" +
		// user.getUuid(), 403, "Forbidden");
	}

	@Test
	public void testLocationHeader() {
		String name = "someUser";

		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(name);
		request.setPassword("bla");
		MeshResponse<UserResponse> response = client().createUser(request).invoke();
		latchFor(response);
		assertSuccess(response);
		try (NoTx noTx = db.noTx()) {
			meshRoot().getUserRoot().reload();
			User user = meshRoot().getUserRoot().findByUsername(name);
			assertNotNull("User should have been created.", user);
			assertEquals(CREATED.code(), response.getResponse().statusCode());
			String location = response.getResponse().getHeader(HttpHeaders.LOCATION);
			assertEquals("Location header value did not match", "http://localhost:" + getPort() + "/api/v1/users/" + user.getUuid(), location);
		}
	}

	@Test
	public void testLocationWithHostHeader() {
		String name = "someUser";

		UserCreateRequest userRequest = new UserCreateRequest();
		userRequest.setUsername(name);
		userRequest.setPassword("bla");

		MeshRequest<UserResponse> request = client().createUser(userRequest);
		request.getRequest().putHeader(HttpHeaders.HOST, "jotschi.de:" + getPort());
		MeshResponse<UserResponse> response = request.invoke();
		latchFor(response);
		assertSuccess(response);
		try (NoTx noTx = db.noTx()) {
			meshRoot().getUserRoot().reload();
			User user = meshRoot().getUserRoot().findByUsername(name);
			assertNotNull("User should have been created.", user);
			assertEquals(CREATED.code(), response.getResponse().statusCode());
			String location = response.getResponse().getHeader(HttpHeaders.LOCATION);
			assertEquals("Location header value did not match", "http://jotschi.de:" + getPort() + "/api/v1/users/" + user.getUuid(), location);
		}
	}
}
