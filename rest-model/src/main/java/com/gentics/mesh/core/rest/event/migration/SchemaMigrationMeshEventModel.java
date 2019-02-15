package com.gentics.mesh.core.rest.event.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.schema.SchemaReference;
import com.gentics.mesh.core.rest.schema.impl.SchemaReferenceImpl;

public class SchemaMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the source schema version.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference fromVersion;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the target schema version.")
	@JsonDeserialize(as = SchemaReferenceImpl.class)
	private SchemaReference toVersion;

	public SchemaMigrationMeshEventModel() {
	}

	public SchemaReference getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(SchemaReference fromVersion) {
		this.fromVersion = fromVersion;
	}

	public SchemaReference getToVersion() {
		return toVersion;
	}

	public void setToVersion(SchemaReference toVersion) {
		this.toVersion = toVersion;
	}

}
