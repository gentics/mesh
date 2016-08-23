package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.auth.MeshJWTAuthProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.ext.web.Cookie;


public class JWTAuthRestHandler extends AbstractAuthRestHandler {

	public static final String TOKEN_COOKIE_KEY = "mesh.token";
	
	public static JWTAuthRestHandler create() {
		return new JWTAuthRestHandler();
	}

	@Override
	public void handleLogin(InternalActionContext ac) {
		MeshJWTAuthProvider provider = getAuthProvider();
		
		try {
			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
			if (request.getUsername() == null) {
				throw error(BAD_REQUEST, "error_json_field_missing", "username");
			}
			if (request.getPassword() == null) {
				throw error(BAD_REQUEST, "error_json_field_missing", "password");
			}
			
			provider.generateToken(request.getUsername(), request.getPassword(), rh -> {
				if (rh.failed()) {
					throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
				} else {
					ac.addCookie(Cookie.cookie(TOKEN_COOKIE_KEY, rh.result()).setPath("/"));
					ac.send(JsonUtil.toJson(new TokenResponse(rh.result())));
				}
			});
		} catch (Exception e) {
			throw error(UNAUTHORIZED, "auth_login_failed", e);
		}
	}

	/**
	 * Gets the auth provider as MeshJWTAuthProvider
	 * @return
	 */
	private MeshJWTAuthProvider getAuthProvider() {
		MeshAuthProvider provider = springConfiguration.authProvider();
		if (provider instanceof MeshJWTAuthProvider) {
			return (MeshJWTAuthProvider)provider;
		} else {
			throw new IllegalStateException("AuthProvider must be an instance of MeshJWTAuthProvider when using JWT!");
		}
	}
}
