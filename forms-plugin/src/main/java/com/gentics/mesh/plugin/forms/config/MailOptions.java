package com.gentics.mesh.plugin.forms.config;

import io.vertx.ext.mail.StartTLSOptions;

/**
 * Mailserver configuration options.
 */
public class MailOptions {

	public static final int DEFAULT_PORT = 25;
	public static final String DEFAULT_HOST = "localhost";
	public static final int DEFAULT_MAX_POOL_SIZE = 10;
	public static final boolean DEFAULT_SSL = false;
	public static final boolean DEFAULT_TRUST_ALL = false;
	public static final StartTLSOptions DEFAULT_START_TLS = StartTLSOptions.OPTIONAL;
	public static final boolean DEFAULT_ALLOW_RCPT_ERRORS = false;
	public static final boolean DEFAULT_KEEP_ALIVE = true;
	public static final boolean DEFAULT_ATTACH_FILE_UPLOADS = false;
	public static final boolean DEFAULT_DISABLE_ESMTP = false;

	private String hostname = DEFAULT_HOST;

	private int port = DEFAULT_PORT;

	private String authMethods;

	private String username;

	private String password;

	private boolean ssl = DEFAULT_SSL;

	private boolean trustAll = DEFAULT_TRUST_ALL;

	private StartTLSOptions startTls = DEFAULT_START_TLS;

	private String ownHostname;

	private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

	private boolean keepAlive = DEFAULT_KEEP_ALIVE;

	private boolean attachFileUploads = DEFAULT_ATTACH_FILE_UPLOADS;


	public MailOptions() {
	}

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

	public String getAuthMethods() {
		return authMethods;
	}

	public void setAuthMethods(String authMethods) {
		this.authMethods = authMethods;
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

	public StartTLSOptions getStartTls() {
		return startTls;
	}

	public void setStartTls(StartTLSOptions startTls) {
		this.startTls = startTls;
	}

	public boolean isKeepAlive() {
		return keepAlive;
	}

	public void setKeepAlive(boolean keepAlive) {
		this.keepAlive = keepAlive;
	}

	public int getMaxPoolSize() {
		return maxPoolSize;
	}

	public void setMaxPoolSize(int maxPoolSize) {
		this.maxPoolSize = maxPoolSize;
	}

	public String getOwnHostname() {
		return ownHostname;
	}

	public void setOwnHostname(String ownHostname) {
		this.ownHostname = ownHostname;
	}

	public boolean isAttachFileUploads() {
		return attachFileUploads;
	}

	public void setAttachFileUploads(boolean attachFileUploads) {
		this.attachFileUploads = attachFileUploads;
	}

}
