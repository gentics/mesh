package com.gentics.mesh.core.user;

import static com.gentics.mesh.MeshVersion.CURRENT_API_BASE_PATH;
import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;
import static com.gentics.mesh.core.rest.MeshEvent.USER_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.USER_UPDATED;
import static com.gentics.mesh.core.rest.common.Permission.CREATE;
import static com.gentics.mesh.core.rest.common.Permission.DELETE;
import static com.gentics.mesh.core.rest.common.Permission.READ;
import static com.gentics.mesh.core.rest.common.Permission.UPDATE;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.validateDeletion;
import static com.gentics.mesh.test.ElasticsearchTestMode.TRACKING;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.PROJECT_AND_NODE;
import static com.gentics.mesh.test.context.MeshTestHelper.awaitConcurrentRequests;
import static com.gentics.mesh.test.context.MeshTestHelper.validateCreation;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.CREATED;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static io.vertx.core.http.HttpHeaders.HOST;
import static io.vertx.core.http.HttpHeaders.LOCATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.junit.Ignore;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.tagfamily.HibTagFamily;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.SortOrder;
import com.gentics.mesh.core.rest.common.ListResponse;
import com.gentics.mesh.core.rest.common.Permission;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserAPITokenResponse;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserPermissionResponse;
import com.gentics.mesh.core.rest.user.UserResetTokenResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.parameter.client.GenericParametersImpl;
import com.gentics.mesh.parameter.impl.NodeParametersImpl;
import com.gentics.mesh.parameter.impl.PagingParametersImpl;
import com.gentics.mesh.parameter.impl.RolePermissionParametersImpl;
import com.gentics.mesh.parameter.impl.SortingParametersImpl;
import com.gentics.mesh.parameter.impl.UserParametersImpl;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.definition.BasicRestTestcases;
import com.gentics.mesh.util.UUIDUtil;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(elasticsearch = TRACKING, testSize = PROJECT_AND_NODE, startServer = true)
public class UserEndpointTest extends AbstractMeshTest implements BasicRestTestcases {

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		String uuid = userUuid();
		UserResponse restUser = call(() -> client().findUserByUuid(uuid));
		try (Tx tx = tx()) {
			assertThat(restUser).matches(user());
		}
		// TODO assert groups
		// TODO assert perms
	}

	@Test
	public void testReadByUUIDRaw() throws IOException {
		String uuid = userUuid();
		JsonObject json = httpGetNowJson(CURRENT_API_BASE_PATH + "/users/" + uuid, client().getAuthentication().getToken(),
			new GenericParametersImpl().setFields("uuid"));
		assertEquals(uuid, json.getString("uuid"));
		assertFalse(json.containsKey("lastname"));
		assertFalse(json.containsKey("firstname"));
		assertFalse(json.containsKey("username"));
	}

	@Test
	public void testUpdateUsingExpiredToken() {
		String uuid = userUuid();
		String oldHash = tx(() -> user().getPasswordHash());
		assertNull("Initially the token code should be null", tx(() -> user().getResetToken()));
		assertNull("Initially the token issue timestamp should be null", tx(() -> user().getResetTokenIssueTimestamp()));

		// 1. Get new token
		UserResetTokenResponse response = call(() -> client().getUserResetToken(uuid));
		assertNotNull("The user token code should now be set to a non-null value but it was not.", tx(response::getToken));
		assertNotNull("The token code issue timestamp should be set.", tx(response::getCreated));

		// 2. Fake an old issue timestamp
		tx(tx -> {
			HibUser user1 = tx.userDao().findByUuid(uuid);
			user1.setResetTokenIssueTimestamp(System.currentTimeMillis() - 1000 * 60 * 60);
			return tx.<CommonTx>unwrap().userDao().mergeIntoPersisted(user1);
		});

		// 2. Logout the current client user
		client().logout().blockingGet();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl(response.getToken())), UNAUTHORIZED,
			"user_error_provided_token_invalid");

		// 4. Assert that the password was not updated
		String newHash = tx(tx -> { return tx.userDao().findByUuid(uuid).getPasswordHash(); });
		assertEquals("The password hash has not been updated.", oldHash, newHash);
		assertNull("The token code should have been set to null since it has expired.", tx(() -> user().getResetToken()));
		assertNull("The token issue timestamp should have been set to null since it has expired", tx(() -> user().getResetTokenIssueTimestamp()));

	}

	/**
	 * Test that the user is able to update itself even without the assigned perm and being logged out. The reset token should grant the right to update
	 * himself.
	 */
	@Test
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void testUpdateUsingToken() {
		String uuid = tx(() -> user().getUuid());
		String oldHash = tx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", tx(() -> user().getResetToken()));

		// 1. Get new token
		UserResetTokenResponse response = call(() -> client().getUserResetToken(uuid));
		assertNotNull("The user token code should now be set to a non-null value but it was not", tx(() -> user().getResetToken()));

		// 2. Logout the current client user
		client().logout().blockingGet();
		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), user(), UPDATE_PERM);
		});

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl(response.getToken())));

		// 4. Assert that the password was updated
		HibUser updatedUser = tx(tx -> { return tx.userDao().findByUuid(uuid); });
		String newHash = tx(updatedUser::getPasswordHash);
		assertNull("The token code should have been set to null since it is now used up", tx(updatedUser::getResetToken));

		assertNotEquals("The password hash has not been updated.", oldHash, newHash);
	}

	@Test
	@Override
	public void testReadPermittedSorted() throws Exception {
		for (int i = 0; i < 10; i++) {
			final String name = "test12345_" + i;
			UserCreateRequest request = new UserCreateRequest();
			request.setUsername(name);
			request.setPassword(name);
			UserResponse response = call(() -> client().createUser(request));
			if ((i % 2) == 0) {
				tx(tx -> {
					tx.roleDao().revokePermissions(role(), tx.userDao().findByUuid(response.getUuid()), READ_PERM);
				});
			}
		}
		UserListResponse list = call(() -> client().findUsers(new SortingParametersImpl("name", SortOrder.DESCENDING)));
		assertEquals("Total data size should be 8", 8, list.getData().size());
		assertThat(list.getData()).isSortedAccordingTo((fa, fb) -> getTestContext().getSortComparator().reversed().compare(
				fa != null ? fa.getUsername() : null,
				fb != null ? fb.getUsername() : null));
	}

	/**
	 * Test that the user is able to update itself even without the assigned perm. The reset token should grant the right to update himself.
	 */
	@Test
	@Ignore("Fails on CI pipeline. See https://github.com/gentics/mesh/issues/608")
	public void testUpdateUsingTokenWithoutLogout() {
		String uuid = tx(() -> user().getUuid());
		String oldHash = tx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", tx(() -> user().getResetToken()));

		// 1. Get new token
		UserResetTokenResponse response = call(() -> client().getUserResetToken(uuid));
		assertNotNull("The user token code should now be set to a non-null value but it was not", tx(() -> user().getResetToken()));

		// Make sure the user has no actual update perm anymore
		tx(tx -> {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), user(), UPDATE_PERM);
		});

		// 2. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl(response.getToken())));

		// 3. Assert that the password was updated
		String newHash = tx(() -> user().getPasswordHash());
		assertNull("The token code should have been set to null since it is now used up", tx(() -> user().getResetToken()));

		assertNotEquals("The password hash has not been updated.", oldHash, newHash);
	}

	@Test
	public void testUpdateUsingBogusToken() {
		String uuid = tx(() -> user().getUuid());
		String oldHash = tx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", tx(() -> user().getResetToken()));

		// 1. Get new token
		call(() -> client().getUserResetToken(uuid));

		// 2. Logout the current client user
		client().logout().blockingGet();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request, new UserParametersImpl("bogusToken")), UNAUTHORIZED, "user_error_provided_token_invalid");

		// 4. Assert that the password was not updated
		String newHash = tx(() -> user().getPasswordHash());
		assertEquals("The password hash should not have been updated.", oldHash, newHash);

	}

	@Test
	public void testUpdateWithNoToken() {
		disableAnonymousAccess();
		String uuid = tx(() -> user().getUuid());
		String oldHash = tx(() -> user().getPasswordHash());
		assertNull("Initially the token code should have been set to null", tx(() -> user().getResetToken()));

		// 1. Get new token
		call(() -> client().getUserResetToken(uuid));

		// 2. Logout the current client user
		client().logout().blockingGet();

		// 3. Update the user using the token code
		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("newPass");
		call(() -> client().updateUser(uuid, request), UNAUTHORIZED, "error_not_authorized");

		// 4. Assert that the password was not updated
		String newHash = tx(() -> user().getPasswordHash());
		assertEquals("The password hash should not have been updated.", oldHash, newHash);
	}

	@Test
	public void testFetchUserToken() {
		String uuid = tx(() -> user().getUuid());

		UserResetTokenResponse response = call(() -> client().getUserResetToken(uuid));
		assertThat(response.getToken()).isNotEmpty();
		String storedToken = tx(tx -> { return tx.userDao().findByUuid(uuid).getResetToken(); });
		assertEquals("The token that is currently stored did not match up with the returned token by the API", storedToken, response.getToken());
	}

	@Test
	public void testAPIToken() {
		String uuid = tx(() -> user().getUuid());
		MeshResponse<UserAPITokenResponse> completeResponse = client().issueAPIToken(uuid).getResponse().blockingGet();
		assertThat(completeResponse.getHeader("Cache-Control")).hasValue("private");

		UserAPITokenResponse response = completeResponse.getBody();
		assertNull("The key was previously not issued.", response.getPreviousIssueDate());
		assertThat(response.getToken()).isNotEmpty();

		assertNotNull(tx(tx -> { return tx.userDao().findByUuid(uuid).getAPIKeyTokenCode(); }));
		client().setLogin(null, null);
		client().setAPIKey(response.getToken());

		// Check whether new cookies are generated when using an API key
		MeshRequest<UserResponse> userRequest = client().findUserByUuid(uuid);
		MeshResponse<UserResponse> userResponse = userRequest.getResponse().blockingGet();
		assertThat(userResponse.getCookies()).as("Requests using the api key should not yield a new cookie").isEmpty();

		// Now invalidate the api key by generating a new one
		String oldKey = response.getToken();
		response = call(() -> client().issueAPIToken(uuid));
		assertNotEquals("Each key should be unique.", oldKey, response.getToken());
		assertNotNull("The key was already requested once. Thus the date should be set.", response.getPreviousIssueDate());

		// And continue invoking requests
		call(() -> client().findUserByUuid(uuid), UNAUTHORIZED, "error_not_authorized");

		// Now set the active key and verify that the request works
		client().setAPIKey(response.getToken());

		call(() -> client().findUserByUuid(uuid));

		call(() -> client().invalidateAPIToken(uuid));
		assertNull(tx(tx -> { return tx.userDao().findByUuid(uuid).getAPIKeyTokenCode(); }));
		assertNull(tx(tx -> { return tx.userDao().findByUuid(uuid).getAPITokenIssueTimestamp(); }));
		call(() -> client().findUserByUuid(uuid), UNAUTHORIZED, "error_not_authorized");
	}

	@Test
	public void testIssueAPIKeyWithoutPerm() {
		tx((tx) -> {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), user(), UPDATE_PERM);
			tx.success();
		});

		call(() -> client().findUserByUuid(userUuid()));
		call(() -> client().issueAPIToken(userUuid()), FORBIDDEN, "error_missing_perm", userUuid(), UPDATE_PERM.getRestPerm().getName());
	}

	@Test
	public void testRevokeAPIKeyWithoutPerm() {

		String uuid = userUuid();
		call(() -> client().findUserByUuid(uuid));
		call(() -> client().issueAPIToken(uuid));

		tx((tx) -> {
			RoleDao roleDao = tx.roleDao();
			HibUser user = user();
			roleDao.revokePermissions(role(), user, UPDATE_PERM);
			tx.success();
		});

		call(() -> client().invalidateAPIToken(uuid), FORBIDDEN, "error_missing_perm", uuid, UPDATE_PERM.getRestPerm().getName());
	}

	@Test
	public void testReadPermissions() {
		HibTagFamily tagFamily;
		String pathToElement;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			data().addTagFamilies();
			tagFamily = data().getTagFamily("colors");
			roleDao.grantPermissions(role(), tagFamily, InternalPermission.values());

			// Add permission on own role
			roleDao.grantPermissions(role(), tagFamily, InternalPermission.UPDATE_PERM);
			assertTrue(roleDao.hasPermission(role(), InternalPermission.UPDATE_PERM, tagFamily));
			pathToElement = "projects/" + project().getUuid() + "/tagFamilies/" + tagFamily.getUuid();
			tx.success();
		}

		String userUuid = tx(() -> user().getUuid());
		UserPermissionResponse response = call(() -> client().readUserPermissions(userUuid, pathToElement));
		assertNotNull(response);
		assertThat(response).hasPerm(Permission.basicPermissions());

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			// Revoke single permission and check again
			roleDao.revokePermissions(role(), tagFamily, InternalPermission.UPDATE_PERM);
			assertFalse(roleDao.hasPermission(role(), InternalPermission.UPDATE_PERM, tagFamily));
			tx.success();
		}
		UserPermissionResponse permissionResponse = call(() -> client().readUserPermissions(userUuid, pathToElement));
		assertNotNull(permissionResponse);
		assertThat(permissionResponse).hasPerm(READ, CREATE, DELETE);
	}

	@Test
	public void testReadByUuidWithRolePerms() {
		UserResponse userResponse = call(() -> client().findUserByUuid(userUuid(), new RolePermissionParametersImpl().setRoleUuid(roleUuid())));
		assertNotNull(userResponse.getRolePerms());
		assertThat(userResponse.getRolePerms()).hasPerm(READ, CREATE, UPDATE, DELETE);
	}

	@Test
	public void testReadUserWithMultipleGroups() {
		try (Tx tx = tx()) {
			GroupDao groupDao = tx.groupDao();
			UserDao userDao = tx.userDao();

			HibUser user = user();
			assertEquals(1, userDao.getGroups(user).count());

			for (int i = 0; i < 10; i++) {
				HibGroup extraGroup = groupDao.create("group_" + i, user());
				tx.groupDao().addUser(extraGroup, user());
			}
			assertEquals(11, userDao.getGroups(user()).count());
			tx.success();
		}

		UserResponse response = call(() -> client().findUserByUuid(userUuid()));
		assertEquals(11, response.getGroups().size());
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws InterruptedException {
		try (Tx tx = tx()) {
			int nJobs = 10;
			awaitConcurrentRequests(nJobs, i -> client().findUserByUuid(userUuid()));
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		int nJobs = 200;
		awaitConcurrentRequests(nJobs, i -> client().findUserByUuid(userUuid()));
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibUser user = user();
			assertNotNull("The username of the user must not be null.", user.getUsername());
			roleDao.revokePermissions(role(), user, READ_PERM);
			tx.success();
		}
		call(() -> client().findUserByUuid(userUuid()), FORBIDDEN, "error_missing_perm", userUuid(), READ_PERM.getRestPerm().getName());
	}

	@Test
	public void testCreateAdmin() {
		UserCreateRequest request = new UserCreateRequest();
		request.setUsername("test1234");
		request.setAdmin(true);
		request.setPassword("finger");

		UserResponse response = adminCall(() -> client().createUser(request));
		assertThat(response).isAdmin();

		// Test missing permission
		request.setUsername("test4321");
		call(() -> client().createUser(request), FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
	}

	@Test
	public void testUpdateAdmin() {
		UserCreateRequest createRequest = new UserCreateRequest();
		createRequest.setUsername("test1234");
		createRequest.setAdmin(false);
		createRequest.setPassword("finger");

		UserResponse response = adminCall(() -> client().createUser(createRequest));
		assertThat(response).isNotAdmin();

		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setAdmin(true);
		UserResponse response2 = adminCall(() -> client().updateUser(response.getUuid(), updateRequest));
		assertThat(response2).isAdmin();

		// Test missing permission
		updateRequest.setAdmin(false);
		call(() -> client().updateUser(response.getUuid(), updateRequest), FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
	}

	@Test
	public void testCreateAdminUserAsNonAdmin() {
		UserCreateRequest createRequest = new UserCreateRequest();
		createRequest.setUsername("test1234");
		createRequest.setAdmin(true);
		createRequest.setPassword("finger");

		runAsNonAdmin(() -> {
			return call(() -> client().createUser(createRequest), FORBIDDEN, "user_error_admin_privilege_needed_for_admin_flag");
		});
	}

	@Test
	public void testCreateNonAdminUserAsNonAdmin() {
		UserCreateRequest createRequest = new UserCreateRequest();
		createRequest.setUsername("test1234");
		createRequest.setAdmin(false);
		createRequest.setPassword("finger");

		UserResponse response = nonAdminCall(() -> client().createUser(createRequest));
		assertThat(response).isNotAdmin();
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {
		final int intialUserCount = users().size();
		final int nUsers = 20;

		long foundUsers;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			GroupDao groupDao = tx.groupDao();

			for (int i = 0; i < nUsers; i++) {
				String username = "testuser_" + i;
				HibUser user = userDao.create(username, user());
				groupDao.addUser(group(), user);
				user.setLastname("should_be_listed");
				user.setFirstname("should_be_listed");
				user.setEmailAddress("should_be_listed");
				roleDao.grantPermissions(role(), user, READ_PERM);
			}
			HibUser invisibleUser = userDao.create("should_not_be_listed", user());
			invisibleUser.setLastname("should_not_be_listed");
			invisibleUser.setFirstname("should_not_be_listed");
			invisibleUser.setEmailAddress("should_not_be_listed");
			userDao.addGroup(invisibleUser, group());
			foundUsers = userDao.count();
			tx.success();
		}

		assertEquals("We did not find the expected count of users attached to the user root vertex.", intialUserCount + nUsers + 1, foundUsers);

		// Test default paging parameters
		ListResponse<UserResponse> restResponse = call(() -> client().findUsers());
		assertNull(restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		// Admin User + Guest User + Dummy User = 3
		assertEquals(intialUserCount + nUsers, restResponse.getMetainfo().getTotalCount());
		assertEquals(intialUserCount + nUsers, restResponse.getData().size());

		long perPage = 2;
		int totalUsers = intialUserCount + nUsers;
		int totalPages = ((int) Math.ceil(totalUsers / (double) perPage));
		final long currentPerPage = 2;
		restResponse = call(() -> client().findUsers(new PagingParametersImpl(3, currentPerPage)));

		assertEquals("The page did not contain the expected amount of items", perPage, restResponse.getData().size());
		assertEquals("We did not find the expected page in the list response.", 3, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The amount of pages did not match. We have {" + totalUsers + "} users in the system and use a paging of {" + currentPerPage
			+ "}", totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(currentPerPage, restResponse.getMetainfo().getPerPage().longValue());
		assertEquals("The total amount of items does not match the expected one", totalUsers, restResponse.getMetainfo().getTotalCount());

		perPage = 11;

		List<UserResponse> allUsers = new ArrayList<>();
		for (int page = 1; page < totalPages; page++) {
			restResponse = client().findUsers(new PagingParametersImpl(page, perPage)).blockingGet();
			allUsers.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalUsers, allUsers.size());

		// Verify that the invisible is not part of the response
		final String extra3Username = "should_not_be_listed";
		List<UserResponse> filteredUserList = allUsers.parallelStream().filter(restUser -> restUser.getUsername().equals(extra3Username)).collect(
			Collectors.toList());
		assertTrue("User 3 should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		call(() -> client().findUsers(new PagingParametersImpl(1, -1L)), BAD_REQUEST, "error_pagesize_parameter", "-1");

		UserListResponse listResponse = call(() -> client().findUsers(new PagingParametersImpl(4242, 25L)));

		assertEquals("The result list should not contain any item since the page parameter is out of bounds", 0, listResponse.getData().size());
		assertEquals("The requested page should be set in the response but it was not", 4242, listResponse.getMetainfo().getCurrentPage());
		assertEquals("The page count value was not correct.", 1, listResponse.getMetainfo().getPageCount());
		assertEquals("We did not find the correct total count value in the response", nUsers + intialUserCount, listResponse.getMetainfo()
			.getTotalCount());
		assertEquals(25L, listResponse.getMetainfo().getPerPage().longValue());

		listResponse = call(() -> client().findUsers(new PagingParametersImpl(4242)));
		assertNull(listResponse.getMetainfo().getPerPage());

		verifySorting(param -> call(() -> client().findUsers(param)), UserResponse::getUsername, "username", "List of usernames");
	}

	@Test
	public void testInvalidPageParameter() {
		UserListResponse list = call(() -> client().findUsers(new PagingParametersImpl(1, 0L)));
		assertEquals(0, list.getData().size());
		assertTrue(list.getMetainfo().getTotalCount() > 0);
	}

	@Test
	public void testInvalidPageParameter2() {
		call(() -> client().findUsers(new PagingParametersImpl(-1, 25L)), BAD_REQUEST, "error_page_parameter_must_be_positive", "-1");
	}

	@Test
	@Override
	public void testUpdateMultithreaded() throws InterruptedException {
		String uuid = tx(() -> user().getUuid());
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");

		int nJobs = 50;
		awaitConcurrentRequests(nJobs, i -> client().updateUser(uuid, updateRequest));
	}

	@Test
	@Override
	public void testUpdate() throws Exception {
		String oldName = tx(() -> user().getUsername());
		String newName = "dummy_user_changed";
		String uuid = tx(() -> user().getUuid());

		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");
		updateRequest.setUsername(newName);

		expect(USER_UPDATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName(newName).hasUuid(uuid);
		}).total(1);

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));

		awaitEvents();
		waitForSearchIdleEvent();

		assertThat(trackingSearchProvider()).hasStore(HibUser.composeIndexName(), uuid);
		assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);
		trackingSearchProvider().clear().blockingAwait();

		try (Tx tx = tx()) {
			assertThat(restUser).matches(updateRequest);
			assertNull("The user node should have been updated and thus no user should be found.", tx.userDao().findByUsername(oldName));
			HibUser reloadedUser = tx.userDao().findByUsername(newName);
			assertNotNull(reloadedUser);
			assertEquals("Epic Stark", reloadedUser.getLastname());
			assertEquals("Tony Awesome", reloadedUser.getFirstname());
			assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
			assertEquals(newName, reloadedUser.getUsername());
		}
	}

	@Test
	public void testUpdateWithSpecialCharacters() throws Exception {
		String uuid = tx(() -> user().getUuid());
		String oldUsername = tx(() -> user().getUsername());

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

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));
		assertThat(restUser).matches(updateRequest);
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			assertNull("The user node should have been updated and thus no user should be found.", userDao.findByUsername(oldUsername));
			HibUser reloadedUser = userDao.findByUsername(username);
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
		call(() -> client().updateUser("bogus", request), BAD_REQUEST, "error_illegal_uuid", "bogus");
	}

	@Test
	public void testUpdateUserAndSetNodeReference() throws Exception {
		String nodeUuid = tx(() -> folder("news").getUuid());
		String userUuid = userUuid();
		HibUser user = user();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		String username = tx(() -> user.getUsername());
		try (Tx tx = tx()) {
			updateRequest.setEmailAddress("t.stark@stark-industries.com");
			updateRequest.setFirstname("Tony Awesome");
			updateRequest.setLastname("Epic Stark");
			updateRequest.setUsername("dummy_user_changed");
		}

		NodeReference userNodeReference = new NodeReference();
		userNodeReference.setProjectName(PROJECT_NAME);
		userNodeReference.setUuid(nodeUuid);
		updateRequest.setNodeReference(userNodeReference);

		UserResponse restUser = call(() -> client().updateUser(userUuid, updateRequest));
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			assertNotNull(tx.userDao().findByUuid(user().getUuid()).getReferencedNode());
			assertNotNull(restUser.getNodeReference());
			assertEquals(PROJECT_NAME, ((NodeReference) restUser.getNodeReference()).getProjectName());
			assertEquals(nodeUuid, restUser.getNodeReference().getUuid());
			assertThat(restUser).matches(updateRequest);
			assertNull("The user node should have been updated and thus no user should be found.", userDao.findByUsername(username));
			HibUser reloadedUser = userDao.findByUsername("dummy_user_changed");
			assertNotNull(reloadedUser);
			assertEquals("Epic Stark", reloadedUser.getLastname());
			assertEquals("Tony Awesome", reloadedUser.getFirstname());
			assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
			assertEquals("dummy_user_changed", reloadedUser.getUsername());
			assertEquals(nodeUuid, reloadedUser.getReferencedNode().getUuid());
		}
	}

	@Test
	public void testUpdateNodeReferenceTwice() throws Exception {
		String nodeUuid = tx(() -> folder("news").getUuid());
		String nodeUuid2 = tx(() -> folder("2015").getUuid());
		String userUuid = userUuid();
		HibUser user = user();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		String username = tx(() -> user.getUsername());
		try (Tx tx = tx()) {
			updateRequest.setEmailAddress("t.stark@stark-industries.com");
			updateRequest.setFirstname("Tony Awesome");
			updateRequest.setLastname("Epic Stark");
			updateRequest.setUsername("dummy_user_changed");
		}

		NodeReference userNodeReference = new NodeReference();
		userNodeReference.setProjectName(PROJECT_NAME);
		userNodeReference.setUuid(nodeUuid2);
		updateRequest.setNodeReference(userNodeReference);

		call(() -> client().updateUser(userUuid, updateRequest));

		userNodeReference.setUuid(nodeUuid);

		UserResponse restUser = call(() -> client().updateUser(userUuid, updateRequest));

		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			assertNotNull(tx.userDao().findByUuid(user().getUuid()).getReferencedNode());
			assertNotNull(restUser.getNodeReference());
			assertEquals(PROJECT_NAME, ((NodeReference) restUser.getNodeReference()).getProjectName());
			assertEquals(nodeUuid, restUser.getNodeReference().getUuid());
			assertThat(restUser).matches(updateRequest);
			assertNull("The user node should have been updated and thus no user should be found.", userDao.findByUsername(username));
			HibUser reloadedUser = userDao.findByUsername("dummy_user_changed");
			assertNotNull(reloadedUser);
			assertEquals("Epic Stark", reloadedUser.getLastname());
			assertEquals("Tony Awesome", reloadedUser.getFirstname());
			assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
			assertEquals("dummy_user_changed", reloadedUser.getUsername());
			assertEquals(nodeUuid, reloadedUser.getReferencedNode().getUuid());
		}
	}

	@Test
	public void testUpdateUserAndSetNodeReferenceWithoutProjectName() throws Exception {
		String nodeUuid = tx(() -> folder("news").getUuid());
		String userUuid = userUuid();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		try (Tx tx = tx()) {
			updateRequest.setEmailAddress("t.stark@stark-industries.com");
			updateRequest.setFirstname("Tony Awesome");
			updateRequest.setLastname("Epic Stark");
			updateRequest.setUsername("dummy_user_changed");
		}

		NodeReference userNodeReference = new NodeReference();
		userNodeReference.setUuid(nodeUuid);
		updateRequest.setNodeReference(userNodeReference);

		call(() -> client().updateUser(userUuid, updateRequest), BAD_REQUEST, "user_incomplete_node_reference");
	}

	@Test
	public void testCreateUser() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(groupUuid());
		newUser.setPassword("test1234");

		expect(USER_CREATED).match(1, MeshElementEventModelImpl.class, event -> {
			assertThat(event).hasName("new_user").uuidNotNull();
		});

		UserResponse response = call(() -> client().createUser(newUser));

		awaitEvents();

		assertEquals("new_user", response.getUsername());
		assertNotNull(response.getUuid());
		assertThat(response.getGroups()).hasSize(1);
		assertEquals(groupUuid(), response.getGroups().get(0).getUuid());
	}

	@Test
	public void testCreateUserWithNodeReference() {
		String nodeUuid;
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();
			HibNode node = folder("news");
			nodeUuid = node.getUuid();
			assertTrue(userDao.hasPermission(user(), node, READ_PERM));
			tx.success();
		}

		NodeReference reference = new NodeReference();
		reference.setProjectName(PROJECT_NAME);
		reference.setUuid(nodeUuid);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(groupUuid());
		newUser.setPassword("test1234");
		newUser.setNodeReference(reference);

		UserResponse response = call(() -> client().createUser(newUser));
		assertTrue(response.isReference());
		assertNotNull(response.getNodeReference());
		assertNotNull(response.getReferencedNodeReference().getProjectName());
		assertNotNull(response.getNodeReference().getUuid());

	}

	@Test
	public void testReadUserListWithExpandedNodeReference() {
		UserResponse userCreateResponse = tx(() -> {
			HibNode node = folder("news");

			NodeReference reference = new NodeReference();
			reference.setUuid(node.getUuid());
			reference.setProjectName(PROJECT_NAME);

			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setUsername("new_user");
			newUser.setGroupUuid(group().getUuid());
			newUser.setPassword("test1234");
			newUser.setNodeReference(reference);
			UserResponse response = call(() -> client().createUser(newUser));
			return response;
		});

		try (Tx tx = tx()) {
			HibNode node = folder("news");
			UserListResponse userResponse = call(() -> client().findUsers(new PagingParametersImpl().setPerPage(100L), new NodeParametersImpl()
				.setExpandedFieldNames("nodeReference").setLanguages("en")));
			assertNotNull(userResponse);

			UserResponse foundUser = userResponse.getData().stream().filter(u -> u.getUuid().equals(userCreateResponse.getUuid())).findFirst().get();
			assertNotNull(foundUser.getNodeReference());
			assertEquals(node.getUuid(), foundUser.getNodeReference().getUuid());
			assertEquals(NodeResponse.class, foundUser.getNodeReference().getClass());
		}
	}

	@Test
	// test fails since user node references are not yet branch aware
	public void testReadUserWithExpandedNodeReference() {
		String folderUuid;
		UserCreateRequest newUser;
		try (Tx tx = tx()) {
			HibNode node = folder("news");
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

		UserResponse userResponse = call(() -> client().createUser(newUser));
		UserResponse userResponse2 = call(() -> client().findUserByUuid(userResponse.getUuid(), new NodeParametersImpl().setExpandedFieldNames(
			"nodeReference").setLanguages("en")));
		assertNotNull(userResponse2);
		assertNotNull(userResponse2.getNodeReference());
		assertEquals(folderUuid, userResponse2.getNodeReference().getUuid());
		assertEquals(NodeResponse.class, userResponse2.getNodeReference().getClass());

	}

	@Test
	public void testCreateUserWithBogusProjectNameInNodeReference() {
		try (Tx tx = tx()) {
			HibNode node = folder("news");

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
		try (Tx tx = tx()) {
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
		try (Tx tx = tx()) {
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

		try (Tx tx = tx()) {
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
	// try (Tx tx = tx()) {
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
	// try (Tx tx = tx()) {
	// User reloadedUser = boot.userRoot().findByUuidGlobal(uuid);
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
	// try (Tx tx = tx()) {
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
	// try (Tx tx = tx()) {
	// User reloadedUser = boot.userRoot().findByUuidGlobal(uuid);
	// assertEquals("The hash should not be different since the password should
	// not have been updated.", oldHash,
	// reloadedUser.getPasswordHash());
	// }
	//
	// }

	@Test
	public void testUpdatePasswordWithEmptyPassword() {
		String uuid;
		String oldHash;

		try (Tx tx = tx()) {
			tx.userDao().updatePasswordHash(user(), null);
			uuid = user().getUuid();
			oldHash = user().getPasswordHash();
			tx.success();
		}

		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setPassword("   ");

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));
		assertThat(restUser).matches(updateRequest);

		try (Tx tx = tx()) {
			HibUser reloadedUser = tx.userDao().findByUuid(uuid);
			assertEquals("The hash should not be different and thus the password stays old.", oldHash, reloadedUser.getPasswordHash());
		}
	}

	@Test
	public void testUpdatePasswordWithNoOldPassword() {
		String uuid;
		String oldHash;

		try (Tx tx = tx()) {
			tx.userDao().updatePasswordHash(user(), null);
			uuid = user().getUuid();
			oldHash = user().getPasswordHash();
			tx.success();
		}

		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setPassword("new_password");

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));
		assertThat(restUser).matches(updateRequest);

		try (Tx tx = tx()) {
			HibUser reloadedUser = tx.userDao().findByUuid(uuid);
			assertNotEquals("The hash should be different and thus the password updated.", oldHash, reloadedUser.getPasswordHash());
		}
	}

	@Test
	public void testUpdatePassword() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		String username;
		String uuid;
		String oldHash;
		try (Tx tx = tx()) {
			HibUser user = user();
			username = user.getUsername();
			uuid = user.getUuid();
			oldHash = user.getPasswordHash();
			tx.success();
		}
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setPassword("new_password");
		updateRequest.setOldPassword("test123");

		UserResponse restUser = call(() -> client().updateUser(uuid, updateRequest));
		assertThat(restUser).matches(updateRequest);

		try (Tx tx = tx()) {
			HibUser reloadedUser = tx.userDao().findByUsername(username);
			assertNotEquals("The hash should be different and thus the password updated.", oldHash, reloadedUser.getPasswordHash());
		}
	}

	@Test
	public void testUpdatePasswordWithNoPermission() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		String oldHash;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibUser user = user();
			oldHash = user.getPasswordHash();
			roleDao.revokePermissions(role(), user, UPDATE_PERM);
			tx.success();
		}

		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("new_password");
		call(() -> client().updateUser(userUuid(), request), FORBIDDEN, "error_missing_perm", userUuid(), UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			HibUser reloadedUser = tx.userDao().findByUuid(userUuid());
			assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		String oldHash;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			HibUser user = user();
			oldHash = user.getPasswordHash();
			roleDao.revokePermissions(role(), user, UPDATE_PERM);
			// updatedUser.addGroup(group().getName());
			tx.success();
		}

		UserUpdateRequest updatedUser = new UserUpdateRequest();
		updatedUser.setEmailAddress("n.user@spam.gentics.com");
		updatedUser.setFirstname("Joe");
		updatedUser.setLastname("Doe");
		updatedUser.setUsername("new_user");
		call(() -> client().updateUser(userUuid(), updatedUser), FORBIDDEN, "error_missing_perm", userUuid(), UPDATE_PERM.getRestPerm().getName());

		try (Tx tx = tx()) {
			HibUser reloadedUser = tx.userDao().findByUuid(userUuid());
			assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
			assertEquals("The firstname should not be updated.", user().getFirstname(), reloadedUser.getFirstname());
			assertEquals("The firstname should not be updated.", user().getLastname(), reloadedUser.getLastname());
		}

	}

	@Test
	public void testUpdateUserWithConflictingUsername() throws Exception {
		try (Tx tx = tx()) {
			UserDao userDao = tx.userDao();

			// Create an user with a conflicting username
			HibUser user = userDao.create("existing_username", user());
			userDao.addGroup(user, group());
			tx.success();
		}

		UserUpdateRequest request = new UserUpdateRequest();
		request.setUsername("existing_username");
		call(() -> client().updateUser(userUuid(), request), CONFLICT, "user_conflicting_username");

	}

	@Test
	public void testUpdateUserWithSameUsername() throws Exception {
		try (Tx tx = tx()) {
			UserUpdateRequest request = new UserUpdateRequest();
			request.setUsername(user().getUsername());
			call(() -> client().updateUser(userUuid(), request));
		}
	}

	@Test
	public void testCreateUserWithConflictingUsername() throws Exception {
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();

			// Create an user with a conflicting username
			HibUser user = userDao.create("existing_username", user());
			userDao.addGroup(user, group());

			// Add update permission to group in order to create the user in
			// that group
			roleDao.grantPermissions(role(), group(), CREATE_PERM);
			tx.success();
		}

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("existing_username");
		newUser.setGroupUuid(groupUuid());
		newUser.setPassword("test1234");
		call(() -> client().createUser(newUser), CONFLICT, "user_conflicting_username");
	}

	@Test
	public void testCreateUserWithNoPassword() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user_test123");
		newUser.setGroupUuid(groupUuid());
		call(() -> client().createUser(newUser), BAD_REQUEST, "user_missing_password");
	}

	@Test
	public void testCreateUserWithEmptyPassword() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user_test123");
		newUser.setPassword("      ");
		newUser.setGroupUuid(groupUuid());
		call(() -> client().createUser(newUser), BAD_REQUEST, "user_missing_password");
	}

	@Test
	public void testCreateUserWithNoUsername() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setPassword("test123456");
		call(() -> client().createUser(newUser), BAD_REQUEST, "user_missing_username");
	}

	@Test
	public void testCreateUserWithNoParentGroup() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");

		UserResponse response = call(() -> client().createUser(newUser));
		assertEquals(0, response.getGroups().size());
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
		call(() -> client().createUser(newUser), NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testCreateUpdate() {
		try (Tx tx = tx()) {
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
		String groupUuid = tx(() -> group().getUuid());
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(groupUuid);

		UserResponse restUser = call(() -> client().createUser(request));

		try (Tx tx2 = tx()) {
			assertThat(restUser).matches(request);

			HibUser user = tx2.userDao().findByUuid(restUser.getUuid());
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

		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			roleDao.revokePermissions(role(), tx.data().permissionRoots().user(), CREATE_PERM);
			tx.success();
		}

		String userRootUuid = tx(() -> Tx.get().data().permissionRoots().user().getUuid());
		call(() -> client().createUser(request), FORBIDDEN, "error_missing_perm", userRootUuid, CREATE_PERM.getRestPerm().getName());
	}

	@Test
	@Override
	public void testCreateWithUuid() throws Exception {
		String groupUuid = groupUuid();
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(groupUuid);
		String uuid = UUIDUtil.randomUUID();

		UserResponse restUser = call(() -> client().createUser(uuid, request));
		assertThat(restUser).matches(request).hasUuid(uuid);
	}

	@Test
	@Override
	@Ignore("Not valid over dup UUIDs being allowed globally")
	public void testCreateWithDuplicateUuid() throws Exception {
		String groupUuid = groupUuid();
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(groupUuid);
		String uuid = projectUuid();

		call(() -> client().createUser(uuid, request), INTERNAL_SERVER_ERROR, "error_internal");
	}

	@Test
	@Override
	public void testPermissionResponse() {
		UserResponse user = client().findUsers().blockingGet().getData().get(0);
		assertThat(user.getPermissions()).hasNoPublishPermsSet();
	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;

		validateCreation(nJobs, i -> {
			UserCreateRequest request = new UserCreateRequest();
			request.setEmailAddress("n.user@spam.gentics.com");
			request.setFirstname("Joe");
			request.setLastname("Doe");
			request.setUsername("new_user_" + i);
			request.setPassword("test123456");
			request.setGroupUuid(group().getUuid());
			return client().createUser(request);
		});
	}

	/**
	 * Test whether the create rest call will create the correct permissions that allow removal of the object.
	 * 
	 * @throws Exception
	 */
	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		String groupUuid = tx(() -> group().getUuid());

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
		// String response = request(info, HttpMethod.POST, CURRENT_API_BASE_PATH + "/users/",
		// 400, "Bad Request", requestJson);
		// expectMessageResponse("error_parse_request_json_error", response);
	}

	// Delete tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		try (Tx tx = tx()) {
			waitForSearchIdleEvent();
			trackingSearchProvider().reset();
			UserCreateRequest newUser = new UserCreateRequest();
			newUser.setEmailAddress("n.user@spam.gentics.com");
			newUser.setFirstname("Joe");
			newUser.setLastname("Doe");
			newUser.setUsername("new_user");
			newUser.setPassword("test123456");
			newUser.setGroupUuid(group().getUuid());

			UserResponse restUser = call(() -> client().createUser(newUser));
			waitForSearchIdleEvent();

			assertThat(trackingSearchProvider()).hasStore(HibUser.composeIndexName(), restUser.getUuid());
			assertThat(trackingSearchProvider()).hasEvents(1, 0, 0, 0, 0);
			trackingSearchProvider().reset();

			assertTrue(restUser.getEnabled());
			String uuid = restUser.getUuid();

			expect(USER_DELETED).match(1, MeshElementEventModelImpl.class, event -> {
				assertThat(event).hasName("new_user").hasUuid(uuid);
			});

			call(() -> client().deleteUser(uuid));

			awaitEvents();
			waitForSearchIdleEvent();

			try (Tx tx2 = tx()) {
				HibUser loadedUser = tx2.userDao().findByUuid(uuid);
				assertNull("The user should have been deleted.", loadedUser);
			}

			// Load the user again and check whether it is disabled
			call(() -> client().findUserByUuid(uuid), NOT_FOUND, "object_not_found_for_uuid", uuid);

			assertThat(trackingSearchProvider()).hasDelete(HibUser.composeIndexName(), uuid);
			assertThat(trackingSearchProvider()).hasEvents(0, 0, 1, 0, 0);
		}

	}

	@Test
	@Override
	@Ignore("not yet supported")
	public void testDeleteByUUIDMultithreaded() throws InterruptedException {
		int nJobs = 3;
		String uuid = user().getUuid();
		validateDeletion(i -> client().deleteUser(uuid), nJobs);
	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();

			HibUser user = userDao.create("extraUser", user());
			userDao.addGroup(user, group());
			uuid = user.getUuid();
			assertNotNull(uuid);
			HibRole role = tx(() -> tx.roleDao().findByUuid(role().getUuid()));
			roleDao.grantPermissions(role, user, UPDATE_PERM, CREATE_PERM, READ_PERM);
			tx.success();
		}

		try (Tx tx = tx()) {
			call(() -> client().deleteUser(uuid), FORBIDDEN, "error_missing_perm", uuid, DELETE_PERM.getRestPerm().getName());
			assertNotNull("The user should not have been deleted", tx.userDao().findByUuid(uuid));
		}
	}

	@Test(expected = NullPointerException.class)
	public void testDeleteWithUuidNull() throws Exception {
		call(() -> client().deleteUser(null), NOT_FOUND, "object_not_found_for_uuid", "null");
	}

	@Test
	@Ignore
	public void testReadOwnCreatedUser() {

	}

	@Test
	public void testDeleteByUUID2() throws Exception {
		String uuid;
		try (Tx tx = tx()) {
			RoleDao roleDao = tx.roleDao();
			UserDao userDao = tx.userDao();
			HibRole role = roleDao.findByUuid(role().getUuid());

			String name = "extraUser";
			HibUser extraUser = userDao.create(name, user());
			userDao.addGroup(extraUser, group());
			uuid = extraUser.getUuid();
			roleDao.grantPermissions(role, extraUser, DELETE_PERM);
			assertTrue(roleDao.hasPermission(role, DELETE_PERM, extraUser));
			HibUser user = userDao.findByUuid(uuid);
			assertEquals(1, userDao.getGroups(user).count());
			assertTrue("The user should be enabled", user.isEnabled());
			tx.success();
		}

		call(() -> client().deleteUser(uuid));

		try (Tx tx = tx()) {
			assertNull("The user was not deleted.", tx.userDao().findByUuid(uuid));
		}
		// // Check whether the user was correctly disabled
		// try (NoTrx tx = tx()) {
		// User user2 = userRoot.findByUuidBlocking(uuid);
		// user2.reload();
		// assertNotNull(user2);
		// assertFalse("The user should have been disabled",
		// user2.isEnabled());
		// assertEquals(0, user2.getGroups().size());
		// }

	}

	@Test
	@Ignore("Not yet implemented")
	public void testDeleteOwnUser() {

		// String response = request(info, HttpMethod.DELETE, CURRENT_API_BASE_PATH + "/users/" +
		// user.getUuid(), 403, "Forbidden");
	}

	@Test
	public void testLocationHeader() {
		String name = "someUser";

		UserCreateRequest request = new UserCreateRequest();
		request.setUsername(name);
		request.setPassword("bla");

		MeshResponse<UserResponse> response = client().createUser(request).getResponse().blockingGet();
		try (Tx tx = tx()) {
			HibUser user = tx.userDao().findByUsername(name);
			assertNotNull("User should have been created.", user);
			assertEquals(CREATED.code(), response.getStatusCode());
			String location = response.getHeader(LOCATION.toString()).orElse(null);
			assertEquals("Location header value did not match", "http://localhost:" + port() + CURRENT_API_BASE_PATH + "/users/" + user.getUuid(),
				location);
		}
	}

	@Test
	public void testLocationWithHostHeader() {
		String name = "someUser";

		UserCreateRequest userRequest = new UserCreateRequest();
		userRequest.setUsername(name);
		userRequest.setPassword("bla");

		MeshRequest<UserResponse> request = client().createUser(userRequest);
		request.setHeader(HOST.toString(), "jotschi.de:" + port());
		MeshResponse<UserResponse> response = request.getResponse().blockingGet();
		try (Tx tx = tx()) {
			HibUser user = tx.userDao().findByUsername(name);
			assertNotNull("User should have been created.", user);
			assertEquals(CREATED.code(), response.getStatusCode());
			String location = response.getHeader(LOCATION.toString()).orElse(null);
			assertEquals("Location header value did not match", "http://jotschi.de:" + port() + CURRENT_API_BASE_PATH + "/users/" + user.getUuid(),
				location);
		}
	}

	@Test
	public void testUserRolesHash() {
		UserResponse response = call(() -> client().findUserByUuid(user().getUuid()));

		assertTrue("Roles hash should be in response", !StringUtils.isBlank(response.getRolesHash()));
	}
}
