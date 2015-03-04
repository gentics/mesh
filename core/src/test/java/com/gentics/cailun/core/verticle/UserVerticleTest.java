package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.core.data.service.UserService;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.UserInfo;

public class UserVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Autowired
	private UserService userService;

	private UserInfo info;

	@Override
	public AbstractRestVerticle getVerticle() {
		return userVerticle;
	}

	@Before
	public void setup() {
		info = data().getUserInfo();
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
	public void testUpdateUser() {
		fail("Not yet implemented");
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
	public void testUpdateUserWithNoPermission() {
		fail("Not yet implemented");
	}

}
