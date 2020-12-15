package com.gentics.mesh.core.rest.event.migration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.core.rest.schema.impl.MicroschemaReferenceImpl;

/**
 * Event model for microschema migrations.
 */
public class MicroschemaMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the source microschema version.")
	@JsonDeserialize(as = MicroschemaReferenceImpl.class)
	private MicroschemaReference fromVersion;

	@JsonProperty(required = true)
	@JsonPropertyDescription("Reference to the target microschema version.")
	@JsonDeserialize(as = MicroschemaReferenceImpl.class)
	private MicroschemaReference toVersion;

	public MicroschemaMigrationMeshEventModel() {
	}

	public MicroschemaReference getFromVersion() {
		return fromVersion;
	}

	public void setFromVersion(MicroschemaReference fromVersion) {
		this.fromVersion = fromVersion;
	}

	public MicroschemaReference getToVersion() {
		return toVersion;
	}

	public void setToVersion(MicroschemaReference toVersion) {
		this.toVersion = toVersion;
	}
}
