package com.gentics.mesh.parameter;

/**
 * Interface for binary check update requests.
 */
public interface BinaryCheckParameters extends ParameterProvider {

	String SECRET_PARAMETER_KEY = "secret";

	/**
	 * Get the secret parameter.
	 *
	 * @return The secret parameter.
	 */
	default String getSecret() {
		return getParameter(SECRET_PARAMETER_KEY);
	}

	/**
	 * Set the {@code secret} parameter.
	 *
	 * @param secret The secret parameter.
	 * @return Fluent API.
	 */
	default BinaryCheckParameters setSecret(String secret) {
		setParameter(SECRET_PARAMETER_KEY, secret);

		return this;
	}
}
