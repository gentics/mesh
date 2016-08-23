package com.gentics.mesh.core.verticle.auth;

import com.gentics.mesh.context.InternalActionContext;

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
