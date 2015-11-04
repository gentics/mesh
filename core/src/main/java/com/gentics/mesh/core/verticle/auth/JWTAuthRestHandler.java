package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import org.springframework.stereotype.Component;

import com.gentics.mesh.auth.MeshJWTAuthProvider;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

@Component
public class JWTAuthRestHandler extends AbstractHandler implements AuthenticationRestHandler{

	@Override
	public void handleMe(InternalActionContext ac) {
		db.asyncNoTrx(tx -> {
			MeshAuthUser requestUser = ac.getUser();
			transformAndResponde(ac, requestUser, OK);
		} , ac.errorHandler());
	}

	@Override
	public void handleLogin(InternalActionContext ac) {
		MeshJWTAuthProvider provider = springConfiguration.authProvider();
		
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
			
			JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
			provider.authenticate(authInfo, rh -> {
				if (rh.failed()) {
					ac.fail(UNAUTHORIZED, "auth_login_failed", rh.cause());
				} else {
					User authenticated = rh.result();
					String token = provider.generateToken(authenticated);
					ac.send(new JsonObject().put("token", token).toString(), OK);
				}
			});
		} catch (Exception e) {
			ac.fail(UNAUTHORIZED, "auth_login_failed", e);
		}
	}

	@Override
	public void handleLogout(InternalActionContext ac) {
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);
	}
	
	/**
	 * Refreshes the web token. Requires a valid token and user.
	 * @param ac
	 */
	public void handleRefresh(InternalActionContext ac) {
		MeshJWTAuthProvider provider = springConfiguration.authProvider();
		MeshAuthUser user = ac.getUser();
		if (user == null) {
			ac.fail(UNAUTHORIZED, "auth_login_failed");
			return;
		}
		String token = provider.generateToken(ac.getUser());
		ac.send(new JsonObject().put("token", token).toString(), OK);
	}
	
}
