package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.Group;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.GroupService;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.user.request.UserCreateRequest;
import com.gentics.cailun.core.rest.user.request.UserUpdateRequest;
import com.gentics.cailun.core.rest.user.response.UserResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class UserVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Autowired
	private UserService userService;

	@Autowired
	private GroupService groupService;

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
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]}";
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
		String json = "{\"message\":\"Missing permission on object {" + user.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadAllUsers() throws Exception {
		User user = info.getUser();

		User user2 = new User("testuser_2");
		user2 = userService.save(user2);
		info.getGroup().addUser(user2);

		User user3 = new User("testuser_3");
		user3 = userService.save(user3);
		info.getGroup().addUser(user3);
		groupService.save(info.getGroup());

		assertNotNull(userService.findByUsername(user.getUsername()));
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), user2, PermissionType.READ);
		// Don't grant permissions to user3

		String response = request(info, HttpMethod.GET, "/api/v1/users/", 200, "OK");
		String json = "{\"dummy_user\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"testuser_2\":{\"uuid\":\"uuid-value\",\"lastname\":null,\"firstname\":null,\"username\":\"testuser_2\",\"emailAddress\":null,\"groups\":[\"dummy_user_group\"]}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Update tests

	@Test
	public void testUpdateUser() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		UserResponse restUser = new UserResponse();
		restUser.setEmailAddress("t.stark@stark-industries.com");
		restUser.setFirstname("Tony Awesome");
		restUser.setLastname("Epic Stark");
		restUser.setUsername("dummy_user_changed");
		restUser.addGroup(info.getGroup().getName());

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"message\":\"OK\"}";
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
		restUser.addGroup(info.getGroup().getName());

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"message\":\"OK\"}";
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
		restUser.addGroup(info.getGroup().getName());

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 403, "Forbidden",
				new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"message\":\"Missing permission on object {" + user.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		assertTrue("The hash should not be updated.", oldHash == reloadedUser.getPasswordHash());
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
		String json = "{\"message\":\"Missing permission on object {" + user.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		assertTrue("The hash should not be updated.", oldHash == reloadedUser.getPasswordHash());
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

		UserResponse newUser = new UserResponse();
		newUser.setUsername("existing_username");
		newUser.addGroup(info.getGroup().getName());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 409, "Conflict", requestJson);
		String json = "{\"message\":\"A user with the username {existing_username} already exists. Please choose a different username.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testUpdateUserAndAddGroup() throws Exception {
		User user = info.getUser();

		// 1. Create a new group
		Group newGroup = new Group("additional_group");
		groupService.save(newGroup);
		info.getGroup().addGroup(newGroup);
		groupService.save(info.getGroup());

		// 2. Add needed permissions
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), newGroup, PermissionType.UPDATE);

		// 3. Setup rest model
		UserResponse restUser = new UserResponse();
		restUser.setEmailAddress("t.stark@stark-industries.com");
		restUser.setFirstname("Tony Awesome");
		restUser.setLastname("Epic Stark");
		restUser.setUsername("dummy_user_changed");

		// 4. Add both groups to the list of groups
		restUser.addGroup(info.getGroup().getName());
		restUser.addGroup(newGroup.getName());

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"message\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// Reload the group and verify that the user was added to the group
		newGroup = groupService.reload(newGroup);
		Assert.assertEquals("The group should now list one user.", 1, newGroup.getUsers().size());
		User userInGroup = newGroup.getUsers().iterator().next();
		assertTrue("The user is not part of the group", user.getId() == userInGroup.getId());

	}

	@Test
	public void testUpdateUserAndRemoveGroup() throws JsonGenerationException, JsonMappingException, IOException, Exception {
		User user = info.getUser();

		Group newGroup = new Group("additional_group");
		newGroup.addUser(user);
		newGroup = groupService.save(newGroup);
		info.getGroup().addGroup(newGroup);
		groupService.save(info.getGroup());
		Assert.assertEquals("The group should have one member", 1, newGroup.getUsers().size());

		roleService.addPermission(info.getRole(), newGroup, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		UserResponse restUser = new UserResponse();
		restUser.setEmailAddress("t.stark@stark-industries.com");
		restUser.setFirstname("Tony Awesome");
		restUser.setLastname("Epic Stark");
		restUser.setUsername("dummy_user_changed");
		// Only add the first group to the rest request. Thus the second one should be removed when allowed.
		restUser.addGroup(info.getGroup().getName());

		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", new ObjectMapper().writeValueAsString(restUser));
		String json = "{\"message\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		newGroup = groupService.reload(newGroup);
		Assert.assertEquals("The group should no longer have members", 0, newGroup.getUsers().size());

	}

	// Create tests

	@Test
	public void testCreateUserWithConflictingUsername() throws Exception {

		// Create an user with a conflicting username
		User conflictingUser = new User("existing_username");
		conflictingUser = userService.save(conflictingUser);
		info.getGroup().addUser(conflictingUser);

		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserResponse newUser = new UserResponse();
		newUser.setUsername("existing_username");
		newUser.addGroup(info.getGroup().getName());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"Either username or password was not specified.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testCreateUserWithNoPassword() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserResponse newUser = new UserResponse();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user_test123");
		newUser.addGroup(info.getGroup().getName());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"Either username or password was not specified.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
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
		String json = "{\"message\":\"Either username or password was not specified.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testCreateUserWithNoGroups() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 200, "OK", requestJson);
		String json = "{\"message\":\"No groups were specified. You need to specify at least one group for the user.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testCreateUserWithBogusGroups() throws Exception {
		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		UserCreateRequest newUser = new UserCreateRequest();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.addGroup("bogus");

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 200, "OK", requestJson);
		String json = "{\"message\":\"Could not find parent group {bogus}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

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
		newUser.addGroup(info.getGroup().getName());

		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"new_user\",\"emailAddress\":\"n.user@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

	}

	@Test
	public void testCreateUserWithBogusJson() throws Exception {

		String requestJson = "bogus text";
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 400, "Bad Request", requestJson);
		String json = "{\"message\":\"Could not parse request json.\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Delete tests

	@Test
	public void testDeleteUserByUUID() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 200, "OK");
		String json = "{\"message\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", userService.findByUUID(user.getUuid()));
	}

	@Test
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		User user = info.getUser();

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + user.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The user should not have been deleted", userService.findByUUID(user.getUuid()));
	}

	// User / Group Methods

	@Test
	public void testAddUserToGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUserToGroupWithNoPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUserToGroupWithBogusGroupId() {
		fail("Not yet implemented");
	}

	// Delete tests
	@Test
	public void testRemoveUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromGroupWithoutPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveSameUserFromGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromLastGroupWithPerm() {
		fail("Not yet implemented");
	}

	@Test
	public void testRemoveUserFromGroupWithBogusID() {
		fail("Not yet implemented");
	}

}
