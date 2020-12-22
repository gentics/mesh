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

	@JsonProperty(required = false)
	@JsonPropertyDescription("Global write quorum setting. Allowed values are numbers and 'all', 'majority'.")
	private String writeQuorum;

	@JsonProperty(required = false)
	@JsonPropertyDescription("Global read quorum setting.")
	private Integer readQuorum;

	public ClusterConfigRequest() {
	}

	public List<ClusterServerConfig> getServers() {
		return servers;
	}

	public void setServers(List<ClusterServerConfig> servers) {
		this.servers = servers;
	}

	public Integer getReadQuorum() {
		return readQuorum;
	}

	public void setReadQuorum(Integer readQuorum) {
		this.readQuorum = readQuorum;
	}

	public String getWriteQuorum() {
		return writeQuorum;
	}

	public void setWriteQuorum(String writeQuorum) {
		this.writeQuorum = writeQuorum;
	}

}
