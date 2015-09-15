package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import org.springframework.stereotype.Component;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.NoTrx;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.User;

@Component
public class AuthenticationRestHandler extends AbstractHandler {

	public void handleMe(ActionContext ac) {
		try (NoTrx tx = db.noTrx()) {
			MeshAuthUser requestUser = ac.getUser();
			transformAndResponde(ac, requestUser);
		}
	}

	public void handleLogin(ActionContext ac) {
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
					GenericMessageResponse message = new GenericMessageResponse("OK");
					ac.send(JsonUtil.toJson(message));
				}
			});
		} catch (Exception e) {
			ac.fail(UNAUTHORIZED, "auth_login_failed", e);
		}

	}

	public void handleLogout(ActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message));
	}

}
