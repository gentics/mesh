package com.gentics.mesh.etc.config;

import static com.gentics.mesh.etc.config.env.OptionUtils.isEmpty;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

import io.vertx.core.http.ClientAuth;

/**
 * Mesh Http Server configuration POJO.
 */
@GenerateDocumentation
public class HttpServerConfig implements Option {

	public static final String DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN = "";

	public static final boolean DEFAULT_CORS_ALLOW_CREDENTIALS = false;

	public static final boolean DEFAULT_USE_ALPN = false;

	public static final String HTTP_PORT_KEY = "httpPort";

	public static final String DEFAULT_HTTP_HOST = "0.0.0.0";

	public static final int DEFAULT_HTTP_PORT = 8080;

	public static final int DEFAULT_HTTPS_PORT = 8443;

	public static final String DEFAULT_CERT_PATH = "config/cert.pem";
	public static final String DEFAULT_KEY_PATH = "config/key.pem";
	public static final ClientAuth DEFAULT_CLIENT_AUTH_MODE = ClientAuth.NONE;
	public static final boolean DEFAULT_SERVER_TOKENS = true;
	public static final boolean DEFAULT_MINIFY_JSON = true;
	public static final int DEFAULT_MAX_FORM_ATTRIBUTE_SIZE = -1;

	public static final String MESH_HTTP_PORT_ENV = "MESH_HTTP_PORT";
	public static final String MESH_HTTPS_PORT_ENV = "MESH_HTTPS_PORT";

	public static final String MESH_HTTP_HOST_ENV = "MESH_HTTP_HOST";
	public static final String MESH_HTTP_CORS_ORIGIN_PATTERN_ENV = "MESH_HTTP_CORS_ORIGIN_PATTERN";
	public static final String MESH_HTTP_CORS_ENABLE_ENV = "MESH_HTTP_CORS_ENABLE";
	public static final String MESH_HTTP_MINIFY_JSON_ENV = "MESH_HTTP_MINIFY_JSON";

	public static final String MESH_HTTP_SSL_ENABLE_ENV = "MESH_HTTP_SSL_ENABLE";
	public static final String MESH_HTTP_HTTP_ENABLE_ENV = "MESH_HTTP_ENABLE";
	public static final String MESH_HTTP_SSL_CERT_PATH_ENV = "MESH_HTTP_SSL_CERT_PATH";
	public static final String MESH_HTTP_SSL_KEY_PATH_ENV = "MESH_HTTP_SSL_KEY_PATH";
	public static final String MESH_HTTP_SSL_CLIENT_AUTH_MODE_ENV = "MESH_HTTP_SSL_CLIENT_AUTH_MODE";
	public static final String MESH_HTTP_SSL_TRUSTED_CERTS_ENV = "MESH_HTTP_SSL_TRUSTED_CERTS";
	public static final String MESH_HTTP_CORS_ALLOW_CREDENTIALS_ENV = "MESH_HTTP_CORS_ALLOW_CREDENTIALS";
	public static final String MESH_HTTP_USE_ALPN_ENV = "MESH_HTTP_USE_ALPN";
	public static final String MESH_HTTP_SERVER_TOKENS_ENV = "MESH_HTTP_SERVER_TOKENS";
	public static final String MESH_HTTP_SERVER_MAX_FORM_ATTRIBUTE_SIZE_ENV = "MESH_HTTP_SERVER_MAX_FORM_ATTRIBUTE_SIZE";

