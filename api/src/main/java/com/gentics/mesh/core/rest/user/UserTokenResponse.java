package com.gentics.mesh.core.rest.user;

import com.gentics.mesh.core.rest.common.RestModel;

public class UserTokenResponse implements RestModel {

	private String token;

	private String created;

	/**
	 * Set the token.
	 * 
	 * @param token
	 * @return Fluent API
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

	/**
	 * Return the date on which the token was created.
	 * 
	 * @return
	 */
	public String getCreated() {
		return created;
	}

	/**
	 * Set the date on which the token was created.
	 * 
	 * @param created
	 * @return Fluent API
	 */
	public UserTokenResponse setCreated(String created) {
		this.created = created;
		return this;
	}
}
