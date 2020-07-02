package com.gentics.mesh.plugin.common.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

import java.util.List;

/**
 * Response containing a list of UUIDs of created nodes.
 */
public class NodeBulkCreatedResponse implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("The UUIDs of the created nodes")
	List<String> uuids;

	public NodeBulkCreatedResponse(List<String> uuids) {
		this.uuids = uuids;
	}

	public List<String> getUuids() {
		return uuids;
	}

	public NodeBulkCreatedResponse setUuids(List<String> uuids) {
		this.uuids = uuids;

		return this;
	}
}
