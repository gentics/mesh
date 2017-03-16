package com.gentics.mesh.core.rest.node;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.core.rest.common.RestModel;

public class NodeChildrenInfo implements RestModel {

	@JsonPropertyDescription("Reference to the schema of the node child")
	private String schemaUuid;

	@JsonPropertyDescription("Count of children which utilize the schema.")
	private long count;

	public String getSchemaUuid() {
		return schemaUuid;
	}

	public NodeChildrenInfo setSchemaUuid(String schemaUuid) {
		this.schemaUuid = schemaUuid;
		return this;
	}

	public long getCount() {
		return count;
	}

	public NodeChildrenInfo setCount(long count) {
		this.count = count;
		return this;
	}

}
