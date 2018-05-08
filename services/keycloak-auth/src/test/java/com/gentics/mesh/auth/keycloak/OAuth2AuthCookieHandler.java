package com.gentics.mesh.auth.keycloak;

import io.vertx.core.Handler;
import io.vertx.ext.auth.oauth2.OAuth2Auth;
import io.vertx.ext.web.RoutingContext;

public interface OAuth2AuthCookieHandler extends Handler<RoutingContext> {

	static OAuth2AuthCookieHandler create(OAuth2Auth auth) {
		return new OAuth2AuthCookieHandlerImpl(auth);
	}

}
