package com.gentics.mesh.core.rest.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST POJO for a user reset token response.
 */
public class UserResetTokenResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("JSON Web Token which was issued by the API.")
	private String token;

	@JsonProperty(required = true)
	@JsonPropertyDescription("ISO8601 date of the creation date for the provided token")
	private String created;

	/**
	 * Set the token.
	 * 
	 * @param token
	 * @return Fluent API
	 */
	public UserResetTokenResponse setToken(String token) {
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
	public UserResetTokenResponse setCreated(String created) {
		this.created = created;
		return this;
	}
}
