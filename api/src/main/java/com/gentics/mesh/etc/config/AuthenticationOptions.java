package com.gentics.mesh.etc.config;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Authentication options POJO.
 */
@GenerateDocumentation
public class AuthenticationOptions implements Option {

	public static final String DEFAULT_ALGORITHM = "HS256";

	public static final int DEFAULT_TOKEN_EXPIRATION_TIME = 60 * 60; // 1 hour

	public static final String DEFAULT_KEYSTORE_PATH = CONFIG_FOLDERNAME + "/keystore.jceks";

	public static final String MESH_AUTH_TOKEN_EXP_ENV = "MESH_AUTH_TOKEN_EXP";
	public static final String MESH_AUTH_KEYSTORE_PASS_ENV = "MESH_AUTH_KEYSTORE_PASS";
	public static final String MESH_AUTH_KEYSTORE_PATH_ENV = "MESH_AUTH_KEYSTORE_PATH";
	public static final String MESH_AUTH_JWT_ALGO_ENV = "MESH_AUTH_JWT_ALGO";
	public static final String MESH_AUTH_ANONYMOUS_ENABLED_ENV = "MESH_AUTH_ANONYMOUS_ENABLED";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Time in minutes which an issued token stays valid.")
	@EnvironmentVariable(name = MESH_AUTH_TOKEN_EXP_ENV, description = "Override the configured JWT expiration time.")
	private int tokenExpirationTime = DEFAULT_TOKEN_EXPIRATION_TIME;

	@JsonProperty(required = true)
	@JsonPropertyDescription("The Java keystore password for the keystore file.")
	@EnvironmentVariable(name = MESH_AUTH_KEYSTORE_PASS_ENV, description = "Override the configured keystore password.")
	private String keystorePassword = null;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Path to the java keystore file which will be used to store cryptographic keys.")
	@EnvironmentVariable(name = MESH_AUTH_KEYSTORE_PATH_ENV, description = "Override the configured keystore path.")
	private String keystorePath = DEFAULT_KEYSTORE_PATH;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Algorithm which is used to verify and sign JWT.")
	@EnvironmentVariable(name = MESH_AUTH_JWT_ALGO_ENV, description = "Override the configured algorithm which is used to sign the JWT.")
	private String algorithm = DEFAULT_ALGORITHM;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether anonymous access should be enabled.")
	@EnvironmentVariable(name = MESH_AUTH_ANONYMOUS_ENABLED_ENV, description = "Override the configured anonymous enabled flag.")
	private boolean enableAnonymousAccess = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("OAuth2 related configuration options.")
	private OAuth2Options oauth2 = new OAuth2Options();

	/**
	 * Gets the time after which an authentication token should expire.
	 * 
	 * @return The expiration time in seconds
	 */
	public int getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	/**
	 * Sets the time after which an authentication token should expire.
	 * 
	 * @param tokenExpirationTime
	 *            The expiration time in seconds
	 * @return Fluent API
	 */
	public AuthenticationOptions setTokenExpirationTime(int tokenExpirationTime) {
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

	public boolean isEnableAnonymousAccess() {
		return enableAnonymousAccess;
	}

	public AuthenticationOptions setEnableAnonymousAccess(boolean enableAnonymousAccess) {
		this.enableAnonymousAccess = enableAnonymousAccess;
		return this;
	}

	public OAuth2Options getOauth2() {
		return oauth2;
	}

	public AuthenticationOptions setOauth2(OAuth2Options oauth2) {
		this.oauth2 = oauth2;
		return this;
	}

	public void validate(MeshOptions meshOptions) {
		Objects.requireNonNull(getKeystorePassword(), "The keystore password was not specified.");
		Objects.requireNonNull(keystorePath, "The keystore path cannot be null.");
		if (keystorePath.trim().isEmpty()) {
			throw new IllegalArgumentException("The keystore path cannot be empty");
		}
	}
}
