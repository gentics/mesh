package com.gentics.mesh.etc.config;

import org.apache.commons.lang.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class HttpServerConfig {

	public static final String DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN = "NOT_SET";

	public static final String HTTP_PORT_KEY = "httpPort";
	public static final int DEFAULT_HTTP_PORT = 8080;

	private int port = DEFAULT_HTTP_PORT;

	private Boolean ssl = false;

	private String corsAllowedOriginPattern = DEFAULT_CORS_ALLOWED_ORIGIN_PATTERN;

	private Boolean enableCors = false;
	private String certPath;
	private String keyPath;

	public HttpServerConfig() {
	}

	public boolean isSsl() {
		return BooleanUtils.isTrue(ssl);
	}

	public void setSsl(Boolean ssl) {
		this.ssl = ssl;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public Boolean getEnableCors() {
		return enableCors;
	}

	public void setEnableCors(Boolean enableCors) {
		this.enableCors = enableCors;
	}

	@JsonIgnore
	public boolean isCorsEnabled() {
		return this.enableCors != null && this.enableCors == true;
	}

	public String getCorsAllowedOriginPattern() {
		return this.corsAllowedOriginPattern;
	}

	public void setCorsAllowedOriginPattern(String corsAllowedOriginPattern) {
		this.corsAllowedOriginPattern = corsAllowedOriginPattern;
	}

	/**
	 * Return the path to the PEM style server key file.
	 * 
	 * @return
	 */
	public String getKeyPath() {
		return keyPath;
	}

	public void setKeyPath(String keyPath) {
		this.keyPath = keyPath;
	}

	/**
	 * Return the path to the PEM style server cert file.
	 * 
	 * @return
	 */
	public String getCertPath() {
		return certPath;
	}

	public void setCertPath(String certPath) {
		this.certPath = certPath;
	}

}
