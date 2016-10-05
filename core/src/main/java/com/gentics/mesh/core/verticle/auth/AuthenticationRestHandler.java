package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.core.verticle.handler.HandlerUtilities.operateNoTx;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshAuthHandler;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.json.JsonUtil;

@Singleton
public class AuthenticationRestHandler extends AbstractHandler {

	private MeshAuthHandler authHandler;

	@Inject
	public AuthenticationRestHandler(MeshAuthHandler authHandler) {
		this.authHandler = authHandler;
	}

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	public void handleMe(InternalActionContext ac) {
		operateNoTx(() -> {
			// TODO add permission check
			MeshAuthUser requestUser = ac.getUser();
			return requestUser.transformToRest(ac, 0);
		}).subscribe(model -> ac.send(model, OK), ac::fail);
	}

	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	public void handleLogout(InternalActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);
	}

	// /**
	// * Handle a login request.
	// *
	// * @param ac
	// */
	// /**
	// * Handle a login request.
	// *
	// * @param ac
	// */
	// public void handleLogin(InternalActionContext ac) {
	// try {
	// LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
	// // TODO fail on missing field
	// JsonObject authInfo = new JsonObject().put("username", request.getUsername()).put("password", request.getPassword());
	// authProvider.authenticate(authInfo, rh -> {
	// if (rh.failed()) {
	// throw error(UNAUTHORIZED, "auth_login_failed", rh.cause());
	// } else {
	// User user = rh.result();
	// if (user instanceof MeshAuthUser) {
	// ac.setUser((MeshAuthUser) user);
	// ac.send(JsonUtil.toJson(new GenericMessageResponse("OK")), OK);
	// } else {
	// log.error("Auth Provider did not return a {" + MeshAuthUser.class.getName() + "} user got {" + user.getClass().getName()
	// + "} instead.");
	// throw error(BAD_REQUEST, "auth_login_failed");
	// }
	// }
	// });
	// } catch (Exception e) {
	// throw error(UNAUTHORIZED, "auth_login_failed", e);
	// }
	//
	// }

	public void handleLoginJWT(InternalActionContext ac) {

		try {
			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
			if (request.getUsername() == null) {
				throw error(BAD_REQUEST, "error_json_field_missing", "username");
			}
			if (request.getPassword() == null) {
				throw error(BAD_REQUEST, "error_json_field_missing", "password");
			}

			authHandler.login(ac, request.getUsername(), request.getPassword());
		} catch (Exception e) {
			throw error(UNAUTHORIZED, "auth_login_failed", e);
		}
	}

}
