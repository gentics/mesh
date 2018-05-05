package com.gentics.mesh.auth.keycloak;

import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;
import org.junit.ClassRule;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.impl.AccessTokenImpl;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class KeycloakAuthTest {

	private static final Logger log = LoggerFactory.getLogger(KeycloakAuthTest.class);

	@ClassRule
	public static KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmFile("src/test/resources/realm.json")
		.waitStartup();

	@Test
	public void testKeyCloak() throws IOException {
		int keycloakPort = keycloak.getFirstMappedPort();
		System.out.println("Port: " + keycloakPort);
		Vertx vertx = Vertx.vertx();

		Router router = Router.router(vertx);
		JsonObject config = loadJson("/keycloak-installation.json").put("auth-server-url", "http://localhost:" + keycloakPort + "/auth");
		OAuth2Auth keyCloakAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);

		OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(keyCloakAuth,
			"http://localhost:8080");

		oauth2.setupCallback(router.get("/callback"));

		router.route("/protected/*").handler(oauth2);

		router.route("/protected/me").handler(rc -> {
			User user = rc.user();
			if (user instanceof AccessTokenImpl) {
				AccessTokenImpl token = (AccessTokenImpl) user;
				// TODO extract the role information from the user info
				token.userInfo(info -> {
					if (info.failed()) {
						info.cause().printStackTrace();
					} else {
						System.out.println(info.result().encodePrettily());
					}
				});
			}

			// TODO decode the access_token and extract the needed information
			System.out.println(rc.user().getClass().getName());
			rc.response().end(rc.user().principal().encodePrettily());
		});
		router.route("/protected/*").handler(rc -> {

			rc.response().end("Welcome to the protected resource!");
		});
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);

		System.in.read();
	}

	private JsonObject loadJson(String path) throws IOException {
		return new JsonObject(IOUtils.toString(getClass().getResource(path), Charset.defaultCharset()));
	}

}

