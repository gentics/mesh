package com.gentics.mesh.auth;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

/**
 * This class extends the Vert.x JWTAuthHandler, so that it also works when the token is set as a cookie. Central authentication handler for mesh.
 */
@Singleton
public class MeshAuthHandler extends JWTAuthHandlerImpl {

	@Inject
	public MeshAuthHandler(MeshAuthProvider authProvider) {
		super(authProvider, null);
	}

	@Override
	public void handle(RoutingContext context) {
		Cookie token = context.getCookie(MeshAuthProvider.TOKEN_COOKIE_KEY);
		if (token != null) {
			context.request().headers().set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getValue());
		}
		super.handle(context);
	}

}
