package com.gentics.mesh.core.endpoint.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.provider.MeshJWTAuthProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.endpoint.handler.AbstractHandler;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.json.JsonUtil;

@Singleton
public class AuthenticationRestHandler extends AbstractHandler {

	private MeshJWTAuthProvider authProvider;
	private HandlerUtilities utils;

	@Inject
	public AuthenticationRestHandler(MeshJWTAuthProvider authProvider, HandlerUtilities utils) {
		this.authProvider = authProvider;
		this.utils = utils;
	}

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	public void handleMe(InternalActionContext ac) {
		utils.syncTx(ac, tx -> {
			// TODO add permission check
			MeshAuthUser requestUser = ac.getUser();
			return requestUser.transformToRestSync(ac, 0);
		}, model -> ac.send(model, OK));
	}

	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	public void handleLogout(InternalActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(message.toJson(), OK);
	}

	/**
	 * Handle a login request.
	 * 
	 * @param ac
	 */
	public void handleLoginJWT(InternalActionContext ac) {
		try {
			LoginRequest request = JsonUtil.readValue(ac.getBodyAsString(), LoginRequest.class);
			if (request.getUsername() == null) {
				throw error(BAD_REQUEST, "error_json_field_missing", "username");
			}
			if (request.getPassword() == null) {
				throw error(BAD_REQUEST, "error_json_field_missing", "password");
			}
			authProvider.login(ac, request.getUsername(), request.getPassword(), request.getNewPassword());
		} catch (GenericRestException e) {
			throw e;
		} catch (Exception e) {
			throw error(UNAUTHORIZED, "auth_login_failed", e);
		}
	}

}
