package com.gentics.mesh.auth;

import io.vertx.ext.auth.User;

/**
 * POJO for an authentication result.
 */
public class AuthenticationResult {

	private User user;

	private boolean usingAPIKey = false;

	public AuthenticationResult(User user) {
		this.user = user;
	}

	/**
	 * Return the set user.
	 * 
	 * @return
	 */
	public User getUser() {
		return user;
	}

	/**
	 * Retrieve a flag which indicates whether a API key/token was used to authenticate the user.
	 * 
	 * @return
	 */
	public boolean isUsingAPIKey() {
		return usingAPIKey;
	}

	public void setUsingAPIKey(boolean usingAPIKey) {
		this.usingAPIKey = usingAPIKey;
	}

}
