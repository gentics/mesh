package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;

/**
 * Mesh Http Server configuration POJO.
 */
@GenerateDocumentation
public class HttpServerConfig {

	public static final String DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN = "";

	public static final String HTTP_PORT_KEY = "httpPort";

	public static final int DEFAULT_HTTP_PORT = 8080;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh HTTP server port.")
	private int port = DEFAULT_HTTP_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configured CORS allowed origin pattern. You can specify a regex to include multiple hosts if you want to do so.")
	private String corsAllowedOriginPattern = DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Flag which indicates whether CORS handling should be enabled.")
	private Boolean enableCors = false;

	public HttpServerConfig() {
	}

	/**
	 * Return the http server port.
	 * 
	 * @return Http server port
	 */
	public int getPort() {
		return port;
	}

	/**
	 * Set the http server port.
	 * 
	 * @param port
	 *            Http server port
	 * @return Fluent API
	 */
	public HttpServerConfig setPort(int port) {
		this.port = port;
		return this;
	}

	/**
	 * Return the CORS enabled flag. By default CORS is disabled.
	 * 
	 * @return CORS enabled flag
	 */
	public Boolean getEnableCors() {
		return enableCors;
	}

	/**
	 * Set the flag which will enable CORS.
	 * 
	 * @param enableCors
	 *            CORS enabled flag
	 * @return Fluent API
	 */
	public HttpServerConfig setEnableCors(Boolean enableCors) {
		this.enableCors = enableCors;
		return this;
	}

	/**
	 * Return the CORS flag.
	 * 
	 * @return CORS enabled flag
	 */
	@JsonIgnore
	public boolean isCorsEnabled() {
		return this.enableCors != null && this.enableCors == true;
	}

	/**
	 * Return the CORS allowed origin pattern.
	 * 
	 * @return CORS allowed pattern
	 */
	public String getCorsAllowedOriginPattern() {
		return this.corsAllowedOriginPattern;
	}

	/**
	 * Set the CORS allowed origin pattern.
	 * 
	 * @param corsAllowedOriginPattern
	 *            CORS allowed pattern
	 * @return Fluent API
	 */
	public HttpServerConfig setCorsAllowedOriginPattern(String corsAllowedOriginPattern) {
		this.corsAllowedOriginPattern = corsAllowedOriginPattern;
		return this;
	}

}
