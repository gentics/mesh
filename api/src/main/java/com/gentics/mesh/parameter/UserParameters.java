package com.gentics.mesh.parameter;

public interface UserParameters extends ParameterProvider {

	/**
	 * Return the token code which was set.
	 * 
	 * @return
	 */
	String getToken();

	/**
	 * Set the one time token which can be used to update the user even if the credentials don't allow it otherwise.
	 * 
	 * @param token
	 * @return Fluent API
	 */
	UserParameters setToken(String token);
}
