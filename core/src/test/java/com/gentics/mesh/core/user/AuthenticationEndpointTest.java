package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.ClientHelper.expectException;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import com.gentics.mesh.rest.client.impl.MeshRestOkHttpClientImpl;
import org.junit.Test;

import com.syncleus.ferma.tx.Tx;
import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Single;

@MeshTestSetting(useElasticsearch = false, testSize = FULL, startServer = true)
public class AuthenticationEndpointTest extends AbstractMeshTest {

	@Test
	public void testRestClient() throws Exception {
		try (Tx tx = tx()) {
			User user = user();
			String username = user.getUsername();
			String uuid = user.getUuid();

			MeshRestClient client = MeshRestClient.create("localhost", port(), false, Mesh.vertx());
			client.setLogin(username, data().getUserInfo().getPassword());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.blockingGet();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			UserResponse me = client().me().blockingGet();

			assertNotNull(me);
			assertEquals(uuid, me.getUuid());

			disableAnonymousAccess();

			Single<GenericMessageResponse> logoutFuture = client.logout();
			logoutFuture.blockingGet();

			call(() -> client.me(), UNAUTHORIZED, "error_not_authorized");
		}
	}

	@Test
	public void testDisableAnonymousAccess() {
		client().logout();
		UserResponse response = client().me().toSingle().blockingGet();
		assertEquals("anonymous", response.getUsername());
		client().disableAnonymousAccess();
		call(() -> client().me(), UNAUTHORIZED, "error_not_authorized");
	}

	@Test
	public void testLoginAndDisableUser() {
		String username = db().tx(() -> user().getUsername());

		MeshRestClient client = MeshRestClient.create("localhost", port(), false, Mesh.vertx());
		client.setLogin(username, data().getUserInfo().getPassword());
		Single<GenericMessageResponse> future = client.login();

		GenericMessageResponse loginResponse = future.blockingGet();
		assertNotNull(loginResponse);
		assertEquals("OK", loginResponse.getMessage());

		try (Tx tx = tx()) {
			User user = user();
			user.disable();
			tx.success();
		}

		call(() -> client.me(), UNAUTHORIZED, "error_not_authorized");
	}

	@Test
	public void testAutomaticTokenRefresh() throws InterruptedException {
		try (Tx tx = tx()) {
			User user = user();
			String username = user.getUsername();

			MeshRestClient client = MeshRestClient.create("localhost", port(), false, Mesh.vertx());
			client.setLogin(username, data().getUserInfo().getPassword());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.blockingGet();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			MeshResponse<UserResponse> response = client.me().invoke();
			latchFor(response);
			String meshTokenCookie1 = response.getRawResponse().getHeader("Set-Cookie");

			Thread.sleep(2000);

			response = client.me().invoke();
			latchFor(response);
			String meshTokenCookie2 = response.getRawResponse().getHeader("Set-Cookie");

			assertNotEquals("Both cookies should be different. Otherwise the token was not regenerated and the exp. date was not bumped.",
					meshTokenCookie1, meshTokenCookie2);
		}
	}

}
