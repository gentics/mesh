package com.gentics.mesh.core.verticle.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.json.JsonUtil;

public abstract class AbstractAuthRestHandler extends AbstractHandler implements AuthenticationRestHandler {

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	@Override
	public void handleMe(InternalActionContext ac) {
		db.asyncNoTrxExperimental(() -> {
			//TODO add permission check
			MeshAuthUser requestUser = ac.getUser();
			return requestUser.transformToRest(ac);
		}).subscribe(model -> ac.respond(model, OK), ac::fail);
	}

	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	@Override
	public void handleLogout(InternalActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);
	}

}
