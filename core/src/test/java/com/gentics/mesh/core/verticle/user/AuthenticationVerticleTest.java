package com.gentics.mesh.core.verticle.user;

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.vertx.core.Future;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.AuthenticationVerticle;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AuthenticationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AuthenticationVerticle authenticationVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return authenticationVerticle;
	}

	// Read Tests

	@Test
	public void testRestClient() throws Exception {
		User user = info.getUser();

		MeshRestClient client = new MeshRestClient("localhost", getPort());
		client.setLogin(user.getUsername(), info.getPassword());
		Future<UserResponse> response = client.login();
		latchFor(response);
		UserResponse loginResponse = response.result();
		assertNotNull(loginResponse);
		assertEquals(user.getUuid(), loginResponse.getUuid());

		Future<UserResponse> meResponse = client.me();
		latchFor(meResponse);
		UserResponse me = meResponse.result();
		assertFalse("The request failed.", meResponse.failed());

		assertNotNull(me);
		assertEquals(user.getUuid(), me.getUuid());
	}


}
