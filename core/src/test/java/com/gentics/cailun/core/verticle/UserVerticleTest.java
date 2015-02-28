package com.gentics.cailun.core.verticle;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.User;
import com.gentics.cailun.test.AbstractRestVerticleTest;

public class UserVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private UserVerticle userVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return userVerticle;
	}

	@Test
	public void testReadTagByUUID() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"}";
		User user = getDataProvider().getTestUser();
		assertNotNull("The UUID of the user must not be null.", user.getUuid());
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/users/" + user.getUuid(), 200, "OK");
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUsername() throws Exception {
		String json = "{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"}";
		User user = getDataProvider().getTestUser();
		assertNotNull("The username of the user must not be null.", user.getUsername());
		String response = testAuthenticatedRequest(HttpMethod.GET, "/api/v1/users/" + user.getUsername(), 200, "OK");
		System.out.println(response);
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	public void testReadWithNoPermission() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadAllUsers() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteUserByUsername() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteUserByUUID() {
		fail("Not yet implemented");
	}

	@Test
	public void testDeleteWithNoPermission() {
		fail("Not yet implemented");
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
