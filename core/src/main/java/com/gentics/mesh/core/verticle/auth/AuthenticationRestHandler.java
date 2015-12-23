package com.gentics.mesh.core.verticle.auth;

import org.springframework.stereotype.Component;

import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.handler.InternalHttpActionContext;

@Component
public interface AuthenticationRestHandler {

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	public void handleMe(InternalHttpActionContext ac);

	/**
	 * Handle a login request.
	 * 
	 * @param ac
	 */
	public void handleLogin(InternalHttpActionContext ac);
	
	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	public void handleLogout(InternalHttpActionContext ac);

}
