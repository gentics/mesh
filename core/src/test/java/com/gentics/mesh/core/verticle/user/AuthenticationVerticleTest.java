package com.gentics.mesh.core.verticle.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import io.vertx.core.Future;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.AuthenticationVerticle;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AuthenticationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AuthenticationVerticle authenticationVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return authenticationVerticle;
	}

	// Read Tests

	@Test
	public void testRestClient() throws Exception {
		User user = user();

		MeshRestClient client = new MeshRestClient("localhost", getPort());
		client.setLogin(user.getUsername(), password());
		Future<GenericMessageResponse> future = client.login();
		latchFor(future);
		assertSuccess(future);
		GenericMessageResponse loginResponse = future.result();
		assertNotNull(loginResponse);
		assertEquals("OK", loginResponse.getMessage());

		Future<UserResponse> meResponse = client.me();
		latchFor(meResponse);
		UserResponse me = meResponse.result();
		assertFalse("The request failed.", meResponse.failed());

		assertNotNull(me);
		assertEquals(user.getUuid(), me.getUuid());
	}

}
