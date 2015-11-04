package com.gentics.mesh.core.verticle.auth;

import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.verticle.handler.AbstractHandler;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.jwt.JWTAuth;

public class JWTAuthRestHandler extends AbstractHandler implements AuthenticationRestHandler{

	@Override
	public void handleMe(InternalActionContext ac) {
		
	}

	@Override
	public void handleLogin(InternalActionContext ac) {
		AuthProvider _prov = springConfiguration.authProvider();
		JWTAuth prov;
		if (_prov instanceof JWTAuth) {
			prov = (JWTAuth)_prov;
		} else {
			//TODO Proper error handling
			ac.fail(new Exception("Invalid AuthProvider. Make sure you are using JWTAuth."));
		}
		
	}

	@Override
	public void handleLogout(InternalActionContext ac) {
		GenericMessageResponse message = new GenericMessageResponse("OK");
		ac.send(JsonUtil.toJson(message), OK);		
	}
	
}
