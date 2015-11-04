package com.gentics.mesh.core.rest.auth;

/**
 * This response is returned when a new JWToken is requested
 * @author philippguertler
 */
public class TokenResponse {
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
}
