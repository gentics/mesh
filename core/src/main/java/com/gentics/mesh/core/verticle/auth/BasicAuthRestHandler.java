package com.gentics.mesh.core.verticle.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.handler.InternalHttpActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

@Component
public class BasicAuthRestHandler extends AbstractAuthRestHandler {

	public static BasicAuthRestHandler create() {
		return new BasicAuthRestHandler();
	}

	/**
	 * Handle a login request.
	 * 
	 * @param ac
	 */
	@Override
	public void handleLogin(InternalHttpActionContext ac) {
		try {
			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
			// TODO fail on missing field
			JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
			springConfiguration.authProvider().authenticate(authInfo, rh -> {
				if (rh.failed()) {
					ac.fail(UNAUTHORIZED, "auth_login_failed", rh.cause());
				} else {
					User authenticated = rh.result();
					ac.setUser(authenticated);
					ac.send(JsonUtil.toJson(new GenericMessageResponse("OK")), OK);
				}
			});
		} catch (Exception e) {
			ac.fail(UNAUTHORIZED, "auth_login_failed", e);
		}

	}
}
