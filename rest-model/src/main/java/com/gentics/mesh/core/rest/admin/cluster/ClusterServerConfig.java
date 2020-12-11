package com.gentics.mesh.core.rest.admin.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * Configuration for clustering POJO's
 */
public class ClusterServerConfig implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Name of the server.")
	private String name;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Role of the server which can be MASTER or REPLICA.")
	private ServerRole role;

	public ClusterServerConfig() {
	}

	public String getName() {
		return name;
	}

	public ClusterServerConfig setName(String name) {
		this.name = name;
		return this;
	}

	public ClusterServerConfig setRole(ServerRole restRole) {
		this.role = restRole;
		return this;
	}

	public ServerRole getRole() {
		return role;
	}

}
