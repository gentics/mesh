package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.I18NService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserListResponse;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class UserVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Autowired
	private UserService userService;

	@Autowired
	private GroupService groupService;

	@Autowired
	private I18NService i18n;

	@Override
	public AbstractRestVerticle getVerticle() {
		return userVerticle;
	}

	// Read Tests

	@Test
	public void testReadTagByUUID() throws Exception {
		User user = info.getUser();
		assertNotNull("The UUID of the user must not be null.", user.getUuid());

		roleService.addPermission(info.getRole(), user, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"],\"perms\":[]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadByUUIDWithNoPermission() throws Exception {
		User user = info.getUser();
		assertNotNull("The username of the user must not be null.", user.getUsername());

		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), user, PermissionType.CREATE);
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, user.getUuid());
	}

	@Test
	public void testReadAllUsers() throws Exception {
		User user = info.getUser();
		roleService.addPermission(info.getRole(), user, PermissionType.READ);

		final int nUsers = 142;
		for (int i = 0; i < nUsers; i++) {
			User extraUser = new User("extra_user_" + i);
			extraUser.setLastname("A" + i);
			extraUser.setFirstname("A" + i);
			extraUser.setEmailAddress("test" + i);
			extraUser = userService.save(extraUser);
			extraUser.getGroups().add(info.getGroup());
			// info.getGroup().addUser(extraUser);
			roleService.addPermission(info.getRole(), extraUser, PermissionType.READ);
		}
		User user3 = new User("testuser_3");
		user3.setLastname("should_not_be_listed");
		user3.setFirstname("should_not_be_listed");
		user3.setEmailAddress("should_not_be_listed");
		user3 = userService.save(user3);
		info.getGroup().addUser(user3);
		groupService.save(info.getGroup());

		// Don't grant permissions to user3

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/users/", 200, "OK");
		UserListResponse restResponse = JsonUtils.readValue(response, UserListResponse.class);
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/users/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, UserListResponse.class);
		Assert.assertEquals(perPage, restResponse.getData().size());

		// Extrausers + user for login
		int totalUsers = nUsers + 1;
		int totalPages = (int) Math.ceil(totalUsers / perPage);
		Assert.assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		Assert.assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(totalUsers, restResponse.getMetainfo().getTotalCount());

		List<UserResponse> allUsers = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/users/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, UserListResponse.class);
			allUsers.addAll(restResponse.getData());
		}
		Assert.assertEquals("Somehow not all users were loaded when loading all pages.", totalUsers, allUsers.size());

		// Verify that user3 is not part of the response
		final String extra3Username = user3.getUsername();
		List<UserResponse> filteredUserList = allUsers.parallelStream().filter(restUser -> restUser.getUsername().equals(extra3Username))
				.collect(Collectors.toList());
		assertTrue("User 3 should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/users/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/users/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/users/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/users/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":6,\"total_count\":143}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);

	}

	// Update tests

	@Test
	public void testUpdateUser() throws Exception {
		User user = info.getUser();
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		UserUpdateRequest restUser = new UserUpdateRequest();
		restUser.setUuid(user.getUuid());
		restUser.setEmailAddress("t.stark@stark-industries.com");
		restUser.setFirstname("Tony Awesome");
		restUser.setLastname("Epic Stark");
		restUser.setUsername("dummy_user_changed");

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", JsonUtils.toJson(restUser));
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Epic Stark\",\"firstname\":\"Tony Awesome\",\"username\":\"dummy_user_changed\",\"emailAddress\":\"t.stark@stark-industries.com\",\"groups\":[\"dummy_user_group\"],\"perms\":[]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		Assert.assertEquals("Epic Stark", reloadedUser.getLastname());
		Assert.assertEquals("Tony Awesome", reloadedUser.getFirstname());
		Assert.assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
		Assert.assertEquals("dummy_user_changed", reloadedUser.getUsername());
	}

	@Test
	public void testUpdatePassword() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		User user = info.getUser();
		String oldHash = user.getPasswordHash();
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		UserUpdateRequest restUser = new UserUpdateRequest();
		restUser.setPassword("new_password");

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"],\"perms\":[]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		assertTrue("The hash should be different and thus the password updated.", oldHash != reloadedUser.getPasswordHash());
		Assert.assertEquals(user.getUsername(), reloadedUser.getUsername());
		Assert.assertEquals(user.getFirstname(), reloadedUser.getFirstname());
		Assert.assertEquals(user.getLastname(), reloadedUser.getLastname());
		Assert.assertEquals(user.getEmailAddress(), reloadedUser.getEmailAddress());
	}

	@Test
	public void testUpdatePasswordWithNoPermission() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		User user = info.getUser();
		String oldHash = user.getPasswordHash();
		roleService.addPermission(info.getRole(), user, PermissionType.READ);

		UserUpdateRequest restUser = new UserUpdateRequest();
		restUser.setPassword("new_password");

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 403, "Forbidden",
				new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"message\":\"Missing permissions on object \\\"" + user.getUuid() + "\\\"\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
	}

	@Test
	public void testUpdateUserWithNoPermission() throws Exception {
		User user = info.getUser();
		String oldHash = user.getPasswordHash();
		roleService.addPermission(info.getRole(), user, PermissionType.READ);

		UserResponse updatedUser = new UserResponse();
		updatedUser.setEmailAddress("n.user@spam.gentics.com");
		updatedUser.setFirstname("Joe");
		updatedUser.setLastname("Doe");
		updatedUser.setUsername("new_user");
		updatedUser.addGroup(info.getGroup().getName());

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 403, "Forbidden",
				new ObjectMapper().writeValueAsString(updatedUser));

		String json = "{\"message\":\"Missing permissions on object \\\"" + user.getUuid() + "\\\"\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		assertTrue("The hash should not be updated.", oldHash.equals(reloadedUser.getPasswordHash()));
		Assert.assertEquals("The firstname should not be updated.", user.getFirstname(), reloadedUser.getFirstname());
		Assert.assertEquals("The firstname should not be updated.", user.getLastname(), reloadedUser.getLastname());
	}

	@Test
	public void testUpdateUserWithConflictingUsername() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		// Create an user with a conflicting username
		User conflictingUser = new User("existing_username");
		conflictingUser = userService.save(conflictingUser);
		info.getGroup().addUser(conflictingUser);

		UserUpdateRequest newUser = new UserUpdateRequest();
		newUser.setUsername("existing_username");
		newUser.setUuid(user.getUuid());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 409, "Conflict", requestJson);
		expectMessageResponse("user_conflicting_username", response);

	}

	// Create tests

	@Test
	public void testCreateUserWithConflictingUsername() throws Exception {

		// Create an user with a conflicting username
		User conflictingUser = new User("existing_username");
		conflictingUser = userService.save(conflictingUser);
		info.getGroup().addUser(conflictingUser);

		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setUsername("existing_username");
		newUser.setGroupUuid(info.getGroup().getUuid());
		newUser.setPassword("test1234");

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 409, "Conflict", requestJson);
		expectMessageResponse("user_conflicting_username", response);

	}

	@Test
	public void testCreateUserWithNoPassword() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user_test123");
		newUser.setGroupUuid(info.getGroup().getUuid());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		expectMessageResponse("user_missing_password", response);
	}

	@Test
	public void testCreateUserWithNoUsername() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setPassword("test123456");

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		expectMessageResponse("user_missing_username", response);
	}

	@Test
	public void testCreateUserWithNoParentGroup() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		expectMessageResponse("user_missing_parentgroup_field", response);

	}

	@Test
	public void testCreateUserWithBogusParentGroup() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.setGroupUuid("bogus");

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 404, "Not Found", requestJson);
		expectMessageResponse("object_not_found_for_uuid", response, "bogus");

	}

	@Test
	public void testCreateUser() throws Exception {

		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.setGroupUuid(info.getGroup().getUuid());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"new_user\",\"emailAddress\":\"n.user@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	/**
	 * Test whether the create rest call will create the correct permissions that allow removal of the object.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testCreateDeleteUser() throws Exception {

		// Add create permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.CREATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.setGroupUuid(info.getGroup().getUuid());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"new_user\",\"emailAddress\":\"n.user@spam.gentics.com\",\"groups\":[\"dummy_user_group\"],\"perms\":[]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		UserResponse restUser = JsonUtils.readValue(response, UserResponse.class);

		response = request(info, HttpMethod.DELETE, "/api/v1/users/" + restUser.getUUID(), 200, "OK");
		expectMessageResponse("user_deleted", response, restUser.getUUID());

	}

	@Test
	public void testCreateUserWithBogusJson() throws Exception {

		String requestJson = "bogus text";
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);

		expectMessageResponse("error_parse_request_json_error", response);
	}

	// Delete tests

	@Test
	public void testDeleteUserByUUID() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 200, "OK");
		expectMessageResponse("user_deleted", response, user.getUuid());
		assertNull("The user should have been deleted", userService.findByUUID(user.getUuid()));
	}

	@Test
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		User user = info.getUser();
		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, user.getUuid());
		assertNotNull("The user should not have been deleted", userService.findByUUID(user.getUuid()));
	}

}
