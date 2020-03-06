package com.gentics.mesh.core.rest.admin.cluster;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

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

	public void setName(String name) {
		this.name = name;
	}

	public void setRole(ServerRole restRole) {
		this.role = restRole;
	}

	public ServerRole getRole() {
		return role;
	}

}
