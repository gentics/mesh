package com.gentics.mesh.core.rest.admin.cluster.coordinator;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Rest model for the coordination master response.
 */
public class CoordinatorMasterResponse implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("Cluster node name of the coordination master.")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("HTTP port of the coordination master Gentics Mesh API.")
	private int port;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Hostname of the coordination master instance.")
	private String host;

	@JsonProperty(required = false)
	@JsonPropertyDescription("UUID of the coordination master instance.")
	private String uuid;

	public CoordinatorMasterResponse() {
	}

	public CoordinatorMasterResponse(String uuid, String name, int port, String host) {
		this.uuid = uuid;
		this.name = name;
		this.port = port;
		this.host = host;
	}

	public String getName() {
		return name;
	}

	@Setter
	public CoordinatorMasterResponse setName(String name) {
		this.name = name;
		return this;
	}

	public int getPort() {
		return port;
	}

	@Setter
	public CoordinatorMasterResponse setPort(int port) {
		this.port = port;
		return this;
	}

	public String getHost() {
		return host;
	}

	@Setter
	public CoordinatorMasterResponse setHost(String host) {
		this.host = host;
		return this;
	}

	public String getUuid() {
		return uuid;
	}

	@Setter
	public CoordinatorMasterResponse setUuid(String uuid) {
		this.uuid = uuid;
		return this;
	}
}
