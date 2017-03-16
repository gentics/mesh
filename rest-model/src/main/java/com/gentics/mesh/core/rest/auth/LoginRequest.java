package com.gentics.mesh.core.rest.auth;

import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a basic login model.
 */
public class LoginRequest implements RestModel {

	private String username;
	private String password;

	public LoginRequest() {
	}
	
	/**
	 * Return the password.
	 * 
	 * @return Password to be used for login
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Return the username.
	 * 
	 * @return Username to be used for login
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Set the login username.
	 * 
	 * @param username
	 *            Username to be used for login
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the login password.
	 * 
	 * @param password
	 *            Password to be used for login
	 */
	public void setPassword(String password) {
		this.password = password;
	}

}
