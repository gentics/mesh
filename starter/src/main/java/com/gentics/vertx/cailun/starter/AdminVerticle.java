package com.gentics.vertx.cailun.starter;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.apex.addons.BasicAuthHandler;
import io.vertx.ext.apex.addons.LocalSessionStore;
import io.vertx.ext.apex.addons.SessionHandler;
import io.vertx.ext.apex.core.BodyHandler;
import io.vertx.ext.apex.core.CookieHandler;
import io.vertx.ext.apex.core.RoutingContext;
import io.vertx.ext.apex.core.Session;
import io.vertx.ext.apex.core.SessionStore;
import io.vertx.ext.auth.AuthService;

import com.gentics.vertx.cailun.rest.AbstractCailunRestVerticle;

public class AdminVerticle extends AbstractCailunRestVerticle {

	public AdminVerticle() {
		super("admin");
	}

	@Override
	public void start() throws Exception {
		super.start();
		Handler<RoutingContext> handler = rc -> {
			Session sess = rc.session();
			System.out.println(sess);
			System.out.println(sess.isLoggedIn());
			rc.response().end("Welcome to the protected resource!");
		};

		route().handler(BodyHandler.bodyHandler());
		route().handler(CookieHandler.cookieHandler());
		SessionStore store = LocalSessionStore.localSessionStore(vertx);
		route().handler(SessionHandler.sessionHandler(store));
		// JsonObject authConfig = new JsonObject().put("properties_path", "classpath:login/loginusers.properties");
		JsonObject authConfig = new JsonObject();
		// authConfig.put(AuthService.AUTH_REALM_CLASS_NAME_FIELD, ShiroAuthRealmImpl.class.getCanonicalName());
		AuthService authService = AuthService.create(vertx, authConfig);

		route("/protected").handler(BasicAuthHandler.basicAuthHandler(authService, BasicAuthHandler.DEFAULT_REALM));

		route("/protected/somepage").handler(handler);
	}

}
