package com.gentics.mesh.core.rest.event.migration;

import com.gentics.mesh.core.rest.schema.SchemaReference;

public class SchemaMigrationMeshEventModel extends AbstractMigrationMeshEventModel {

	private SchemaReference fromVersion;

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
