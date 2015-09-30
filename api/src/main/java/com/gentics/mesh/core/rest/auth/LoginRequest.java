package com.gentics.mesh.core.rest.auth;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a basic login model.
 */
public class LoginRequest implements RestModel {

	private String username;
	private String password;

	/**
	 * Return the password.
	 * 
	 * @return
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Return the username.
	 * 
	 * @return
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the login username.
	 * 
	 * @param username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the login password.
	 * 
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