	public static final int DEFAULT_VERTICLE_AMOUNT = 2 * Runtime.getRuntime().availableProcessors();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh HTTP server port. Default is: " + DEFAULT_HTTP_PORT)
	@EnvironmentVariable(name = MESH_HTTP_PORT_ENV, description = "Override the configured server http port.")
	private int port = DEFAULT_HTTP_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh HTTPS server port. Default is: " + DEFAULT_HTTPS_PORT)
	@EnvironmentVariable(name = MESH_HTTPS_PORT_ENV, description = "Override the configured server https port.")
	private int sslPort = DEFAULT_HTTPS_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh HTTP server host to bind to. Default is: " + DEFAULT_HTTP_HOST)
	@EnvironmentVariable(name = MESH_HTTP_HOST_ENV, description = "Override the configured http server host which is used to bind to.")
	private String host = DEFAULT_HTTP_HOST;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configured CORS allowed origin pattern. You can specify a regex to include multiple hosts if you want to do so.")
	@EnvironmentVariable(name = MESH_HTTP_CORS_ORIGIN_PATTERN_ENV, description = "Override the configured CORS allowed origin pattern.")
	private String corsAllowedOriginPattern = DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether credentials are allowed to be passed along using CORS requests.")
	@EnvironmentVariable(name = MESH_HTTP_CORS_ALLOW_CREDENTIALS_ENV, description = "Override the configured CORS allowed credentials flag.")
	private Boolean corsAllowCredentials = DEFAULT_CORS_ALLOW_CREDENTIALS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether application-level protocol negotiation (aka ALPN) should be used. Normally HTTP/2 connections need this.")
	@EnvironmentVariable(name = MESH_HTTP_USE_ALPN_ENV, description = "Override the configured ALPN usage flag.")
	private boolean useAlpn = DEFAULT_USE_ALPN;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether CORS handling should be enabled.")
	@EnvironmentVariable(name = MESH_HTTP_CORS_ENABLE_ENV, description = "Override the configured CORS enable flag.")
	private Boolean enableCors = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether http server should be enabled. Default: true")
	@EnvironmentVariable(name = MESH_HTTP_HTTP_ENABLE_ENV, description = "Override the configured http server flag.")
	private boolean http = true;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether https server should be enabled. Default: false")
	@EnvironmentVariable(name = MESH_HTTP_SSL_ENABLE_ENV, description = "Override the configured https server flag.")
	private boolean ssl = false;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether SSL support be enabled.")
	@EnvironmentVariable(name = MESH_HTTP_SSL_CERT_PATH_ENV, description = "Override the configured SSL enable flag.")
	private String certPath = DEFAULT_CERT_PATH;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Path to the SSL private key. Default: " + DEFAULT_KEY_PATH)
	@EnvironmentVariable(name = MESH_HTTP_SSL_KEY_PATH_ENV, description = "Override the configured SSL enable flag.")
	private String keyPath = DEFAULT_KEY_PATH;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the client certificate handling mode. Options: none, request, required. Default: none")
	@EnvironmentVariable(name = MESH_HTTP_SSL_CLIENT_AUTH_MODE_ENV, description = "Override the configured client certificate handling mode.")
	private ClientAuth clientAuthMode = DEFAULT_CLIENT_AUTH_MODE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the trusted SSL certificates.")
	@EnvironmentVariable(name = MESH_HTTP_SSL_TRUSTED_CERTS_ENV, description = "Override the configured trusted SSL certificates.")
	private List<String> trustedCertPaths = new ArrayList<>();

	@JsonProperty(required = false)
	@JsonPropertyDescription("Amount of rest API verticles to be deployed. Default is 2 * CPU Cores")
	@EnvironmentVariable(name = "MESH_HTTP_VERTICLE_AMOUNT", description = "Override the http verticle amount.")
	private int verticleAmount = DEFAULT_VERTICLE_AMOUNT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Minify JSON responses to save the payload space. Default is true")
	@EnvironmentVariable(name = MESH_HTTP_MINIFY_JSON_ENV, description = "Override the minify JSON flag.")
	private boolean minifyJson = DEFAULT_MINIFY_JSON;

	@JsonProperty(defaultValue = "" + DEFAULT_MAX_FORM_ATTRIBUTE_SIZE)
	@JsonPropertyDescription("Set the maximum size of a form attribute, set to -1 for unlimited.")
	@EnvironmentVariable(name = MESH_HTTP_SERVER_MAX_FORM_ATTRIBUTE_SIZE_ENV, description = "Override the max form attribute size")
	private int maxFormAttributeSize = DEFAULT_MAX_FORM_ATTRIBUTE_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Set the http server tokens flag which controls whether the server should expose version information via headers, REST endpoints and GraphQL. Default is true")
	@EnvironmentVariable(name = MESH_HTTP_SERVER_TOKENS_ENV, description = "Override the http server tokens flag.")
	private boolean serverTokens = DEFAULT_SERVER_TOKENS;

	public HttpServerConfig() {
	}

	public String getHost() {
		return host;
	}

	@Setter
	public HttpServerConfig setHost(String host) {
		this.host = host;
		return this;
	}

