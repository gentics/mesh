package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

/**
 * Configuration for the embedded metrics server.
 */
@GenerateDocumentation
public class MetricsConfig implements Option {

	public static final String MESH_METRICS_HTTP_PORT_ENV = "MESH_METRICS_HTTP_PORT";
	public static final String MESH_METRICS_HTTP_HOST_ENV = "MESH_METRICS_HTTP_HOST";
	public static final String MESH_METRICS_ENABLED_ENV = "MESH_METRICS_ENABLED";

	public static final boolean DEFAULT_METRICS_ENABLED = true;

	public static final int DEFAULT_METRICS_HTTP_PORT = 8081;

	public static final String DEFAULT_METRICS_HTTP_HOST = "127.0.0.1";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Enable or disable the metrics system. Default is: " + DEFAULT_METRICS_ENABLED)
	@EnvironmentVariable(name = MESH_METRICS_ENABLED_ENV, description = "Override the configured metrics enabled flag.")
	public boolean enabled = DEFAULT_METRICS_ENABLED;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh metrics HTTP server port. Default is: " + DEFAULT_METRICS_HTTP_PORT)
	@EnvironmentVariable(name = MESH_METRICS_HTTP_PORT_ENV, description = "Override the configured metrics server http port.")
	private int port = DEFAULT_METRICS_HTTP_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh metrics HTTP server host to bind to. Default is: " + DEFAULT_METRICS_HTTP_HOST)
	@EnvironmentVariable(name = MESH_METRICS_HTTP_PORT_ENV, description = "Override the configured metrics http server host which is used to bind to.")
	private String host = DEFAULT_METRICS_HTTP_HOST;

	public MetricsConfig() {
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
