package com.gentics.mesh.etc.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.etc.config.env.EnvironmentVariable;
import com.gentics.mesh.etc.config.env.Option;

@GenerateDocumentation
public class PublicHttpServerConfig implements Option {

	public static final String MESH_PUBLIC_HTTP_PORT_ENV = "MESH_PUBLIC_HTTP_PORT";
	public static final String MESH_PUBLIC_HTTP_HOST_ENV = "MESH_PUBLIC_HTTP_HOST";

	public static final int DEFAULT_PUBLIC_HTTP_PORT = 8081;

	public static final String DEFAULT_PUBLIC_HTTP_HOST = "127.0.0.1";

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh public HTTP server port. Default is: " + DEFAULT_PUBLIC_HTTP_PORT)
	@EnvironmentVariable(name = MESH_PUBLIC_HTTP_PORT_ENV, description = "Override the configured server http port.")
	private int port = DEFAULT_PUBLIC_HTTP_PORT;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Configure the Gentics Mesh public HTTP server host to bind to. Default is: " + DEFAULT_PUBLIC_HTTP_HOST)
	@EnvironmentVariable(name = MESH_PUBLIC_HTTP_PORT_ENV, description = "Override the configured public http server host which is used to bind to.")
	private String host = DEFAULT_PUBLIC_HTTP_HOST;

	public PublicHttpServerConfig() {
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
