package com.gentics.mesh.core.rest.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * This response is returned when a new JWToken is requested.
 */
public class TokenResponse implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("A current JWT access token")
	private String token;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public TokenResponse(String token) {
		this.token = token;
	}
	
	public TokenResponse() {}
}
