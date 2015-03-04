package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.core.rest.response.RestUser;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class UserVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Autowired
	private UserService userService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return userVerticle;
	}

	@Test
	public void testReadTagByUUID() throws Exception {
		User user = info.getUser();
		assertNotNull("The UUID of the user must not be null.", user.getUuid());

		roleService.addPermission(info.getRole(), user, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUsername() throws Exception {
		User user = info.getUser();
		assertNotNull("The username of the user must not be null.", user.getUsername());
		assertNotNull(userService.findByUsername(user.getUsername()));

		roleService.addPermission(info.getRole(), user, PermissionType.READ);

		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUsername(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadByUsernameWithNoPermission() throws Exception {
		User user = info.getUser();
		assertNotNull("The username of the user must not be null.", user.getUsername());

		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), user, PermissionType.CREATE);
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUsername(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + user.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadAllUsers() throws Exception {
		User user = info.getUser();

		User user2 = new User("testuser_2");
		user2 = userService.save(user2);

		User user3 = new User("testuser_3");
		user3 = userService.save(user3);

		assertNotNull(userService.findByUsername(user.getUsername()));
		roleService.addPermission(info.getRole(), user, PermissionType.READ);
		roleService.addPermission(info.getRole(), user2, PermissionType.READ);
		// Don't grant permissions to user3

		String response = request(info, HttpMethod.GET, "/api/v1/users/", 200, "OK");
		String json = "{\"dummy_user\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\"},\"testuser_2\":{\"uuid\":\"uuid-value\",\"lastname\":null,\"firstname\":null,\"username\":\"testuser_2\",\"emailAddress\":null}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testDeleteUserByUsername() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUsername(), 200, "OK");
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", userService.findByUUID(user.getUuid()));
	}

	@Test
	public void testDeleteUserByUUID() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 200, "OK");
		String json = "{\"msg\":\"OK\"}";
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

	@Test
	public void testUpdateUser() throws Exception {
		User user = info.getUser();

		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		String requestJson = "{\"uuid\":\""
				+ user.getUuid()
				+ "\",\"lastname\":\"Epic Stark\",\"firstname\":\"Tony Awesome\",\"username\":\"dummy_user_changed\",\"emailAddress\":\"t.stark@stark-industries.com\"}";
		String response = request(info, HttpMethod.PUT, "/api/v1/users/" + user.getUuid(), 200, "OK", requestJson);
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		User reloadedUser = userService.findByUUID(user.getUuid());
		Assert.assertEquals("Epic Stark", reloadedUser.getLastname());
		Assert.assertEquals("Tony Awesome", reloadedUser.getFirstname());
		Assert.assertEquals("t.stark@stark-industries.com", reloadedUser.getEmailAddress());
		Assert.assertEquals("dummy_user_changed", reloadedUser.getUsername());
	}

	@Test
	public void testUpdateUserWithInvalidData() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdatePassword() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateUserWithNoPermission() throws Exception {

	}

	@Test
	public void testCreateUser() throws Exception {

		// Add update permission to group in order to create the user in that group
		roleService.addPermission(info.getRole(), info.getGroup(), PermissionType.UPDATE);

		RestUser newUser = new RestUser();
		newUser.setEmailAddress("n.user@spam.gentics.com");
		newUser.setFirstname("Joe");
		newUser.setLastname("Doe");
		newUser.setUsername("new_user");
		newUser.setPassword("test123456");
		newUser.addGroup(info.getGroup().getName());
		
		String requestJson = new ObjectMapper().writeValueAsString(newUser);
		String response = request(info, HttpMethod.POST, "/api/v1/users/", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"new_user\",\"emailAddress\":\"n.user@spam.gentics.com\",\"password\":null,\"groups\":[\"dummy_user_group\"]}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// TODO marshall json to restmodel and inspect it

	}

}
