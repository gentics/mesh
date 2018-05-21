package com.gentics.mesh.auth;

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;

import com.gentics.mesh.test.docker.KeycloakContainer;

import io.reactivex.Single;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.impl.NoStackTraceThrowable;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.AccessToken;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class KeycloakAuthTest extends AbstractKeycloakTest {

	@ClassRule
	public static KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmFile("src/test/resources/realm.json")
		.waitStartup();

	private static Vertx vertx = Vertx.vertx();

	private HttpClient client = vertx
		.createHttpClient(new HttpClientOptions().setDefaultHost("localhost").setDefaultPort(keycloak.getFirstMappedPort()));

	@Test
	public void testKeyCloak() throws IOException {
		int keycloakPort = keycloak.getFirstMappedPort();
		System.out.println("Port: " + keycloakPort);

		JsonObject config = loadKeycloakConfig();

		OAuth2Auth keyCloakAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		Router router = Router.router(vertx);
		router.route().handler(CookieHandler.create());
		OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(keyCloakAuth, "http://localhost:8080");
		OAuth2AuthCookieHandler oauth2cookies = new OAuth2AuthCookieHandlerImpl(keyCloakAuth);

		// oauth2.setupCallback(router.get("/callback"));
		// router.route().handler(oauth2cookies);
		router.route().handler(oauth2cookies);
		router.route("/protected/*").handler(oauth2).failureHandler(rc -> {
			if (rc.failed()) {
				Throwable error = rc.failure();
				if (error instanceof NoStackTraceThrowable) {
					NoStackTraceThrowable s = (NoStackTraceThrowable) error;
					String msg = s.getMessage();
					if ("callback route is not configured.".equalsIgnoreCase(msg)) {
						// Suppress the error and use 401 instead
						rc.response().setStatusCode(401).end();
						return;
					}
				} else {
					rc.fail(error);
				}
			}
		});
		router.route("/protected/me").handler(rc -> {

			User user = rc.user();
			if (user instanceof AccessToken) {
				AccessToken token = (AccessToken) user;
				// TODO extract the role information from the user info
				token.userInfo(info -> {
					if (info.failed()) {
						info.cause().printStackTrace();
					} else {
						rc.response().end(info.result().encodePrettily());
						// System.out.println(info.result().encodePrettily());
					}
				});
			}

		});
		router.route("/protected/*").handler(rc -> {
			rc.response().end("Welcome to the protected resource!");
		});
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);

		JsonObject token = loginKeycloak();
		System.out.println(token.encodePrettily());
		JsonObject info = get("/protected/me", token);
		System.out.println(info.encodePrettily());

		System.out.println(get("/protected/me"));

	}

	private JsonObject loadKeycloakConfig() throws IOException {
		int keycloakPort = keycloak.getFirstMappedPort();

		Single<JsonObject> result = Single.create(sub -> {
			client.getNow("/auth/realms/master-test", rh -> {
				int code = rh.statusCode();
				if (code != 200) {
					sub.onError(new RuntimeException("Error while loading realm info. Got code {" + code + "}"));
				}
				rh.bodyHandler(bh -> {
					JsonObject json = bh.toJsonObject();
					sub.onSuccess(json);
				});
			});
		});
		JsonObject json = result.blockingGet();
		JsonObject clientConfig = loadJson("/keycloak-installation.json");
		clientConfig.put("auth-server-url", "http://localhost:" + keycloakPort + "/auth");
		clientConfig.put("realm-public-key", json.getString("public_key"));
		return clientConfig;
	}

	private JsonObject loginKeycloak() throws IOException {
		String secret = "9b65c378-5b4c-4e25-b5a1-a53a381b5fb4";

		StringBuilder content = new StringBuilder();
		content.append("client_id=mesh&");
		content.append("username=dummyuser&");
		content.append("password=finger&");
		content.append("grant_type=password&");
		content.append("client_secret=" + secret);
		RequestBody body = RequestBody.create(MediaType.parse("application/x-www-form-urlencoded"), content.toString());
		// .header("Content-Type", "application/x-www-form-urlencoded")
		Request request = new Request.Builder()
			.post(body)
			.url("http://localhost:" + keycloak.getFirstMappedPort() + "/auth/realms/master-test/protocol/openid-connect/token")
			.build();

		Response response = client().newCall(request).execute();
		return new JsonObject(response.body().string());
	}

}
