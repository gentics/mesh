package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Single;

@MeshTestSetting(testSize = FULL, startServer = true)
public class AuthenticationEndpointTest extends AbstractMeshTest {

	@Test
	public void testRestClient() throws Exception {
		try (Tx tx = tx()) {
			User user = user();
			String username = user.getUsername();
			String uuid = user.getUuid();

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
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
	@Ignore("It is currently not possible to disable users via REST.")
	public void testLoginAndDisableUser() {
		String username = db().tx(() -> user().getUsername());

		MeshRestClient client = MeshRestClient.create("localhost", port(), false);
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

			MeshRestClient client = MeshRestClient.create("localhost", port(), false);
			client.setLogin(username, data().getUserInfo().getPassword());
			Single<GenericMessageResponse> future = client.login();

			GenericMessageResponse loginResponse = future.blockingGet();
			assertNotNull(loginResponse);
			assertEquals("OK", loginResponse.getMessage());

			String meshTokenCookie1 = client.me().getResponse().blockingGet().getHeader("Set-Cookie").orElse(null);

			Thread.sleep(2000);

			String meshTokenCookie2 = client.me().getResponse().blockingGet().getHeader("Set-Cookie").orElse(null);

			assertNotEquals("Both cookies should be different. Otherwise the token was not regenerated and the exp. date was not bumped.",
					meshTokenCookie1, meshTokenCookie2);
		}
	}

}
