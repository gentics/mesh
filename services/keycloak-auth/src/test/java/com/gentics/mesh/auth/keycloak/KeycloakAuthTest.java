package com.gentics.mesh.auth.keycloak;

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.auth.oauth2.OAuth2FlowType;
import io.vertx.ext.auth.oauth2.providers.KeycloakAuth;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.OAuth2AuthHandler;

public class KeycloakAuthTest extends AbstractKeycloakTest {

	@ClassRule
	public static KeycloakContainer keycloak = new KeycloakContainer()
		.withRealmFile("src/test/resources/realm.json")
		.waitStartup();

	@Test
	public void testKeyCloak() throws IOException {
		int keycloakPort = keycloak.getFirstMappedPort();
		System.out.println("Port: " + keycloakPort);

		Vertx vertx = Vertx.vertx();
		JsonObject config = loadJson("/keycloak-installation.json")
			.put("auth-server-url", "http://localhost:" + keycloakPort + "/auth");
		OAuth2Auth keyCloakAuth = KeycloakAuth.create(vertx, OAuth2FlowType.AUTH_CODE, config);
		// SessionStore store = LocalSessionStore.create(vertx);

		Router router = Router.router(vertx);
		router.route().handler(CookieHandler.create());
		// router.route().handler(SessionHandler.create(store));
		// router.route().handler(UserSessionHandler.create(keyCloakAuth));
		OAuth2AuthHandler oauth2 = OAuth2AuthHandler.create(keyCloakAuth, "http://localhost:8080");

		OAuth2AuthCookieHandler oauth2cookies = OAuth2AuthCookieHandler.create(keyCloakAuth);
		router.route().handler(oauth2cookies);
		oauth2.setupCallback(router.get("/callback"));
		router.route().handler(oauth2cookies);

		router.route("/protected/*").handler(oauth2);
		router.route("/protected/*").handler(rc -> {
			rc.response().end("Welcome to the protected resource!");
		});
		vertx.createHttpServer().requestHandler(router::accept).listen(8080);

		System.in.read();
	}

}
