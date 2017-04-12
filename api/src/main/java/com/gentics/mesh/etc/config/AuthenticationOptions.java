package com.gentics.mesh.etc.config;

/**
 * Authentication options POJO.
 */
public class AuthenticationOptions {

	public static final String DEFAULT_ALGORITHM = "HS256";

	public static final long DEFAULT_TOKEN_EXPIRATION_TIME = 60 * 60; // 1 hour

	public static final String DEFAULT_KEYSTORE_PATH = "keystore.jceks";

	public static final String DEFAULT_KEYSTORE_PASSWORD = "secret";

	private long tokenExpirationTime = DEFAULT_TOKEN_EXPIRATION_TIME;

	private String keystorePassword = DEFAULT_KEYSTORE_PASSWORD;

	private String keystorePath = DEFAULT_KEYSTORE_PATH;

	private String algorithm = DEFAULT_ALGORITHM;

	/**
	 * Gets the time after which an authentication token should expire.
	 * 
	 * @return The expiration time in seconds
	 */
	public long getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	/**
	 * Sets the time after which an authentication token should expire.
	 * 
	 * @param tokenExpirationTime
	 *            The expiration time in seconds
	 * @return Fluent API
	 */
	public AuthenticationOptions setTokenExpirationTime(long tokenExpirationTime) {
		this.tokenExpirationTime = tokenExpirationTime;
		return this;
	}

	/**
	 * Gets the password which is used to open the java key store.
	 * 
	 * @return Keystore password
	 */
	public String getKeystorePassword() {
		return keystorePassword;
	}

	/**
	 * Sets the password which is used to open the java key store.
	 * 
	 * @param password
	 * @return Fluent API
	 * 
	 */
	public AuthenticationOptions setKeystorePassword(String password) {
		this.keystorePassword = password;
		return this;
	}

	/**
	 * Gets the path to the keystore file.
	 * 
	 * @return Path to keystore
	 */
	public String getKeystorePath() {
		return keystorePath;
	}

	/**
	 * Sets the path to the keystore file.
	 * 
	 * @param keystorePath
	 * @return Fluent API
	 * 
	 */
	public AuthenticationOptions setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
		return this;
	}

	/**
	 * Return the algorithm which is used to sign the JWT tokens. By default {@value #DEFAULT_ALGORITHM} is used.
	 * 
	 * @return Configured algorithm
	 */
	public String getAlgorithm() {
		return algorithm;
	}

	/**
	 * Set the algorithm which is used to sign the JWT tokens.
	 * 
	 * @param algorithm
	 * @return Fluent API
	 */
	public AuthenticationOptions setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
		return this;
	}
}
