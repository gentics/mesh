package com.gentics.mesh.core.rest.schema.response;

public class SchemaReference {

	private String name;
	private String uuid;

	public SchemaReference() {
	}

	public String getSchemaName() {
		return name;
	}

	public void setSchemaName(String name) {
		this.name = name;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}
}
