package com.gentics.mesh.etc.config;

import org.apache.commons.lang.BooleanUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Mesh Http Server configuration POJO.
 */
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

	/**
	 * Return the flag which indicates whether SSL should be enabled. By default SSL is not enabled.
	 * 
	 * @return Ssl enabled flag
	 */
	public boolean isSsl() {
		return BooleanUtils.isTrue(ssl);
	}

	/**
	 * Set the SSL enabled flag.
	 * 
	 * @param ssl
	 *            Ssl enabled flag
	 */
	public void setSsl(Boolean ssl) {
		this.ssl = ssl;
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
	 */
	public void setPort(int port) {
		this.port = port;
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
	 * Set the flag which will enable cors.
	 * 
	 * @param enableCors
	 *            CORS enabled flag
	 */
	public void setEnableCors(Boolean enableCors) {
		this.enableCors = enableCors;
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
	 */
	public void setCorsAllowedOriginPattern(String corsAllowedOriginPattern) {
		this.corsAllowedOriginPattern = corsAllowedOriginPattern;
	}

	/**
	 * Return the path to the PEM style server key file.
	 * 
	 * @return Path to pem style key file
	 */
	public String getKeyPath() {
		return keyPath;
	}

	/**
	 * Set the PEM style server key file.
	 * 
	 * @param keyPath
	 *            Path to pem style key file
	 */
	public void setKeyPath(String keyPath) {
		this.keyPath = keyPath;
	}

	/**
	 * Return the path to the PEM style server cert file.
	 * 
	 * @return Path to pem style cert file
	 */
	public String getCertPath() {
		return certPath;
	}

	/**
	 * Set the path to the PEM style server cert file.
	 * 
	 * @param certPath
	 *            Path to pem style cert file
	 */
	public void setCertPath(String certPath) {
		this.certPath = certPath;
	}

}
