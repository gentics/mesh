package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.auth.User;

public class BasicAuthRestHandler extends AbstractAuthRestHandler {

	private static final Logger log = LoggerFactory.getLogger(BasicAuthRestHandler.class);

	public static BasicAuthRestHandler create() {
		return new BasicAuthRestHandler();
	}

	/**
	 * Handle a login request.
	 * 
	 * @param ac
	 */
	@Override
	public void handleLogin(InternalActionContext ac) {
		try {
			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
			// TODO fail on missing field
			JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
			springConfiguration.authProvider().authenticate(authInfo, rh -> {
				if (rh.failed()) {
					throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
				} else {
					User user = rh.result();
					if (user instanceof MeshAuthUser) {
						ac.setUser((MeshAuthUser) user);
						ac.send(JsonUtil.toJson(new GenericMessageResponse("OK")), OK);
					} else {
						log.error("Auth Provider did not return a {" + MeshAuthUser.class.getName() + "} user got {" + user.getClass().getName()
								+ "} instead.");
						throw error(BAD_REQUEST, "auth_login_failed");
					}
				}
			});
		} catch (Exception e) {
			throw error(UNAUTHORIZED, "auth_login_failed", e);
		}

	}
}
