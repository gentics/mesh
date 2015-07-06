package com.gentics.mesh.core.verticle.user;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.AuthenticationVerticle;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class AuthenticationVerticleRestClientTest extends AbstractRestVerticleTest {

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
		AtomicBoolean finished = new AtomicBoolean(false);
		CountDownLatch latch = new CountDownLatch(1);
		MeshRestClient client = new MeshRestClient("localhost", getPort());
		Future<UserResponse> response = client.login(user.getUsername(), info.getPassword());
		response.setHandler(rh -> {
			if (rh.failed()) {
				rh.cause().printStackTrace();
			}
			UserResponse userResponse = rh.result();
			assertNotNull(userResponse);
			assertEquals(user.getUuid(), userResponse.getUuid());
			finished.set(true);
			latch.countDown();
		});
		latch.await(10, TimeUnit.SECONDS);
		assertTrue(finished.get());
	}

}
