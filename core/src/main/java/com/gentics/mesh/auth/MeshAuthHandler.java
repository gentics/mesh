package com.gentics.mesh.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.Cookie;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.impl.JWTAuthHandlerImpl;

/**
 * This class extends the vertx JWTAuthHandler, so that it also works when the token is set as a cookie. Central authentication handler for mesh.
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

	/**
	 * Handle the login action and set a token cookie if the credentials are valid.
	 * 
	 * @param ac
	 * @param username
	 * @param password
	 */
	public void login(InternalActionContext ac, String username, String password) {
		((MeshAuthProvider) authProvider).generateToken(username, password, rh -> {
			if (rh.failed()) {
				throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
			} else {
				ac.addCookie(Cookie.cookie(MeshAuthProvider.TOKEN_COOKIE_KEY, rh.result()).setPath("/"));
				ac.send(JsonUtil.toJson(new TokenResponse(rh.result())));
			}
		});

	}
}
