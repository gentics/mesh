package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.RestModel;

public class NodeChildrenInfo implements RestModel {

	private String schemaUuid;

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
