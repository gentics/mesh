package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import org.springframework.stereotype.Component;

import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.auth.MeshJWTAuthProvider;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.auth.TokenResponse;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.handler.InternalHttpActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.ext.web.Cookie;

@Component
public class JWTAuthRestHandler extends AbstractHandler implements AuthenticationRestHandler{

	public static final String TOKEN_COOKIE_KEY = "mesh.token";
	
	@Override
	public void handleMe(InternalHttpActionContext ac) {
		db.asyncNoTrx(tx -> {
			MeshAuthUser requestUser = ac.getUser();
			transformAndResponde(ac, requestUser, OK);
		} , ac.errorHandler());
	}

	@Override
	public void handleLogin(InternalHttpActionContext ac) {
		MeshJWTAuthProvider provider = getAuthProvider();
		
		try {
			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
			if (request.getUsername() == null) {
				ac.fail(BAD_REQUEST, "error_json_field_missing", "username");
				return;
			}
			if (request.getPassword() == null) {
				ac.fail(BAD_REQUEST, "error_json_field_missing", "password");
				return;
			}
			
			provider.generateToken(request.getUsername(), request.getPassword(), rh -> {
				if (rh.failed()) {
					ac.fail(UNAUTHORIZED, "auth_login_failed", rh.cause());
				} else {
					ac.addCookie(Cookie.cookie(TOKEN_COOKIE_KEY, rh.result()).setPath("/"));
					ac.send(JsonUtil.toJson(new TokenResponse(rh.result())));
				}
			});
		} catch (Exception e) {
			ac.fail(UNAUTHORIZED, "auth_login_failed", e);
		}
	}

	@Override
	public void handleLogout(InternalHttpActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);
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
