package com.gentics.mesh.core.rest.schema;

import com.gentics.mesh.core.rest.schema.impl.SchemaImpl;

public class SchemaUpdateRequest extends SchemaImpl {

	private String uuid;

	public SchemaUpdateRequest() {
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

}
