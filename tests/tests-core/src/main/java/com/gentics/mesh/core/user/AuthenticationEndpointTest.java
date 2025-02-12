package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.data.user.MeshAuthUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshRestClientMessageException;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.util.TokenUtil;

import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@MeshTestSetting(testSize = FULL, startServer = true)
public class AuthenticationEndpointTest extends AbstractMeshTest {

	@Test
	public void testRestClient() throws Exception {
		try (Tx tx = tx()) {
			HibUser user = user();
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
	public void testApiTokenLogin() {
		UserResponse response = call(() -> client().me());
		String token = call(() -> client().issueAPIToken(response.getUuid())).getToken();
		client().setAPIKey(token).setLogin(null, null, null);
		GenericMessageResponse loginResponse = client().login().blockingGet();
		assertNotNull(loginResponse);
		assertEquals("OK", loginResponse.getMessage());
	}

	@Test
	public void testApiTokenBadLogin() {
		String token = TokenUtil.randomToken();
		client().setAPIKey(token).setLogin(null, null, null);
		try {
			client().login().blockingGet();
		} catch (Exception e) {
			if (e.getCause() instanceof MeshRestClientMessageException) {
				MeshRestClientMessageException me = (MeshRestClientMessageException) e.getCause();
				assertEquals(401, me.getStatusCode());
				return;
			}
		}
		fail();
	}

	@Test
	public void testDisableAnonymousAccess() {
		client().logout();
		UserResponse response = client().me().toSingle().blockingGet();
		assertEquals("anonymous", response.getUsername());
		client().disableAnonymousAccess();
		call(() -> client().me(), UNAUTHORIZED, "error_not_authorized");
	}

	/**
	 * This test does belong to the `AuthUserTest` suite, but needs a running server, so placed here for convenience.
	 */
	@Test
	public void testLoadPrincipalWithReferencedNodeWithoutTx() {
		NodeResponse referencedNode = createNode();
		tx(tx -> {
			MeshAuthUser au = getRequestMeshAuthUser();
			HibNode rn = tx.nodeDao().findByUuidGlobal(referencedNode.getUuid());
			tx.userDao().findByUuid(au.getDelegate().getUuid()).setReferencedNode(rn);
			tx.success();
		});
		MeshAuthUser user = tx(() -> getRequestMeshAuthUser());

		JsonObject json = user.principal();
		assertNotNull(json);
		assertEquals(userUuid(), json.getString("uuid"));
		assertEquals(tx(() -> user.getDelegate().getEmailAddress()), json.getString("emailAddress"));
		assertEquals(tx(() -> user.getDelegate().getLastname()), json.getString("lastname"));
		assertEquals(tx(() -> user.getDelegate().getFirstname()), json.getString("firstname"));
		assertEquals(tx(() -> user.getDelegate().getUsername()), json.getString("username"));

		assertNotNull(json.getString("nodeReference"));
		assertEquals(referencedNode.getUuid(), json.getString("nodeReference"));

		JsonArray roles = json.getJsonArray("roles");
		for (int i = 0; i < roles.size(); i++) {
			JsonObject role = roles.getJsonObject(i);
			assertNotNull(role.getString("uuid"));
			assertNotNull(role.getString("name"));
		}
		assertEquals("The principal should contain two roles.", 1, roles.size());
		JsonArray groups = json.getJsonArray("groups");
		for (int i = 0; i < roles.size(); i++) {
			JsonObject group = groups.getJsonObject(i);
			assertNotNull(group.getString("uuid"));
			assertNotNull(group.getString("name"));
		}
		assertEquals("The principal should contain two groups.", 1, groups.size());
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
			HibUser user = user();
			user.disable();
			tx.success();
		}

		call(() -> client.me(), UNAUTHORIZED, "error_not_authorized");
	}

	@Test
	public void testAutomaticTokenRefresh() throws InterruptedException {
		try (Tx tx = tx()) {
			HibUser user = user();
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

	@Test
	public void testBasicAuth() throws IOException {
		OkHttpClient client = httpClient().newBuilder().cookieJar(new TestCookieJar()).build();
		Response response = client.newCall(new Request.Builder()
			.get()
			.url(String.format("http://%s:%s/api/v2/auth/login", "localhost", port()))
			.header("Authorization", "Basic " + base64("admin:admin"))
			.build()).execute();

		assertThat(response.code()).isEqualTo(200);

		response = client.newCall(new Request.Builder()
			.get()
			.url(String.format("http://%s:%s/api/v2/auth/me", "localhost", port()))
			.build()).execute();
		JsonObject responseBody = new JsonObject(response.body().string());
		assertThat(responseBody.getString("username")).isEqualTo("admin");
	}

	private String base64(String input) {
		return Base64.getEncoder().encodeToString(input.getBytes(StandardCharsets.UTF_8));
	}

	public static class TestCookieJar implements CookieJar {

		private List<Cookie> cookies;

		@Override
		public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
			this.cookies =  cookies;
		}

		@Override
		public List<Cookie> loadForRequest(HttpUrl url) {
			if (cookies != null)
				return cookies;
			return new ArrayList<Cookie>();

		}
	}
}