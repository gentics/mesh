package com.gentics.mesh.auth.keycloak;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import okhttp3.OkHttpClient;
import okhttp3.OkHttpClient.Builder;
import okhttp3.Request;
import okhttp3.Response;

public abstract class AbstractKeycloakTest {

	private static final Logger log = LoggerFactory.getLogger(AbstractKeycloakTest.class);

	private void setAuthCookie(RoutingContext rc) {
		// JsonObject principal = rc.user().principal();
		//
		// Cookie cookie = Cookie.cookie(COOKIE_AUTH, principal.getString("access_token"))
		// .setPath("/")
		// .setMaxAge(principal.getLong("expires_in", DEFAULT_AUTH_TIMEOUT));
		//
		// rc.addCookie(cookie);
		//
		// cookie = Cookie.cookie(COOKIE_AUTH_REFRESH, principal.getString("refresh_token"))
		// .setPath("/")
		// .setMaxAge(principal.getLong("refresh_expires_in", DEFAULT_AUTH_TIMEOUT));
		//
		// rc.addCookie(cookie);
	}

	// private JsonObject loginKeycloak() throws IOException {
	// String secret = "9b65c378-5b4c-4e25-b5a1-a53a381b5fb4";
	//
	// StringBuilder content = new StringBuilder();
	// content.append("client_id=mesh&");
	// content.append("username=dummyuser&");
	// content.append("password=finger&");
	// content.append("grant_type=password&");
	// content.append("client_secret=" + secret);
	// RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), content.toString());
	// // .header("Content-Type", "application/x-www-form-urlencoded")
	// Request request = new Request.Builder()
	// .post(body)
	// .url("http://localhost:" + keycloak.getFirstMappedPort() + "/auth/realms/master-test/protocol/openid-connect/token")
	// .build();
	//
	// Response response = client().newCall(request).execute();
	// return new JsonObject(response.body().string());
	// }
	
	// JsonObject token = loginKeycloak();
	// System.out.println(token.encodePrettily());
	//
	// System.in.read();
	// JsonObject info = get("/protected/me", token);
	// System.out.println(info.encodePrettily());

	protected OkHttpClient client() {
		Builder builder = new OkHttpClient.Builder();
		return builder.build();
	}

	protected void addDebugRoute(Router router) {
		router.route("/protected/me").handler(rc -> {
			User user = rc.user();
//			if (user instanceof AccessTokenImpl) {
//				AccessTokenImpl token = (AccessTokenImpl) user;
//				// TODO extract the role information from the user info
//				token.userInfo(info -> {
//					if (info.failed()) {
//						info.cause().printStackTrace();
//					} else {
//						System.out.println(info.result().encodePrettily());
//					}
//				});
//			}

			rc.response().end(rc.user().principal().encodePrettily());
		});

	}

	protected JsonObject get(String path, JsonObject token) throws IOException {
		Request request = new Request.Builder()
			.header("Accept", "application/json")
			.header("Authorization", "Bearer " + token.getString("access_token"))
			.url("http://localhost:8080" + path)
			.build();

		Response response = client().newCall(request).execute();
		return new JsonObject(response.body().string());
	}

	protected JsonObject loadJson(String path) throws IOException {
		return new JsonObject(IOUtils.toString(getClass().getResource(path), Charset.defaultCharset()));
	}
}
