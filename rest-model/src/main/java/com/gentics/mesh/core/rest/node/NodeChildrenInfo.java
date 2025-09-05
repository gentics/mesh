package com.gentics.mesh.core.rest.node;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.gentics.mesh.annotation.Setter;
import com.gentics.mesh.core.rest.common.RestModel;

/**
 * REST model for the node children field.
 */
public class NodeChildrenInfo implements RestModel {

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
	public NodeChildrenInfo setSchemaUuid(String schemaUuid) {
		this.schemaUuid = schemaUuid;
		return this;
	}

	public long getCount() {
		return count;
	}

	@Setter
	public NodeChildrenInfo setCount(long count) {
		this.count = count;
		return this;
	}

	@Override
	public String toString() {
		return "uuid: %s, count: %d".formatted(schemaUuid, count);
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof NodeChildrenInfo) {
			NodeChildrenInfo other = NodeChildrenInfo.class.cast(obj);
			return count == other.count && StringUtils.equals(schemaUuid, other.schemaUuid);
		} else {
			return false;
		}
	}
}
