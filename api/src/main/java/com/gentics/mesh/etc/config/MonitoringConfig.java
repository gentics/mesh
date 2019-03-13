package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Configuration for the monitoring features.
 */
@GenerateDocumentation
public class MonitoringConfig implements Option {

	public static final String MESH_MONITORING_HTTP_PORT_ENV = "MESH_MONITORING_HTTP_PORT";
	public static final String MESH_MONITORING_HTTP_HOST_ENV = "MESH_MONITORING_HTTP_HOST";
	public static final String MESH_MONITORING_ENABLED_ENV = "MESH_MONITORING_ENABLED";

	public static final boolean DEFAULT_MONITORING_ENABLED = true;

	public static final int DEFAULT_MONITORING_HTTP_PORT = 8081;

	public static final String DEFAULT_MONITORING_HTTP_HOST = "127.0.0.1";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enable or disable the monitoring system. Default is: " + DEFAULT_MONITORING_ENABLED)
	@EnvironmentVariable(name = MESH_MONITORING_ENABLED_ENV, description = "Override the configured monitoring enabled flag.")
	public boolean enabled = DEFAULT_MONITORING_ENABLED;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh monitoring HTTP server port. Default is: " + DEFAULT_MONITORING_HTTP_PORT)
	@EnvironmentVariable(name = MESH_MONITORING_HTTP_PORT_ENV, description = "Override the configured monitoring server http port.")
	private int port = DEFAULT_MONITORING_HTTP_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh monitoring HTTP server host to bind to. Default is: " + DEFAULT_MONITORING_HTTP_HOST)
	@EnvironmentVariable(name = MESH_MONITORING_HTTP_PORT_ENV, description = "Override the configured monitoring http server host which is used to bind to.")
	private String host = DEFAULT_MONITORING_HTTP_HOST;

	public MonitoringConfig() {
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	@Override
	public void validate(MeshOptions options) {
	}

}
