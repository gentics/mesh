package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import io.vertx.ext.mail.StartTLSOptions;

public class MailOptions {
	public static final String MAIL_OPTIONS_DEFAULT_HOSTNAME = "localhost";
	public static final int MAIL_OPTIONS_DEFAULT_PORT = 1025;
	public static final int MAIL_OPTIONS_DEFAULT_MAX_POOL_SIZE = 10;
	public static final boolean MAIL_OPTIONS_DEFAULT_SSL = false;
	public static final boolean MAIL_OPTIONS_DEFAULT_TRUST_ALL = false;
	public static final StartTLSOptions MAIL_OPTIONS_DEFAULT_START_TLS = StartTLSOptions.OPTIONAL;
	public static final boolean MAIL_OPTIONS_DEFAULT_KEEP_ALIVE = true;
	public static final String MAIL_OPTIONS_DEFAULT_OWN_HOSTNAME = "";
	public static final String MAIL_OPTIONS_DEFAULT_USERNAME = "";
	public static final String MAIL_OPTIONS_DEFAULT_PASSWORD = "";
	public static final int MAIL_OPTIONS_RETRY_IN_SECONDS = 5000;

	public static final String MAIL_OPTIONS_HOSTNAME_ENV = "hostname";
	public static final String MAIL_OPTIONS_PORT_ENV = "port";
	public static final String MAIL_OPTIONS_MAX_POOL_SIZE_ENV = "JobHandlermaxPoolSize";
	public static final String MAIL_OPTIONS_SSL_ENV = "ssl";
	public static final String MAIL_OPTIONS_TRUST_ALL_ENV = "trustAll";
	public static final String MAIL_OPTIONS_OWN_HOSTNAME_ENV = "ownHostname";
	public static final String MAIL_OPTIONS_USERNAME_ENV = "username";
	public static final String MAIL_OPTIONS_PASSWORD_ENV = "password";
	public static final String MAIL_OPTIONS_KEEP_ALIVE_ENV = "keepAlive";
	public static final String MAIL_OPTIONS_RETRY_IN_SECONDS_ENV = "retry";
	public static final String MAIL_OPTIONS_START_TLS_ENV = "startTSL";

	@JsonProperty(required = true)
	@JsonPropertyDescription("Configure the Mail server hostname. Default is: " + MAIL_OPTIONS_DEFAULT_HOSTNAME)
	@EnvironmentVariable(name = MAIL_OPTIONS_HOSTNAME_ENV, description = "Override the configured server hostname.")
	private String hostname = MAIL_OPTIONS_DEFAULT_HOSTNAME;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Configure the Mail server port. Default is: " + MAIL_OPTIONS_DEFAULT_PORT)
	@EnvironmentVariable(name = MAIL_OPTIONS_PORT_ENV, description = "Override the configured server port.")
	private int port = MAIL_OPTIONS_DEFAULT_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the max pool size. Default is: " + MAIL_OPTIONS_DEFAULT_MAX_POOL_SIZE)
	@EnvironmentVariable(name = MAIL_OPTIONS_MAX_POOL_SIZE_ENV, description = "Override the configured server max pool size.")
	private int maxPoolSize = MAIL_OPTIONS_DEFAULT_MAX_POOL_SIZE;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the server ssl. Default is: " + MAIL_OPTIONS_DEFAULT_SSL)
	@EnvironmentVariable(name = MAIL_OPTIONS_SSL_ENV, description = "Override the configured server ssl.")
	private boolean ssl = MAIL_OPTIONS_DEFAULT_SSL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the trust all variable. Default is: " + MAIL_OPTIONS_DEFAULT_TRUST_ALL)
	@EnvironmentVariable(name = MAIL_OPTIONS_TRUST_ALL_ENV, description = "Override the configured trust all variable.")
	private boolean trustAll = MAIL_OPTIONS_DEFAULT_TRUST_ALL;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the own hostname variable. Default is: " + MAIL_OPTIONS_DEFAULT_OWN_HOSTNAME)
	@EnvironmentVariable(name = MAIL_OPTIONS_OWN_HOSTNAME_ENV, description = "Override the configured own hostname variable.")
	private String ownHostname  = MAIL_OPTIONS_DEFAULT_OWN_HOSTNAME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the username. Default is: " + MAIL_OPTIONS_DEFAULT_USERNAME)
	@EnvironmentVariable(name = MAIL_OPTIONS_USERNAME_ENV, description = "Override the configured username.")
	private String username  = MAIL_OPTIONS_DEFAULT_USERNAME;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the own hostname variable. Default is: " + MAIL_OPTIONS_DEFAULT_PASSWORD)
	@EnvironmentVariable(name = MAIL_OPTIONS_PASSWORD_ENV, description = "Override the configured passord.")
	private String password  = MAIL_OPTIONS_DEFAULT_PASSWORD;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the keep alive variable. Default is: " + MAIL_OPTIONS_DEFAULT_KEEP_ALIVE)
	@EnvironmentVariable(name = MAIL_OPTIONS_KEEP_ALIVE_ENV, description = "Override the configured keep alive variable.")
	private Boolean iskeepAlive  = MAIL_OPTIONS_DEFAULT_KEEP_ALIVE;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Configure the Retry Variable in seconds. Default is: " + MAIL_OPTIONS_RETRY_IN_SECONDS)
	@EnvironmentVariable(name = MAIL_OPTIONS_RETRY_IN_SECONDS_ENV, description = "Override the configured Retry Variable.")
	private int retry  = MAIL_OPTIONS_RETRY_IN_SECONDS;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure Start TLS Variable. Default is: StartTLSOptions.OPTIONAL")
	@EnvironmentVariable(name = MAIL_OPTIONS_START_TLS_ENV, description = "Override the configured start TLS Variable.")
	private StartTLSOptions startTls  = MAIL_OPTIONS_DEFAULT_START_TLS;

	public String getHostname() {
		return hostname;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public boolean isSsl() {
		return ssl;
	}

	public void setSsl(boolean ssl) {
		this.ssl = ssl;
	}

	public boolean isTrustAll() {
		return trustAll;
	}

	public void setTrustAll(boolean trustAll) {
		this.trustAll = trustAll;
	}
	public String getOwnHostname() {
		return ownHostname;
	}

	public void setOwnHostname(String ownHostname) {
		this.ownHostname = ownHostname;
	}
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
	public Boolean getIskeepAlive() {
		return iskeepAlive;
	}

	public void setIskeepAlive(Boolean iskeepAlive) {
		this.iskeepAlive = iskeepAlive;
	}

	public Integer getRetry() {
		return retry;
	}

	public void setRetry(Integer retry) {
		this.retry = retry;
	}

}
