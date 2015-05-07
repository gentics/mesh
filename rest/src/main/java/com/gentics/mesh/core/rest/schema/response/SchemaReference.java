package com.gentics.mesh.core.rest.schema.response;


public class SchemaReference {

	private String schemaName;
	private String schemaUuid;

	public SchemaReference() {
	}

	public String getSchemaName() {
		return schemaName;
	}

	public void setSchemaName(String schemaName) {
		this.schemaName = schemaName;
	}

	public String getSchemaUuid() {
		return schemaUuid;
	}

	public void setSchemaUuid(String schemaUuid) {
		this.schemaUuid = schemaUuid;
	}
}
