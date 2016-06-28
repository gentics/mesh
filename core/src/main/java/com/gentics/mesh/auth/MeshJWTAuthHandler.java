package com.gentics.mesh.auth;

import com.gentics.mesh.etc.config.MeshOptions;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

/**
 * This class extends the vertx JWTAuthHandler, so that it also works when the token is set as a cookie.
 */
public class MeshJWTAuthHandler extends JWTAuthHandlerImpl {

	public MeshJWTAuthHandler(MeshAuthProvider authProvider) {
		super(authProvider, null);
	}

	@Override
	public void handle(RoutingContext context) {
		Cookie token = context.getCookie(MeshOptions.JWT_TOKEN_KEY);
		if (token != null) {
			context.request().headers().set(HttpHeaders.AUTHORIZATION, "Bearer " + token.getValue());
		}
		super.handle(context);
	}

	public static AuthHandler create(MeshAuthProvider authProvider) {
		return new MeshJWTAuthHandler(authProvider);
	}
}
