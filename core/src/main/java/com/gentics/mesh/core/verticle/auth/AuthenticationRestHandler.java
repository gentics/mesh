package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpResponseStatus.UNAUTHORIZED;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.auth.MeshAuthProvider;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.auth.LoginRequest;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.json.JsonUtil;

@Singleton
public class AuthenticationRestHandler extends AbstractHandler {

	private MeshAuthProvider authProvider;
	private Database db;

	@Inject
	public AuthenticationRestHandler(MeshAuthProvider authProvider, Database db) {
		this.authProvider = authProvider;
		this.db = db;
	}

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	public void handleMe(InternalActionContext ac) {
		db.asyncTx(() -> {
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
			authProvider.login(ac, request.getUsername(), request.getPassword());
		} catch (Exception e) {
			throw error(UNAUTHORIZED, "auth_login_failed", e);
		}
	}

}
