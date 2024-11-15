package com.gentics.mesh.core.rest.admin.cluster;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST model for cluster configuration update requests.
 */
public class ClusterConfigRequest implements RestModel {

	@JsonProperty(required = false)
	@JsonPropertyDescription("List of server configurations.")
	private List<ClusterServerConfig> servers = new ArrayList<>();

	public ClusterConfigRequest() {
	}

	public List<ClusterServerConfig> getServers() {
		return servers;
	}

	public void setServers(List<ClusterServerConfig> servers) {
		this.servers = servers;
	}
}
