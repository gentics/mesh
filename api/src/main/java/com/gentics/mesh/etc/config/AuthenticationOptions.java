package com.gentics.mesh.etc.config;

import static com.gentics.mesh.MeshEnv.CONFIG_FOLDERNAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

import io.vertx.core.json.JsonObject;

/**
 * Authentication options POJO.
 */
@GenerateDocumentation
public class AuthenticationOptions implements Option {

	public static final String DEFAULT_ALGORITHM = "HS256";
	public static final String DEFAULT_ISSUER = null;//"Gentics Mesh";

	public static final int DEFAULT_TOKEN_EXPIRATION_TIME = 60 * 60; // 1 hour
	public static final int DEFAULT_LEEWAY = 0;

	public static final boolean DEFAULT_IGNORE_EXPIRATION = false;

	public static final String DEFAULT_KEYSTORE_PATH = CONFIG_FOLDERNAME + "/keystore.jceks";

	public static final String DEFAULT_PUBLIC_KEYS_PATH = CONFIG_FOLDERNAME + "/public-keys.json";

	public static final String MESH_AUTH_TOKEN_EXP_ENV = "MESH_AUTH_TOKEN_EXP";
	public static final String MESH_AUTH_KEYSTORE_PASS_ENV = "MESH_AUTH_KEYSTORE_PASS";
	public static final String MESH_AUTH_KEYSTORE_PATH_ENV = "MESH_AUTH_KEYSTORE_PATH";
	public static final String MESH_AUTH_JWT_ALGO_ENV = "MESH_AUTH_JWT_ALGO";
	public static final String MESH_AUTH_JWT_LEEWAY_ENV = "MESH_AUTH_JWT_LEEWAY";
	public static final String MESH_AUTH_JWT_AUDIENCE_ENV = "MESH_AUTH_JWT_AUDIENCE";
	public static final String MESH_AUTH_JWT_ISSUER_ENV = "MESH_AUTH_JWT_ISSUER";
	public static final String MESH_AUTH_JWT_IGNORE_EXPIRATION_ENV = "MESH_AUTH_JWT_IGNORE_EXPIRATION";
	public static final String MESH_AUTH_ANONYMOUS_ENABLED_ENV = "MESH_AUTH_ANONYMOUS_ENABLED";
	public static final String MESH_AUTH_PUBLIC_KEYS_PATH_ENV = "MESH_AUTH_PUBLIC_KEYS_PATH";

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
	@JsonPropertyDescription("Leeway (in seconds) of how long a JWT should still be considered valid.")
	@EnvironmentVariable(name = MESH_AUTH_JWT_LEEWAY_ENV, description = "Override the configured Leeway (in seconds) of how long a JWT should still be considered valid.")
	private int leeway = DEFAULT_LEEWAY;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The issuer of the JWT which is also written into the token.")
	@EnvironmentVariable(name = MESH_AUTH_JWT_ISSUER_ENV, description = "Override the configured issuer of the JWT.")
	private String issuer = DEFAULT_ISSUER;

	@JsonProperty(required = false)
	@JsonPropertyDescription("The expected audience of the JWT.")
	@EnvironmentVariable(name = MESH_AUTH_JWT_AUDIENCE_ENV, description = "Override the configured audience of the JWT.")
	private List<String> audience = null;

	@JsonProperty(required = false)
	@JsonPropertyDescription("If expired JWT should still be accepted and processed.")
	@EnvironmentVariable(name = MESH_AUTH_JWT_IGNORE_EXPIRATION_ENV, description = "Overrides if an expired JWT should still be accepted and processed.")
	private boolean ignoreExpiration = DEFAULT_IGNORE_EXPIRATION;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether anonymous access should be enabled.")
	@EnvironmentVariable(name = MESH_AUTH_ANONYMOUS_ENABLED_ENV, description = "Override the configured anonymous enabled flag.")
	private boolean enableAnonymousAccess = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the public keys file which contains a list of additional JWK formatted public keys which will be used to verify JWTs.")
	@EnvironmentVariable(name = MESH_AUTH_PUBLIC_KEYS_PATH_ENV, description = "Override the configured public keys file path.")
	private String publicKeysPath = DEFAULT_PUBLIC_KEYS_PATH;

	@JsonIgnore
	private List<JsonObject> publicKeys = new ArrayList<>();

	public List<JsonObject> getPublicKeys() {
		return publicKeys;
	}

	public AuthenticationOptions setPublicKeys(Collection<JsonObject> keys) {
		this.publicKeys = keys.stream()
			.collect(Collectors.toList());
		return this;
	}

	public AuthenticationOptions setPublicKey(JsonObject jwk) {
		this.publicKeys = Arrays.asList(jwk);
		return this;
	}

	public String getPublicKeysPath() {
		return publicKeysPath;
	}

	public AuthenticationOptions setPublicKeysPath(String publicKeysPath) {
		this.publicKeysPath = publicKeysPath;
		return this;
	}

	public int getTokenExpirationTime() {
		return tokenExpirationTime;
	}

	public AuthenticationOptions setTokenExpirationTime(int tokenExpirationTime) {
		this.tokenExpirationTime = tokenExpirationTime;
		return this;
	}

	public String getKeystorePassword() {
		return keystorePassword;
	}

	public AuthenticationOptions setKeystorePassword(String password) {
		this.keystorePassword = password;
		return this;
	}

	public String getKeystorePath() {
		return keystorePath;
	}

	public AuthenticationOptions setKeystorePath(String keystorePath) {
		this.keystorePath = keystorePath;
		return this;
	}

	public String getAlgorithm() {
		return algorithm;
	}

	public AuthenticationOptions setAlgorithm(String algorithm) {
		this.algorithm = algorithm;
		return this;
	}

	public int getLeeway() {
		return leeway;
	}

	public AuthenticationOptions setLeeway(int leeway) {
		this.leeway = leeway;
		return this;
	}

	public String getIssuer() {
		return issuer;
	}

	public AuthenticationOptions setIssuer(String issuer) {
		this.issuer = issuer;
		return this;
	}

	public List<String> getAudience() {
		return audience;
	}

	public AuthenticationOptions setAudience(List<String> audience) {
		this.audience = audience;
		return this;
	}

	public boolean isIgnoreExpiration() {
		return ignoreExpiration;
	}

	public AuthenticationOptions setIgnoreExpiration(boolean ignoreExpiration) {
		this.ignoreExpiration = ignoreExpiration;
		return this;
	}

	public boolean isEnableAnonymousAccess() {
		return enableAnonymousAccess;
	}

	public AuthenticationOptions setEnableAnonymousAccess(boolean enableAnonymousAccess) {
		this.enableAnonymousAccess = enableAnonymousAccess;
		return this;
	}

	public void validate(MeshOptions meshOptions) {
		Objects.requireNonNull(getKeystorePassword(), "The keystore password was not specified.");
		Objects.requireNonNull(keystorePath, "The keystore path cannot be null.");
		if (keystorePath.trim().isEmpty()) {
			throw new IllegalArgumentException("The keystore path cannot be empty");
		}
		// Validate the JWK's
		if (publicKeys != null) {
			for (JsonObject key : publicKeys) {
				Objects.requireNonNull(key.getString("kty"), "The provided JWK has no kty (type).");
				// TODO check whether we should also check the use property
			}
		}
	}

}
