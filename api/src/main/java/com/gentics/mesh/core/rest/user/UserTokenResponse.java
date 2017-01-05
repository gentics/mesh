package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.common.RestModel;

public class UserTokenResponse implements RestModel {

	private String token;

	/**
	 * Set the token.
	 * 
	 * @param token
	 * @return
	 */
	public UserTokenResponse setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * Return the token.
	 * 
	 * @return
	 */
	public String getToken() {
		return token;
	}

}
