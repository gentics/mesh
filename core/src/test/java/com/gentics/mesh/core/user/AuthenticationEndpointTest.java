package com.gentics.mesh.core.user;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestSize.FULL;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.Tx;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.user.UserResponse;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.reactivex.Single;
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
			.header(AUTHORIZATION, "Basic " + base64("admin:admin"))
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