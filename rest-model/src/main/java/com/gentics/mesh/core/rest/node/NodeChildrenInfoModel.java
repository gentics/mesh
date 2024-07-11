package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST model for the node children field.
 */
public class NodeChildrenInfoModel implements RestModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the schema of the node child")
	private String schemaUuid;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Count of children which utilize the schema.")
	private long count;

	public String getSchemaUuid() {
		return schemaUuid;
	}

	@Setter
	public NodeChildrenInfoModel setSchemaUuid(String schemaUuid) {
		this.schemaUuid = schemaUuid;
		return this;
	}

	public long getCount() {
		return count;
	}

	@Setter
	public NodeChildrenInfoModel setCount(long count) {
		this.count = count;
		return this;
	}

}
