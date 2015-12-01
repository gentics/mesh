package com.gentics.mesh.core.rest.node;

import com.gentics.mesh.core.rest.common.RestModel;

public class NodeChildrenInfo implements RestModel {

	private String schemaUuid;

	private long count;

	public String getSchemaUuid() {
		return schemaUuid;
	}

	public void setSchemaUuid(String schemaUuid) {
		this.schemaUuid = schemaUuid;
	}

	public long getCount() {
		return count;
	}

	public void setCount(long count) {
		this.count = count;
	}

}
