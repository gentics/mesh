package com.gentics.mesh.parameter;

/**
 * Definition for user specific query parameters.
 */
public interface UserParameters extends ParameterProvider {

	public static final String TOKEN_PARAMETER_KEY = "token";

	/**
	 * Return the token code which was set.
	 * 
	 * @return
	 */
	default String getToken() {
		return getParameter(TOKEN_PARAMETER_KEY);
	}

	/**
	 * Set the one time token which can be used to update the user even if the credentials don't allow it otherwise.
	 * 
	 * @param token
	 * @return Fluent API
	 */
	default UserParameters setToken(String token) {
		setParameter(TOKEN_PARAMETER_KEY, token);
		return this;
	}
}
