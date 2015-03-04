package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

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

	@Override
	public AbstractRestVerticle getVerticle() {
		return userVerticle;
	}

	@Test
	public void testReadTagByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"}";
		User user = info.getUser();
		assertNotNull("The UUID of the user must not be null.", user.getUuid());
		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUuid(), 200, "OK");
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUsername() throws Exception {
		UserInfo info = data().getUserInfo();

		User user = info.getUser();
		assertNotNull("The username of the user must not be null.", user.getUsername());
		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUsername(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	public void testReadByUsernameWithNoPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		User user = info.getUser();
		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), user, PermissionType.CREATE);
		roleService.addPermission(info.getRole(), user, PermissionType.UPDATE);

		assertNotNull("The username of the user must not be null.", user.getUsername());
		String response = request(info, HttpMethod.GET, "/api/v1/users/" + user.getUsername(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadAllUsers() throws Exception {
		UserInfo info = data().getUserInfo();
		String json = "";
		String response = request(info, HttpMethod.GET, "/api/v1/users/", 200, "OK");
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testDeleteUserByUsername() throws Exception {

		UserInfo info = data().getUserInfo();
		User user = info.getUser();
		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUsername(), 200, "OK");
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", userService.findByUUID(user.getUuid()));
	}

	@Test
	public void testDeleteUserByUUID() throws Exception {

		UserInfo info = data().getUserInfo();
		User user = info.getUser();
		roleService.addPermission(info.getRole(), user, PermissionType.DELETE);

		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 200, "OK");
		String json = "{\"msg\":\"OK\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The user should have been deleted", userService.findByUUID(user.getUuid()));
	}

	@Test
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		UserInfo info = data().getUserInfo();
		User user = info.getUser();
		String json = "{\"message\":\"Missing permission on user {" + user.getUuid() + "}\"}";
		String response = request(info, HttpMethod.DELETE, "/api/v1/users/" + user.getUuid(), 403, "Forbidden");
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
