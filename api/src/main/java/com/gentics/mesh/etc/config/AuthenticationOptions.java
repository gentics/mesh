package com.gentics.mesh.etc.config;

/**
 * Authentication options POJO
 * 
 * @author philippguertler
 */
public class AuthenticationOptions {
	public static enum AuthenticationMethod {
		BASIC_AUTH, JWT
	}

	public static final AuthenticationMethod DEFAULT_AUTHENTICATION_METHOD = AuthenticationMethod.BASIC_AUTH;

	private AuthenticationMethod authenticationMethod = DEFAULT_AUTHENTICATION_METHOD;

	private JWTAuthenticationOptions jwtAuthenticationOptions = new JWTAuthenticationOptions();

	/**
	 * Gets the authentication method
	 * 
	 * @return
	 */
	public AuthenticationMethod getAuthenticationMethod() {
		return authenticationMethod;
	}

	/**
	 * Sets the authentication method
	 * 
	 * @param authenticationMethod
	 */
	public void setAuthenticationMethod(AuthenticationMethod authenticationMethod) {
		this.authenticationMethod = authenticationMethod;
	}

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
