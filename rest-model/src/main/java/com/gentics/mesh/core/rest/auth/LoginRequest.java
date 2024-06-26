package com.gentics.mesh.core.rest.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a basic login model.
 */
public class LoginRequest implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Username of the user which should be logged in.")
	private String username;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Password of the user which should be logged in.")
	private String password;

	@JsonProperty(required = false)
	@JsonPropertyDescription("New password that will be set after successful login.")
	private String newPassword;

	@JsonProperty(required = false)
	@JsonPropertyDescription("API key of the user to log in.")
	private String apikey;

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

	/**
	 * Return the new password.
	 *
	 * @return
	 */
	public String getNewPassword() {
		return newPassword;
	}

	/**
	 * Set the new password.
	 *
	 * @param newPassword
	 * @return
	 */
	public LoginRequest setNewPassword(String newPassword) {
		this.newPassword = newPassword;
		return this;
	}

	/**
	 * Get API key.
	 * 
	 * @return
	 */
	public String getApiKey() {
		return apikey;
	}

	/**
	 * Set API key
	 * @param apikey
	 * @return fluent
	 */
	public LoginRequest setApiKey(String apikey) {
		this.apikey = apikey;
		return this;
	}
}
