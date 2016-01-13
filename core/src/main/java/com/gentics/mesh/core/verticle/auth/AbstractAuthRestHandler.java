package com.gentics.mesh.core.verticle.auth;

import static com.gentics.mesh.util.VerticleHelper.transformAndResponde;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.handler.InternalHttpActionContext;
import com.gentics.mesh.json.JsonUtil;

public abstract class AbstractAuthRestHandler extends AbstractHandler implements AuthenticationRestHandler {
	
	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	@Override
	public void handleMe(InternalHttpActionContext ac) {
		db.asyncNoTrx(tx -> {
			MeshAuthUser requestUser = ac.getUser();
			transformAndResponde(ac, requestUser, OK);
		} , ac.errorHandler());
	}
	
	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	@Override
	public void handleLogout(InternalHttpActionContext ac) {
		ac.logout();
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);
	}

}
