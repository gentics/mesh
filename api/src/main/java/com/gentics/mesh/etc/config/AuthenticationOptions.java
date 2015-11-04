package com.gentics.mesh.etc.config;

/**
 * Authentication options POJO
 * @author philippguertler
 */
public class AuthenticationOptions {
	
	private long tokenExpirationTime;

	/**
	 * Gets the time after which an authentication token should expire.
	 * @return The expiration time in seconds
	 */
	public long getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	/**
	 * Sets the time after which an authentication token should expire.
	 * @param tokenExpirationTime The expiration time in seconds
	 */
	public void setTokenExpirationTime(long tokenExpirationTime) {
		this.tokenExpirationTime = tokenExpirationTime;
	}
}
