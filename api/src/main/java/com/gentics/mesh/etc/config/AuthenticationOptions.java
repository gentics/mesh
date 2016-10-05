package com.gentics.mesh.etc.config;

/**
 * Authentication options POJO
 */
public class AuthenticationOptions {

	private JWTAuthenticationOptions jwtAuthenticationOptions = new JWTAuthenticationOptions();

	/**
	 * Gets the JWT authentication options
	 * 
	 * @return
	 */
	public JWTAuthenticationOptions getJwtAuthenticationOptions() {
		return jwtAuthenticationOptions;
	}

	/**
	 * Sets the JWT authentication options
	 * 
	 * @param jwtAuthenticationOptions
	 */
	public void setJwtAuthenticationOptions(JWTAuthenticationOptions jwtAuthenticationOptions) {
		this.jwtAuthenticationOptions = jwtAuthenticationOptions;
	}

}
