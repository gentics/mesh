package com.gentics.cailun.core.rest.schema.response;

public class SchemaReference {

	private String schemaName;
	private String schemaUuid;

	public SchemaReference() {
	}

	public SchemaReference(String schemaName, String schemaUuid) {
		this.schemaName = schemaName;
		this.schemaUuid = schemaUuid;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getSchemaUuid() {
		return schemaUuid;
	}
}
