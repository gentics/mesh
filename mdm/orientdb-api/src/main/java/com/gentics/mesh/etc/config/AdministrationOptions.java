package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

public class AdministrationOptions implements Option {

	public static final String MESH_DB_ADMIN_HTTP_PORT_ENV = "MESH_DB_ADMIN_HTTP_PORT";
	public static final String MESH_DB_ADMIN_HTTP_HOST_ENV = "MESH_DB_ADMIN_HTTP_HOST";
	public static final String MESH_DB_ADMIN_ENABLED_ENV = "MESH_DB_ADMIN_ENABLED";
	public static final String MESH_DB_ADMIN_LOCAL_ONLY_ENV = "MESH_DB_ADMIN_LOCAL_ONLY";

	public static final boolean DEFAULT_DB_ADMIN_ENABLED = false;
	public static final boolean DEFAULT_DB_ADMIN_LOCAL_ONLY = true;
	public static final int DEFAULT_DB_ADMIN_HTTP_PORT = 8082;
	public static final String DEFAULT_DB_ADMIN_HTTP_HOST = "127.0.0.1";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enable or disable the database administration system. Default is: " + DEFAULT_DB_ADMIN_ENABLED)
	@EnvironmentVariable(name = MESH_DB_ADMIN_ENABLED_ENV, description = "Override the configured database administration enabled flag.")
	private boolean enabled = DEFAULT_DB_ADMIN_ENABLED;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh database administration HTTP server port. Default is: " + DEFAULT_DB_ADMIN_HTTP_PORT)
	@EnvironmentVariable(name = MESH_DB_ADMIN_HTTP_PORT_ENV, description = "Override the configured database administration server http port.")
	private int port = DEFAULT_DB_ADMIN_HTTP_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh database administration HTTP server host to bind to. Default is: " + DEFAULT_DB_ADMIN_HTTP_HOST)
	@EnvironmentVariable(name = MESH_DB_ADMIN_HTTP_HOST_ENV, description = "Override the configured database administration http server host which is used to bind to.")
	private String host = DEFAULT_DB_ADMIN_HTTP_HOST;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Whether to allow administration connections from the same ip address. Default is: " + DEFAULT_DB_ADMIN_LOCAL_ONLY)
	@EnvironmentVariable(name = MESH_DB_ADMIN_LOCAL_ONLY_ENV, description = "Override the configured database administration local-only flag.")
	private boolean localOnly = DEFAULT_DB_ADMIN_LOCAL_ONLY;

	public boolean isEnabled() {
		return enabled;
	}

	public AdministrationOptions setEnabled(boolean enabled) {
		this.enabled = enabled;
		return this;
	}

	public int getPort() {
		return port;
	}

	public AdministrationOptions setPort(int port) {
		this.port = port;
		return this;
	}

	public String getHost() {
		return host;
	}

	public AdministrationOptions setHost(String host) {
		this.host = host;
		return this;
	}

	public boolean isLocalOnly() {
		return localOnly;
	}

	public void setLocalOnly(boolean localOnly) {
		this.localOnly = localOnly;
	}
}
