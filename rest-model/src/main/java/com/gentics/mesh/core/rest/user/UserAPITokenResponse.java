package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * POJO for a REST API token response.
 */
public class UserAPITokenResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Issued client API token.")
	private String token;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Data of the created API token.")
	private UserAPITokenDataModel data;

	public UserAPITokenResponse() {
	}

	/**
	 * Return the API token.
	 * 
	 * @return
	 */
	public String getToken() {
		return token;
	}

	/**
	 * Set the token for the response.
	 * 
	 * @param token
	 * @return Fluent API
	 */
	public UserAPITokenResponse setToken(String token) {
		this.token = token;
		return this;
	}

	/**
	 * Return the API token data
	 * @return data
	 */
	public UserAPITokenDataModel getData() {
		return data;
	}

	/**
	 * Set the API token data
	 * @param data token data
	 * @return fluent API
	 */
	public UserAPITokenResponse setData(UserAPITokenDataModel data) {
		this.data = data;
		return this;
	}
}