	public int getPort() {
		return port;
	}

	@Setter
	public HttpServerConfig setPort(int port) {
		this.port = port;
		return this;
	}

	public int getSslPort() {
		return sslPort;
	}

	@Setter
	public HttpServerConfig setSslPort(int sslPort) {
		this.sslPort = sslPort;
		return this;
	}

	public Boolean getEnableCors() {
		return enableCors;
	}

	@JsonIgnore
	public boolean isCorsEnabled() {
		return this.enableCors != null && this.enableCors == true;
	}

	@Setter
	public HttpServerConfig setEnableCors(Boolean enableCors) {
		this.enableCors = enableCors;
		return this;
	}

	public String getCorsAllowedOriginPattern() {
		return this.corsAllowedOriginPattern;
	}

	@Setter
	public HttpServerConfig setCorsAllowedOriginPattern(String corsAllowedOriginPattern) {
		this.corsAllowedOriginPattern = corsAllowedOriginPattern;
		return this;
	}

	public boolean getCorsAllowCredentials() {
		return corsAllowCredentials;
	}

	@Setter
	public HttpServerConfig setCorsAllowCredentials(boolean allowCredentials) {
		this.corsAllowCredentials = allowCredentials;
		return this;
	}

	public boolean isSsl() {
		return ssl;
	}

	@Setter
	public HttpServerConfig setSsl(boolean ssl) {
		this.ssl = ssl;
		return this;
	}

	public boolean isHttp() {
		return http;
	}

	@Setter
	public HttpServerConfig setHttp(boolean http) {
		this.http = http;
		return this;
	}

	public String getCertPath() {
		return certPath;
	}

	@Setter
	public HttpServerConfig setCertPath(String certPath) {
		this.certPath = certPath;
		return this;
	}

	public String getKeyPath() {
		return keyPath;
	}

	@Setter
	public HttpServerConfig setKeyPath(String keyPath) {
		this.keyPath = keyPath;
		return this;
	}

	public int getVerticleAmount() {
		return verticleAmount;
	}

	@Setter
	public HttpServerConfig setVerticleAmount(int verticleAmount) {
		this.verticleAmount = verticleAmount;
		return this;
	}

	public ClientAuth getClientAuthMode() {
		return clientAuthMode;
	}

	@Setter
	public HttpServerConfig setClientAuthMode(ClientAuth clientAuthMode) {
		this.clientAuthMode = clientAuthMode;
		return this;
	}

	public List<String> getTrustedCertPaths() {
		return trustedCertPaths;
	}

	@Setter
	public HttpServerConfig setTrustedCertPaths(List<String> trustedCertPaths) {
		this.trustedCertPaths = trustedCertPaths;
		return this;
	}

	public boolean isServerTokens() {
		return serverTokens;
	}

	@Setter
	public HttpServerConfig setServerTokens(boolean flag) {
		this.serverTokens = flag;
		return this;
	}

	public int getMaxFormAttributeSize() {
		return maxFormAttributeSize;
	}

	@Setter
	public HttpServerConfig setMaxFormAttributeSize(int maxFormAttributeSize) {
		this.maxFormAttributeSize = maxFormAttributeSize;
		return this;
	}

	public boolean isUseAlpn() {
		return useAlpn;
	}

	@Setter
	public HttpServerConfig setUseAlpn(Boolean useAlpn) {
		this.useAlpn = useAlpn;
		return this;
	}

	public boolean isMinifyJson() {
		return minifyJson;
	}

	@Setter
	public HttpServerConfig setMinifyJson(boolean minifyJson) {
		this.minifyJson = minifyJson;
		return this;
	}

	/**
	 * Validate the settings.
	 */
	public void validate(MeshOptions meshOptions) {
		if (ssl && (isEmpty(getCertPath()) || isEmpty(getKeyPath()))) {
			throw new IllegalStateException("SSL is enabled but either the server key or the cert path was not specified.");
		}
		if (ssl && !Paths.get(getKeyPath()).toFile().exists()) {
			throw new IllegalStateException("Could not find SSL key within path {" + getKeyPath() + "}");
		}
		if (ssl && !Paths.get(getCertPath()).toFile().exists()) {
			throw new IllegalStateException("Could not find SSL cert within path {" + getCertPath() + "}");
		}
	}
}
