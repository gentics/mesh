package com.gentics.mesh.core.user;

import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.core.verticle.auth.AuthenticationVerticle;
import com.gentics.mesh.rest.MeshRestClient;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
import rx.Single;

public class AuthenticationVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private AuthenticationVerticle authenticationVerticle;

	@Override
	public List<AbstractSpringVerticle> getAdditionalVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(authenticationVerticle);
		return list;
	}

	// Read Tests

	@Test
	public void testRestClient() throws Exception {
		User user = user();
		String username = user.getUsername();
		String uuid = user.getUuid();

		MeshRestClient client = MeshRestClient.create("localhost", getPort(), Mesh.vertx(),
				Mesh.mesh().getOptions().getAuthenticationOptions().getAuthenticationMethod());
		client.setLogin(username, password());
		Single<GenericMessageResponse> future = client.login();

		GenericMessageResponse loginResponse = future.toBlocking().value();
		assertNotNull(loginResponse);
		assertEquals("OK", loginResponse.getMessage());

		Future<UserResponse> meResponse = client.me();
		latchFor(meResponse);
		UserResponse me = meResponse.result();
		assertFalse("The request failed.", meResponse.failed());

		assertNotNull(me);
		assertEquals(uuid, me.getUuid());

		Single<GenericMessageResponse> logoutFuture = client.logout();
		logoutFuture.toBlocking().value();

		// assertTrue(client.getCookie().startsWith(MeshOptions.MESH_SESSION_KEY + "=deleted; Max-Age=0;"));
		meResponse = client.me();
		latchFor(meResponse);
		expectException(meResponse, UNAUTHORIZED, "error_not_authorized");
	}

}
