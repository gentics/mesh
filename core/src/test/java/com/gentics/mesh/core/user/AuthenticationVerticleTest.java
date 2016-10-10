package com.gentics.mesh.core.user;

import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import rx.Single;

public class AuthenticationVerticleTest extends AbstractRestVerticleTest {

	// Read Tests

	@Test
	public void testRestClient() throws Exception {
		try (NoTx noTrx = db.noTx()) {
			User user = user();
			String username = user.getUsername();
			String uuid = user.getUuid();

			MeshRestClient client = MeshRestClient.create("localhost", getPort(), Mesh.vertx());
			client.setLogin(username, password());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.toBlocking().value();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			MeshResponse<UserResponse> meResponse = client.me().invoke();
			latchFor(meResponse);
			UserResponse me = meResponse.result();
			assertFalse("The request failed.", meResponse.failed());

			assertNotNull(me);
			assertEquals(uuid, me.getUuid());

			Single<GenericMessageResponse> logoutFuture = client.logout();
			logoutFuture.toBlocking().value();

			meResponse = client.me().invoke();
			latchFor(meResponse);
			expectException(meResponse, UNAUTHORIZED, "error_not_authorized");
		}
	}

	@Test
	public void testLoginAndDisableUser() {
		try (NoTx noTrx = db.noTx()) {
			User user = user();
			String username = user.getUsername();

			MeshRestClient client = MeshRestClient.create("localhost", getPort(), Mesh.vertx());
			client.setLogin(username, password());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.toBlocking().value();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			user.disable();

			MeshResponse<UserResponse> meResponse = client.me().invoke();
			latchFor(meResponse);
			expectException(meResponse, UNAUTHORIZED, "error_not_authorized");
		}
	}

	@Test
	public void testAutomaticTokenRefresh() throws InterruptedException {
		try (NoTx noTrx = db.noTx()) {
			User user = user();
			String username = user.getUsername();

			MeshRestClient client = MeshRestClient.create("localhost", getPort(), Mesh.vertx());
			client.setLogin(username, password());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.toBlocking().value();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			MeshResponse<UserResponse> response = client.me().invoke();
			latchFor(response);
			String meshTokenCookie1 = response.getResponse().getHeader("Set-Cookie");

			Thread.sleep(2000);

			response = client.me().invoke();
			latchFor(response);
			String meshTokenCookie2 = response.getResponse().getHeader("Set-Cookie");

			assertNotEquals("Both cookies should be different. Otherwise the token was not regenerated and the exp. date was not bumped.",
					meshTokenCookie1, meshTokenCookie2);
		}
	}

}
