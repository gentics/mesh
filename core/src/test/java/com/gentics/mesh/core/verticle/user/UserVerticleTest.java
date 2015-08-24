package com.gentics.mesh.core.verticle.user;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.root.UserRoot;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.user.NodeReference;
import com.gentics.mesh.core.rest.user.UserCreateRequest;
import com.gentics.mesh.core.rest.user.UserListResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.core.verticle.UserVerticle;
import com.gentics.mesh.demo.DemoDataProvider;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;

import io.vertx.core.Future;

public class UserVerticleTest extends AbstractBasicCrudVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Override
	public List<AbstractWebVerticle> getVertices() {
		List<AbstractWebVerticle> list = new ArrayList<>();
		list.add(userVerticle);
		return list;
	}

	// Read Tests

	@Test
	@Override
	public void testReadByUUID() throws Exception {
		try (Trx tx = new Trx(db)) {
			User user = user();
			assertNotNull("The UUID of the user must not be null.", user.getUuid());

			Future<UserResponse> future = getClient().findUserByUuid(user.getUuid());
			latchFor(future);
			assertSuccess(future);
			UserResponse restUser = future.result();

			test.assertUser(user, restUser);
			// TODO assert groups
			// TODO assert perms
		}
	}

	@Test
	@Override
	public void testReadByUuidMultithreaded() throws InterruptedException {
		int nJobs = 10;
		String uuid = user().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findUserByUuid(uuid));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws InterruptedException {
		int nJobs = 200;
		Set<Future<UserResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findUserByUuid(user().getUuid()));
		}
		for (Future<UserResponse> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		String uuid;
		try (Trx tx = new Trx(db)) {
			User user = user();
			uuid = user.getUuid();
			assertNotNull("The username of the user must not be null.", user.getUsername());
			role().revokePermissions(user, READ_PERM);
			tx.success();
		}

		Future<UserResponse> future = getClient().findUserByUuid(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	@Override
	public void testReadMultiple() throws Exception {

		String username = "testuser_3";
		try (Trx tx = new Trx(db)) {
			UserRoot root = meshRoot().getUserRoot();
			User user3 = root.create(username, group(), user());
			user3.setLastname("should_not_be_listed");
			user3.setFirstname("should_not_be_listed");
			user3.setEmailAddress("should_not_be_listed");
			tx.success();
		}

		// Test default paging parameters
		Future<UserListResponse> future = getClient().findUsers();
		latchFor(future);
		assertSuccess(future);
		UserListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals(14, restResponse.getData().size());

		int perPage = 2;
		int totalUsers = users().size();
		int totalPages = ((int) Math.ceil(totalUsers / (double) perPage));
		future = getClient().findUsers(new PagingInfo(3, perPage));
		latchFor(future);
		assertSuccess(future);
		restResponse = future.result();

		assertEquals("The page did not contain the expected amount of items", perPage, restResponse.getData().size());
		assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The amount of pages did not match. We have {" + totalUsers + "} users in the system and use a paging of {" + perPage + "}",
				totalPages, restResponse.getMetainfo().getPageCount());
		assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		assertEquals("The total amount of items does not match the expected one", totalUsers, restResponse.getMetainfo().getTotalCount());

		perPage = 11;

		List<UserResponse> allUsers = new ArrayList<>();
		for (int page = 1; page < totalPages; page++) {
			Future<UserListResponse> pageFuture = getClient().findUsers(new PagingInfo(page, perPage));
			latchFor(pageFuture);
			assertSuccess(pageFuture);
			restResponse = pageFuture.result();
			allUsers.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalUsers, allUsers.size());

		// Verify that user3 is not part of the response
		final String extra3Username = username;
		List<UserResponse> filteredUserList = allUsers.parallelStream().filter(restUser -> restUser.getUsername().equals(extra3Username))
				.collect(Collectors.toList());
		assertTrue("User 3 should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		future = getClient().findUsers(new PagingInfo(-1, perPage));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findUsers(new PagingInfo(1, 0));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findUsers(new PagingInfo(1, -1));
		latchFor(future);
		expectException(future, BAD_REQUEST, "error_invalid_paging_parameters");

		future = getClient().findUsers(new PagingInfo(4242, 25));
		latchFor(future);
		assertSuccess(future);

		assertEquals(0, future.result().getData().size());
		assertEquals(4242, future.result().getMetainfo().getCurrentPage());
		assertEquals(1, future.result().getMetainfo().getPageCount());
		assertEquals(14, future.result().getMetainfo().getTotalCount());
		assertEquals(25, future.result().getMetainfo().getPerPage());

	}

	// Update tests

	@Test
	@Override
	public void testUpdateMultithreaded() throws InterruptedException {
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");
		updateRequest.setUsername("dummy_user_changed");

		int nJobs = 5;
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().updateUser(user().getUuid(), updateRequest));
		}
		validateSet(set, barrier);

	}

	@Test
	@Override
	public void testUpdate() throws Exception {

		User user = user();
		String username = user.getUsername();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");
		updateRequest.setUsername("dummy_user_changed");

		Future<UserResponse> future = getClient().updateUser(user.getUuid(), updateRequest);
		latchFor(future);
		assertSuccess(future);
		UserResponse restUser = future.result();
		test.assertUser(updateRequest, restUser);
		try (Trx tx = new Trx(db)) {
			assertNull("The user node should have been updated and thus no user should be found.", boot.userRoot().findByUsername(username));
			User reloadedUser = boot.userRoot().findByUsername("dummy_user_changed");
			assertNotNull(reloadedUser);
			assertEquals("Epic Stark", reloadedUser.getLastname());
			assertEquals("Tony Awesome", reloadedUser.getFirstname());
			assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
			assertEquals("dummy_user_changed", reloadedUser.getUsername());
		}
	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		UserUpdateRequest request = new UserUpdateRequest();
		request.setUsername("New Name");

		Future<UserResponse> future = getClient().updateUser("bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	public void testUpdateUserAndSetNodeReference() throws Exception {
		String nodeUuid = folder("2015").getUuid();
		User user = user();
		String username = user.getUsername();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setEmailAddress("t.stark@stark-industries.com");
		updateRequest.setFirstname("Tony Awesome");
		updateRequest.setLastname("Epic Stark");
		updateRequest.setUsername("dummy_user_changed");

		NodeReference userNodeReference = new NodeReference();
		userNodeReference.setProjectName(DemoDataProvider.PROJECT_NAME);
		userNodeReference.setUuid(nodeUuid);
		updateRequest.setNodeReference(userNodeReference);

		Future<UserResponse> future = getClient().updateUser(user.getUuid(), updateRequest);
		latchFor(future);
		assertSuccess(future);
		UserResponse restUser = future.result();
		assertNotNull(restUser.getNodeReference());
		assertEquals(DemoDataProvider.PROJECT_NAME, restUser.getNodeReference().getProjectName());
		assertEquals(nodeUuid, restUser.getNodeReference().getUuid());

		test.assertUser(updateRequest, restUser);
		try (Trx tx = new Trx(db)) {
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

		Node node = folder("2015");

		NodeReference reference = new NodeReference();
		reference.setProjectName(DemoDataProvider.PROJECT_NAME);
		reference.setUuid(node.getUuid());

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(group().getUuid());
		newUser.setPassword("test1234");
		newUser.setNodeReference(reference);

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		assertSuccess(future);
		UserResponse response = future.result();
		assertNotNull(response.getNodeReference());
		assertNotNull(response.getNodeReference().getProjectName());
		assertNotNull(response.getNodeReference().getUuid());

	}

	@Test
	public void testCreateUserWithBogusProjectNameInNodeReference() {

		Node node = folder("2015");

		NodeReference reference = new NodeReference();
		reference.setProjectName("bogus_name");
		reference.setUuid(node.getUuid());

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(group().getUuid());
		newUser.setPassword("test1234");
		newUser.setNodeReference(reference);

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, BAD_REQUEST, "project_not_found", "bogus_name");
	}

	@Test
	public void testCreateUserWithBogusUuidInNodeReference() {

		NodeReference reference = new NodeReference();
		reference.setProjectName(DemoDataProvider.PROJECT_NAME);
		reference.setUuid("bogus_uuid");

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(group().getUuid());
		newUser.setPassword("test1234");
		newUser.setNodeReference(reference);

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus_uuid");
	}

	@Test
	public void testCreateUserWithMissingProjectNameInNodeReference() {

		NodeReference reference = new NodeReference();
		reference.setUuid("bogus_uuid");

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(group().getUuid());
		newUser.setPassword("test1234");
		newUser.setNodeReference(reference);

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, BAD_REQUEST, "user_incomplete_node_reference");
	}

	@Test
	public void testCreateUserWithMissingUuidNameInNodeReference() {

		NodeReference reference = new NodeReference();
		reference.setProjectName(DemoDataProvider.PROJECT_NAME);
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("new_user");
		newUser.setGroupUuid(group().getUuid());
		newUser.setPassword("test1234");
		newUser.setNodeReference(reference);

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, BAD_REQUEST, "user_incomplete_node_reference");
	}

	@Test
	public void testUpdatePassword() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		User user = user();
		String oldHash = user.getPasswordHash();
		UserUpdateRequest updateRequest = new UserUpdateRequest();
		updateRequest.setPassword("new_password");

		Future<UserResponse> future = getClient().updateUser(user.getUuid(), updateRequest);
		latchFor(future);
		assertSuccess(future);
		UserResponse restUser = future.result();

		test.assertUser(updateRequest, restUser);

		try (Trx tx = new Trx(db)) {
			User reloadedUser = boot.userRoot().findByUsername(user.getUsername());
			assertNotEquals("The hash should be different and thus the password updated.", oldHash, reloadedUser.getPasswordHash());
			assertEquals(user.getUsername(), reloadedUser.getUsername());
			assertEquals(user.getFirstname(), reloadedUser.getFirstname());
			assertEquals(user.getLastname(), reloadedUser.getLastname());
			assertEquals(user.getEmailAddress(), reloadedUser.getEmailAddress());
		}
	}

	@Test
	public void testUpdatePasswordWithNoPermission() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		User user = user();
		String oldHash = user.getPasswordHash();
		try (Trx tx = new Trx(db)) {
			role().revokePermissions(user, UPDATE_PERM);
			tx.success();
		}

		UserUpdateRequest request = new UserUpdateRequest();
		request.setPassword("new_password");
		Future<UserResponse> future = getClient().updateUser(user.getUuid(), request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", user.getUuid());

		try (Trx tx = new Trx(db)) {
			boot.userRoot().findByUuid(user.getUuid(), rh -> {
				User reloadedUser = rh.result();
				assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
			});
		}
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() throws Exception {
		User user = user();
		String oldHash = user.getPasswordHash();
		try (Trx tx = new Trx(db)) {
			role().revokePermissions(user, UPDATE_PERM);
			tx.success();
		}
		UserUpdateRequest updatedUser = new UserUpdateRequest();
		updatedUser.setEmailAddress("n.user@spam.gentics.com");
		updatedUser.setFirstname("Joe");
		updatedUser.setLastname("Doe");
		updatedUser.setUsername("new_user");
		// updatedUser.addGroup(group().getName());

		Future<UserResponse> future = getClient().updateUser(user.getUuid(), updatedUser);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", user.getUuid());
		try (Trx tx = new Trx(db)) {
			boot.userRoot().findByUuid(user.getUuid(), rh -> {
				User reloadedUser = rh.result();
				assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
				assertEquals("The firstname should not be updated.", user.getFirstname(), reloadedUser.getFirstname());
				assertEquals("The firstname should not be updated.", user.getLastname(), reloadedUser.getLastname());
			});
		}
	}

	@Test
	public void testUpdateUserWithConflictingUsername() throws Exception {

		// Create an user with a conflicting username
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			userRoot.create("existing_username", group(), user());
			tx.success();
		}

		UserUpdateRequest request = new UserUpdateRequest();
		request.setUsername("existing_username");

		Future<UserResponse> future = getClient().updateUser(user().getUuid(), request);
		latchFor(future);
		expectException(future, CONFLICT, "user_conflicting_username");

	}

	@Test
	public void testUpdateUserWithSameUsername() throws Exception {
		User user = user();

		UserUpdateRequest request = new UserUpdateRequest();
		request.setUsername(user.getUsername());

		Future<UserResponse> future = getClient().updateUser(user.getUuid(), request);
		latchFor(future);
		assertSuccess(future);
	}

	// Create tests

	@Test
	public void testCreateUserWithConflictingUsername() throws Exception {
		try (Trx tx = new Trx(db)) {

			// Create an user with a conflicting username
			UserRoot userRoot = meshRoot().getUserRoot();
			User conflictingUser = userRoot.create("existing_username", group(), user());
			// Add update permission to group in order to create the user in that group
			role().grantPermissions(group(), CREATE_PERM);
			tx.success();
		}
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("existing_username");
		newUser.setGroupUuid(group().getUuid());
		newUser.setPassword("test1234");

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, CONFLICT, "user_conflicting_username");

	}

	@Test
	public void testCreateUserWithNoPassword() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user_test123");
		newUser.setGroupUuid(group().getUuid());

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, BAD_REQUEST, "user_missing_password");
	}

	@Test
	public void testCreateUserWithNoUsername() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setPassword("test123456");

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, BAD_REQUEST, "user_missing_username");
	}

	@Test
	public void testCreateUserWithNoParentGroup() throws Exception {
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, BAD_REQUEST, "user_missing_parentgroup_field");

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

		Future<UserResponse> future = getClient().createUser(newUser);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");

	}

	@Test
	@Override
	public void testCreate() throws Exception {
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(group().getUuid());

		Future<UserResponse> future = getClient().createUser(request);
		latchFor(future);
		assertSuccess(future);
		UserResponse restUser = future.result();
		test.assertUser(request, restUser);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			boot.userRoot().findByUuid(restUser.getUuid(), rh -> {
				User user = rh.result();
				test.assertUser(user, restUser);
				latch.countDown();
			});
			failingLatch(latch);
		}

	}

	@Test
	@Override
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
		UserCreateRequest request = new UserCreateRequest();
		request.setEmailAddress("n.user@spam.gentics.com");
		request.setFirstname("Joe");
		request.setLastname("Doe");
		request.setUsername("new_user");
		request.setPassword("test123456");
		request.setGroupUuid(group().getUuid());

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().createUser(request));
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
		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.setGroupUuid(group().getUuid());

		Future<UserResponse> createFuture = getClient().createUser(newUser);
		latchFor(createFuture);
		assertSuccess(createFuture);
		UserResponse restUser = createFuture.result();

		try (Trx tx = new Trx(db)) {
			test.assertUser(newUser, restUser);
		}

		Future<UserResponse> readFuture = getClient().findUserByUuid(restUser.getUuid());
		latchFor(readFuture);
		assertSuccess(readFuture);

		Future<GenericMessageResponse> deleteFuture = getClient().deleteUser(restUser.getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);
		expectMessageResponse("user_deleted", deleteFuture, restUser.getUuid() + "/" + restUser.getUsername());
	}

	@Test
	@Ignore("this can't be tested using the rest client")
	public void testCreateUserWithBogusJson() throws Exception {

		// String requestJson = "bogus text";
		// Future<UserResponse> future = getClient().createUser(userCreateRequest)
		// String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		// expectMessageResponse("error_parse_request_json_error", response);
	}

	// Delete tests

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		User user = user();
		assertTrue(user.isEnabled());
		String uuid = user.getUuid();
		String name = user.getName();
		Future<GenericMessageResponse> future = getClient().deleteUser(uuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("user_deleted", future, uuid + "/" + name);
		try (Trx tx = new Trx(db)) {
			boot.userRoot().findByUuid(uuid, rh -> {
				User loadedUser = rh.result();
				assertNotNull("The user should not have been deleted. It should just be disabled.", loadedUser);
				assertFalse(loadedUser.isEnabled());
			});
		}
	}

	@Test
	@Override
	public void testDeleteByUUIDMultithreaded() throws InterruptedException {
		int nJobs = 3;
		String uuid = user().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<GenericMessageResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().deleteUser(uuid));
		}
		validateDeletion(set, barrier);

	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {

		String uuid;
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User user = userRoot.create("extraUser", group(), user());
			uuid = user.getUuid();
			assertNotNull(uuid);
			role().grantPermissions(user, UPDATE_PERM);
			role().grantPermissions(user, CREATE_PERM);
			role().grantPermissions(user, READ_PERM);
			tx.success();
		}

		Future<GenericMessageResponse> future = getClient().deleteUser(uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			userRoot.findByUuid(uuid, rh -> {
				assertNotNull("The user should not have been deleted", rh.result());
			});
		}
	}

	@Test(expected = NullPointerException.class)
	public void testDeleteWithUuidNull() throws Exception {
		Future<GenericMessageResponse> future = getClient().deleteUser(null);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "null");
	}

	@Test
	public void testDeleteByUUID2() throws Exception {
		String uuid;
		String name = "extraUser";
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			User extraUser = userRoot.create(name, group(), user());
			uuid = extraUser.getUuid();
			role().grantPermissions(extraUser, DELETE_PERM);
			assertNotNull(extraUser.getUuid());
			tx.success();
		}
		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();
			userRoot.findByUuid(uuid, rh -> {
				User user = rh.result();
				assertEquals(1, user.getGroups().size());
			});
		}

		Future<GenericMessageResponse> future = getClient().deleteUser(uuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("user_deleted", future, uuid + "/" + name);

		try (Trx tx = new Trx(db)) {
			UserRoot userRoot = meshRoot().getUserRoot();

			// Check whether the user was correctly disabled
			userRoot.findByUuid(uuid, rh -> {
				User user2 = rh.result();
				assertNotNull(user2);
				assertEquals(0, user2.getGroups().size());
				assertFalse("The user should have been disabled", user2.isEnabled());
			});
		}
	}

	@Test
	@Ignore("Not yet implemented")
	public void testDeleteOwnUser() {

		// String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 403, "Forbidden");
	}
}
