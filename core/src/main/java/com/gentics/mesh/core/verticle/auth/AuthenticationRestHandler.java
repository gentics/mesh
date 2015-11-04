package com.gentics.mesh.core.verticle.auth;

import org.springframework.stereotype.Component;

import com.gentics.mesh.handler.InternalActionContext;

@Component
public interface AuthenticationRestHandler {

	/**
	 * Handle a <code>/me</code> request which will return the current user as a JSON response.
	 * 
	 * @param ac
	 */
	public void handleMe(InternalActionContext ac);

	/**
	 * Handle a login request.
	 * 
	 * @param ac
	 */
	public void handleLogin(InternalActionContext ac);
	
	/**
	 * Handle a logout request.
	 * 
	 * @param ac
	 */
	public void handleLogout(InternalActionContext ac);

}
